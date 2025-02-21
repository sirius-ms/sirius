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
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignSubToolJobNoSql;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.middleware.model.compute.AbstractImportSubmission;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ImportMsFromResourceWorkflow implements Workflow, ProgressSupport {
    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);

    private final AbstractImportSubmission submission;


    @Getter
    @NotNull
    private LongLinkedOpenHashSet importedFeatureIds = new LongLinkedOpenHashSet();

    @Getter
    @NotNull
    private LongLinkedOpenHashSet importedCompoundIds = new LongLinkedOpenHashSet();

    private final boolean saveImportedCompounds;

    private final NoSQLProjectImpl project;

    public ImportMsFromResourceWorkflow(NoSQLProjectImpl project, AbstractImportSubmission<?> submission, boolean saveImportedCompounds) {
        this.project = project;
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
        importedFeatureIds = new LongLinkedOpenHashSet();
        importedCompoundIds = new LongLinkedOpenHashSet();
        final List<PathInputResource> inputResources = submission.asPathInputResource();
        if (inputResources != null && !inputResources.isEmpty()) {
            try {
                LcmsAlignSubToolJobNoSql importerJJob = new LcmsAlignSubToolJobNoSql(
                        inputResources.stream().map(PathInputResource::getResource).toList(),
                        () -> project.getProjectSpaceManager(),
                        submission.isAlignLCMSRuns(),
                        submission.getFilter(),
                        submission.getGaussianSigma(),
                        submission.getWaveletScale(),
                        submission.getNoise(),
                        submission.getPersistence(),
                        submission.getMerge(),
                        saveImportedCompounds
                );
                importerJJob.addJobProgressListener(progressSupport);
                SiriusJobs.getGlobalJobManager().submitJob(importerJJob).awaitResult();
                //add imported Ids
                if (importerJJob.getImportedFeatureIds() != null)
                    importedFeatureIds = importerJJob.getImportedFeatureIds();
                if (importerJJob.getImportedCompoundIds() != null)
                    importedCompoundIds = importerJJob.getImportedCompoundIds();


                //Update search index.
                StopWatch stopWatch = null;

                if (project.getSearchService() != null) {
                    stopWatch = StopWatch.createStarted();
                    System.out.println();
                    System.out.println("Indexing newly imported Data...");

                    SearchService searchService = project.getSearchService();
                    //Handle FEATURES
                    if (!importedFeatureIds.isEmpty()) {
                        Partition<Long> partition = Partition.ofSize(importedFeatureIds.stream().sorted().toList(), 100_000);
                        for (List<Long> ids : partition) {
                            searchService.addDocuments(project.getProjectId(),
                                    project.storage().findStr(Filter.where("alignedFeatureId").in(ids.toArray(Long[]::new)), AlignedFeatures.class)
                                            .parallel()
                                            .map(f -> project.convertToApiFeature(f, EnumSet.of(AlignedFeature.OptField.qualities)))
                                            .toList());
                        }
                    }

                    //Handle COMPOUNDS
                    if (!importedCompoundIds.isEmpty()) {
                        //todo IMPLEMENT!
                    }

                    //Handle Runs
                    //todo we should maybe also track runIds because we wanted to add a add run to alignment features.
                    searchService.addDocuments(project.getProjectId(),
                            project.storage().findAllStr(LCMSRun.class)
                                    .parallel()
                                    .map(run -> project.convertToApiRun(run, EnumSet.of(Run.OptField.tags))) //tag might have been added during preprocessing.
                                    .toList());
                }

                System.out.println("Indexing imported Data took: " + stopWatch);
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
