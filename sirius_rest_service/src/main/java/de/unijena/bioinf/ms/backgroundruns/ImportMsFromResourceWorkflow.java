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
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.middleware.model.compute.AbstractImportSubmission;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class ImportMsFromResourceWorkflow implements Workflow, ProgressSupport {
    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);

    private final AbstractImportSubmission submission;

    private final LongList importedCompounds = new LongArrayList();

    private final boolean saveImportedCompounds;

    public Stream<Instance> getImportedInstancesStr() {
        return importedCompounds.longStream().mapToObj(psm::findInstance).filter(Optional::isPresent).map(Optional::get);
    }

    private final NoSQLProjectSpaceManager psm;

    public ImportMsFromResourceWorkflow(ProjectSpaceManager psm, AbstractImportSubmission submission, boolean saveImportedCompounds) {
        if (!(psm instanceof NoSQLProjectSpaceManager)) {
            throw new IllegalArgumentException("Project space type not supported!");
        }
        this.psm = (NoSQLProjectSpaceManager) psm;
        this.submission = submission;
        this.saveImportedCompounds = saveImportedCompounds;
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
        final List<PathInputResource> inputResources = submission.asPathInputResource();
        if (inputResources != null && !inputResources.isEmpty()) {
            try {
                importedCompounds.clear();
                List<Path> inputFiles = inputResources.stream().map(PathInputResource::getResource).toList();
                LcmsAlignSubToolJobNoSql importerJJob = new LcmsAlignSubToolJobNoSql(
                        inputFiles,
                        () -> psm,
                        submission.isAlignLCMSRuns(),
                        submission.isAllowMs1OnlyData(),
                        submission.getFilter(),
                        submission.getGaussianSigma(),
                        submission.getWaveletScale(),
                        submission.getNoise(),
                        submission.getPersistence(),
                        submission.getMerge(),
                        PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class).getDetectable(),
                        saveImportedCompounds
                );
                importerJJob.addJobProgressListener(progressSupport);
                SiriusJobs.getGlobalJobManager().submitJob(importerJJob).awaitResult();
                importedCompounds.addAll(importerJJob.getImportedCompounds());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
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
                } catch (Exception e) {
                    log.warn("Error when clearing lcms input data.", e);
                }
            }
        }
    }
}
