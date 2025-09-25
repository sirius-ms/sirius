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
import de.unijena.bioinf.ms.frontend.subtools.foldchange.AlignedFeaturesFoldChangeJob;
import de.unijena.bioinf.lcms.align.AlignmentThresholds;
import de.unijena.bioinf.lcms.utils.Tracker;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignSubToolJobNoSql;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.middleware.model.compute.AbstractImportSubmission;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.core.tags.Groups;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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

    @NotNull
    private Map<String, LongSet> importedRunIds = new HashMap<>();

    public @NotNull LongLinkedOpenHashSet getImportedRunIds() {
        return importedRunIds.values().stream().flatMap(LongCollection::stream)
                .collect(Collectors.toCollection(LongLinkedOpenHashSet::new));
    }

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
        importedRunIds = new HashMap<>();
        final List<PathInputResource> inputResources = submission.asPathInputResource();
        final AlignmentThresholds thresholds = new AlignmentThresholds();
        thresholds.setMaximalAllowedMassError(submission.getAlignMaxMassDeviation());
        if (submission.getAlignMaxRetentionTimeDeviation()>=0) thresholds.setMaximalAllowedRetentionTimeError(submission.getAlignMaxRetentionTimeDeviation());
        if (inputResources != null && !inputResources.isEmpty()) {
            try {
                // import
                LcmsAlignSubToolJobNoSql importerJJob = new LcmsAlignSubToolJobNoSql(
                        inputResources.stream().map(PathInputResource::getResource).toList(),
                        submission.getSampleTypes(),
                        project::getProjectSpaceManager,
                        submission.isAlignLCMSRuns(),
                        submission.getFilter(),
                        submission.getGaussianSigma(),
                        submission.getWaveletScale(),
                        submission.getNoiseIntensity(),
                        submission.getMinSNR(),
                        thresholds,
                        submission.getTraceMaxMassDeviation(),
                        saveImportedCompounds,
                        new Tracker.NOOP()
                );
                importerJJob.addJobProgressListener(progressSupport);
                SiriusJobs.getGlobalJobManager().submitJob(importerJJob).awaitResult();

                //add imported Ids
                if (importerJJob.getImportedFeatureIds() != null)
                    importedFeatureIds = importerJJob.getImportedFeatureIds();
                if (importerJJob.getImportedCompoundIds() != null)
                    importedCompoundIds = importerJJob.getImportedCompoundIds();
                if (importerJJob.getImportedRunIds() != null)
                    importedRunIds = importerJJob.getImportedRunIds();


                LongSet sampleRuns = importedRunIds.getOrDefault(TagDefinitions.SAMPLE_TYPE_SAMPLE, LongSets.emptySet());
                LongSet blankRuns = importedRunIds.getOrDefault(TagDefinitions.SAMPLE_TYPE_BLANK, LongSets.emptySet());
                if (!sampleRuns.isEmpty() && !blankRuns.isEmpty()) { // compute fold changes if there are mor
                    SiriusJobs.getGlobalJobManager().submitJob(
                            new AlignedFeaturesFoldChangeJob(project.project(),
                                    Groups.SAMPLE_RUNS.getGroupName(), sampleRuns,
                                    Groups.BLANK_RUNS.getGroupName(), blankRuns,
                                    EnumSet.allOf(QuantMeasure.class), EnumSet.allOf(AggregationType.class),
                                    importedFeatureIds
                            )).awaitResult();
                } else {
                    log.warn("Not all runs have a run type specified. But run types are mandatory for fold change analysis. Skipping simple vs blank fold change analysis!");
                }

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
