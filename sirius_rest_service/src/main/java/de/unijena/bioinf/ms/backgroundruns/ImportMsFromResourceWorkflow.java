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
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.jjobs.ProgressSupport;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignSubToolJobNoSql;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignSubToolJobSiriusPs;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class ImportMsFromResourceWorkflow implements Workflow, ProgressSupport {
    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);
    private final boolean alignRuns;
    private final boolean allowMs1OnlyData;
    //    private final boolean clearInput;
    private Iterable<Instance> importedCompounds = null;

    public Iterable<Instance> getImportedInstances() {
        return importedCompounds;
    }

    public Stream<Instance> getImportedInstancesStr() {
        return StreamSupport.stream(importedCompounds.spliterator(), false);
    }

    private final ProjectSpaceManager psm;

    private final Collection<PathInputResource> inputResources;

    public ImportMsFromResourceWorkflow(ProjectSpaceManager psm, Collection<PathInputResource> inputResources, boolean allowMs1OnlyData, boolean alignRuns) {
        this.psm = psm;
        this.inputResources = inputResources;
        this.alignRuns = alignRuns;
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
        Path workingDir = null;
        try {
            if (!alignRuns) {
                InstanceImporter.ImportInstancesJJob importerJJob = new InstanceImporter(psm, x -> true)
                        .makeImportJJob(Collections.unmodifiableCollection(inputResources), true, allowMs1OnlyData);
                importerJJob.addJobProgressListener(progressSupport);
                importedCompounds = SiriusJobs.getGlobalJobManager().submitJob(importerJJob).awaitResult();
            } else {
                //create working dir in same fs as input data. allows e.g. for in-memory fs for working dir.
                if (psm instanceof SiriusProjectSpaceManager spsm) {
                    workingDir = FileUtils.newTempFile("lcms-align-working-dir_", "", inputResources.iterator().next().getResource().getFileSystem());
                    LcmsAlignSubToolJobSiriusPs importerJJob = new LcmsAlignSubToolJobSiriusPs(
                            workingDir,
                            inputResources.stream().map(PathInputResource::getResource).collect(Collectors.toList()),
                            () -> spsm, null, null);
                    SiriusJobs.getGlobalJobManager().submitJob(importerJJob).awaitResult();
                    importerJJob.addJobProgressListener(progressSupport);
                    importedCompounds = new ArrayList<>(importerJJob.getImportedCompounds());
                } else if (psm instanceof NoSQLProjectSpaceManager spsm) {
                    LcmsAlignSubToolJobNoSql importerJJob = new LcmsAlignSubToolJobNoSql(
                            inputResources.stream().map(PathInputResource::getResource).collect(Collectors.toList()),
                            () -> spsm, PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class).getDetectable());
                    SiriusJobs.getGlobalJobManager().submitJob(importerJJob).awaitResult();
                    importerJJob.addJobProgressListener(progressSupport);
                    importedCompounds = new ArrayList<>(importerJJob.getImportedCompounds());
                } else {
                    throw new IllegalArgumentException("Unknown project space implementation. Cannot import!");
                }
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (workingDir != null)
                    FileUtils.deleteRecursively(workingDir);
            } catch (IOException e) {
                log.warn("Error when deleting lcms align working dir.", e);
            }

            inputResources.stream().filter(InputResource::isDeleteAfterImport).forEach(r -> {
                try {
                    FileUtils.deleteRecursively(r.getResource());
                } catch (IOException e) {
                    log.warn("Error when deleting lcms input data.", e);
                }
            });
            //close non-local fs
            inputResources.stream().map(PathInputResource::getResource).map(Path::getFileSystem).distinct()
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
