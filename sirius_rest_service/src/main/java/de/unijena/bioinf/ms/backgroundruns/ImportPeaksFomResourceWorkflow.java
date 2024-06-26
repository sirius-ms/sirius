/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.backgroundruns;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.jjobs.ProgressSupport;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.InstanceImporter;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class ImportPeaksFomResourceWorkflow implements Workflow, ProgressSupport {
    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);
    private final boolean ignoreFormulas;
    private final boolean allowMs1OnlyData;
    private Iterable<Instance> importedCompounds = null;

    public Iterable<Instance> getImportedInstances() {
        return importedCompounds;
    }

    public Stream<Instance> getImportedInstancesStr() {
        return StreamSupport.stream(importedCompounds.spliterator(), false);
    }

    private final ProjectSpaceManager psm;

    private final Collection<InputResource<?>> inputResources;

    public ImportPeaksFomResourceWorkflow(ProjectSpaceManager psm, Collection<InputResource<?>> inputResources, boolean ignoreFormulas, boolean allowMs1OnlyData) {
        this.psm = psm;
        this.inputResources = inputResources;
        this.ignoreFormulas = ignoreFormulas;
        this.allowMs1OnlyData = allowMs1OnlyData;
    }

    @Override
    public void updateProgress(long min, long max, long progress, String shortInfo) {
        progressSupport.updateConnectedProgress(min, max, progress, shortInfo);
    }

    @Override
    public void addJobProgressListener(JobProgressEventListener listener) {
        progressSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removeJobProgressListener(JobProgressEventListener listener) {
        progressSupport.removeProgress(listener);
    }

    @Override
    public JobProgressEvent currentProgress() {
        return progressSupport.currentConnectedProgress();
    }

    @Override
    public JobProgressEvent currentCombinedProgress() {
        return progressSupport.currentCombinedProgress();
    }

    @Override
    public void run() {
        if (inputResources != null && !inputResources.isEmpty()) {
            InstanceImporter.ImportInstancesJJob importerJJob = new InstanceImporter(psm, x -> true)
                    .makeImportJJob(inputResources, ignoreFormulas, allowMs1OnlyData);
            importerJJob.addJobProgressListener(progressSupport);

            try {
                importedCompounds = SiriusJobs.getGlobalJobManager().submitJob(importerJJob).awaitResult();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } finally {
                inputResources.stream().filter(InputResource::isDeleteAfterImport).forEach(r -> {
                    try {
                        if (r instanceof PathInputResource pr)
                            FileUtils.deleteRecursively(pr.getResource());
                    } catch (IOException e) {
                        log.warn("Error when deleting lcms input data.", e);
                    }
                });

                inputResources.stream().filter(r -> r instanceof PathInputResource)
                        .map(r -> (PathInputResource) r)
                        .map(PathInputResource::getResource).map(Path::getFileSystem).distinct()
                        .filter(it -> !Objects.equals(it, FileSystems.getDefault()))
                        .forEach(fs -> {
                            try {
                                fs.close();
                            } catch (IOException e) {
                                log.warn("Error when closing non default file system of lcms input data.", e);
                            }
                        });
            }
        }
    }
}
