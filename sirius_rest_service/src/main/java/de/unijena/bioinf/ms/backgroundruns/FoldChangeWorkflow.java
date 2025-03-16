/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.backgroundruns;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.ms.frontend.subtools.foldchange.AlignedFeaturesFoldChangeJob;
import de.unijena.bioinf.ms.frontend.subtools.foldchange.CompoundsFoldChangeJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.QuantRowType;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChangeJobSubmission;
import de.unijena.bioinf.ms.middleware.model.statistics.Statistics;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.longs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.forEachBatch;

public class FoldChangeWorkflow implements Workflow, ProgressSupport {

    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);

    private final NoSQLProjectImpl project;

    private final FoldChangeJobSubmission jobSubmission;

    private final QuantRowType statsTarget;

    public FoldChangeWorkflow(NoSQLProjectImpl project, FoldChangeJobSubmission jobSubmission, QuantRowType statsTarget) {
        this.statsTarget = statsTarget;
        this.project = project;
        this.jobSubmission = jobSubmission;
    }

    @Override
    public void updateProgress(long min, long max, long progress, @Nullable String shortInfo) {
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
        try {
            LongSet leftRuns = project.findRunsByGroup(jobSubmission.getLeftRunGroup(), Pageable.unpaged(), EnumSet.noneOf(Run.OptField.class))
                    .stream().map(Run::getRunId).mapToLong(Long::parseLong)
                    .collect(LongOpenHashSet::new, LongSet::add, LongSet::addAll);

            LongSet rightRuns = project.findRunsByGroup(jobSubmission.getRightRunGroup(), Pageable.unpaged(), EnumSet.noneOf(Run.OptField.class))
                    .stream().map(Run::getRunId).mapToLong(Long::parseLong)
                    .collect(LongOpenHashSet::new, LongSet::add, LongSet::addAll);


            cleanupFoldChanges();

            if (leftRuns.isEmpty() || rightRuns.isEmpty())
                return;

            switch (statsTarget) {
                case FEATURES -> forEachBatch(project.project().getAllAlignedFeatures(), af -> {
                    List<AlignedFeaturesFoldChangeJob> jobs =
                            Partition.ofNumber(af, SiriusJobs.getGlobalJobManager().getCPUThreads() * 2)
                                    .stream()
                                    .map(afs -> new AlignedFeaturesFoldChangeJob(
                                            project.project(),
                                            jobSubmission.getLeftRunGroup(), leftRuns,
                                            jobSubmission.getRightRunGroup(), rightRuns,
                                            jobSubmission.getQuantificationMeasures(),
                                            jobSubmission.getAggregationTypes(), afs))
                                    .peek(jj -> jj.addJobProgressListener(progressSupport))
                                    .peek(SiriusJobs.getGlobalJobManager()::submitJob)
                                    .toList();

                    @NotNull final Map<String, List<de.unijena.bioinf.ms.middleware.model.statistics.FoldChange>> foldChanges =
                            jobs.stream()
                                    .map(JJob::takeResult)
                                    .flatMap(List::stream)
                                    .map(NoSQLProjectImpl::convertToApiFoldChange)
                                    .collect(Collectors.groupingBy(de.unijena.bioinf.ms.middleware.model.statistics.FoldChange::getObjectId));

                    //update index
                    project.getSearchService().updateDocumentsFields(project.getProjectId(), foldChanges.keySet(), alf -> {
                        List<FoldChange> nuFC = foldChanges.get(alf.getAlignedFeatureId());
                        if (nuFC != null && !nuFC.isEmpty()) {
                            Set<Statistics> updatedStats = new HashSet<>(alf.getStats());
                            updatedStats.addAll(nuFC);
                            alf.setStats(new ArrayList<>(updatedStats));
                        }
                    }, AlignedFeature.class);
                });

                case COMPOUNDS -> forEachBatch(project.project().getAllCompounds(), af -> {
                    List<CompoundsFoldChangeJob> jobs =
                            Partition.ofNumber(af, SiriusJobs.getGlobalJobManager().getCPUThreads() * 2)
                                    .stream()
                                    .map(c -> new CompoundsFoldChangeJob(
                                            project.project(),
                                            jobSubmission.getLeftRunGroup(), leftRuns,
                                            jobSubmission.getRightRunGroup(), rightRuns,
                                            jobSubmission.getQuantificationMeasures(),
                                            jobSubmission.getAggregationTypes(), c))
                                    .peek(jj -> jj.addJobProgressListener(progressSupport))
                                    .peek(SiriusJobs.getGlobalJobManager()::submitJob)
                                    .toList();

                    @NotNull Map<String, List<de.unijena.bioinf.ms.middleware.model.statistics.FoldChange>> foldChanges =
                            jobs.stream()
                                    .map(JJob::takeResult)
                                    .flatMap(List::stream)
                                    .map(NoSQLProjectImpl::convertToApiFoldChange)
                                    .collect(Collectors.groupingBy(de.unijena.bioinf.ms.middleware.model.statistics.FoldChange::getObjectId));

                    //update index
                    //todo add if compound index is implemented
//                    project.getSearchService().updateDocumentsFields(project.getProjectId(), foldChanges.keySet(), c -> {
//                        List<FoldChange> nuFC = foldChanges.get(c.getCompoundId());
//                        if (nuFC != null && !nuFC.isEmpty()) {
//                            Set<Statistics> updatedStats = new HashSet<>(c.getStats());
//                            updatedStats.addAll(nuFC);
//                            c.setStats(new ArrayList<>(updatedStats));
//                        }
//                    }, Compound.class);
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void cleanupFoldChanges() throws IOException {
        project.storage().removeAll(
                Filter.and(
                        Filter.where("left").eq(jobSubmission.getLeftRunGroup()),
                        Filter.where("right").eq(jobSubmission.getRightRunGroup()),
                        Filter.where("aggregation").in(jobSubmission.getAggregationTypes().toArray(AggregationType[]::new)),
                        Filter.where("quantification").in(jobSubmission.getQuantificationMeasures().toArray(QuantMeasure[]::new))
                ),
                statsTarget.getProjectFoldChangeClass()
        );
    }
}
