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
import de.unijena.bioinf.ms.frontend.subtools.foldchange.ClassyfireFoldChangeJob;
import de.unijena.bioinf.ms.frontend.subtools.foldchange.CompoundsFoldChangeJob;
import de.unijena.bioinf.ms.frontend.subtools.foldchange.NpcFoldChangeJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.QuantRowType;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChangeJobSubmission;
import de.unijena.bioinf.ms.middleware.model.statistics.Statistics;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
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

                    // Update the index. Belt and braces since the fold changes reach the index from the project
                    // anyway: this job locks every instance in the project, so the background run that owns it
                    // re-indexes all of them from the database once this returns, and that read carries the fold
                    // changes. Kept because it is what makes the index right the moment the values exist rather
                    // than at the end of the run, and because nothing here should depend on what a caller does
                    // afterwards - but it is not the reason the blank filter finds anything.
                    if (project.getSearchService() != null) {
                        project.getSearchService().updateDocumentsFields(project.getProjectId(), foldChanges.keySet(), alf -> {
                            List<FoldChange> nuFC = foldChanges.get(alf.getAlignedFeatureId());
                            if (nuFC != null && !nuFC.isEmpty())
                                alf.setStats(mergeStats(alf.getStats(), nuFC));
                        }, AlignedFeature.class);
                    }
                });

                /**
                 * no batches for compound classes, as we would duplicate classes over multiple batches...
                 * TODO parallelize loops inside compute methods?
                 */
                case NPC_CLASSES -> {
                    List<de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures> allFeatures =
                            project.project().getAllAlignedFeatures().toList();
                    NpcFoldChangeJob job = new NpcFoldChangeJob(
                            project.project(),
                            jobSubmission.getLeftRunGroup(), leftRuns,
                            jobSubmission.getRightRunGroup(), rightRuns,
                            jobSubmission.getQuantificationMeasures(),
                            jobSubmission.getAggregationTypes(),
                            allFeatures, 0.5);
                    job.addJobProgressListener(progressSupport);
                    SiriusJobs.getGlobalJobManager().submitJob(job).takeResult();
                }

                case CLASSYFIRE_CLASSES -> {
                    List<de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures> allFeatures =
                            project.project().getAllAlignedFeatures().toList();
                    ClassyfireFoldChangeJob job = new ClassyfireFoldChangeJob(
                            project.project(),
                            jobSubmission.getLeftRunGroup(), leftRuns,
                            jobSubmission.getRightRunGroup(), rightRuns,
                            jobSubmission.getQuantificationMeasures(),
                            jobSubmission.getAggregationTypes(),
                            allFeatures, 0.5);
                    job.addJobProgressListener(progressSupport);
                    SiriusJobs.getGlobalJobManager().submitJob(job).takeResult();
                }

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
//                    if (project.getSearchService() != null) {
//                        project.getSearchService().updateDocumentsFields(project.getProjectId(), foldChanges.keySet(), c -> {
//                            List<FoldChange> nuFC = foldChanges.get(c.getCompoundId());
//                            if (nuFC != null && !nuFC.isEmpty()) {
//                                Set<Statistics> updatedStats = new HashSet<>();
//                                if (c.getStats() != null)
//                                    updatedStats.addAll(c.getStats());
//                                updatedStats.addAll(nuFC);
//                                c.setStats(new ArrayList<>(updatedStats));
//                            }
//                        }, Compound.class);
//                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Merges freshly computed fold changes into the ones an indexed feature already carries.
     * <p>
     * A fold change is equal to another when it occupies the same slot - group pair, quantification,
     * aggregation - because its abundances are excluded from equality. A recomputed entry is therefore equal
     * to the one it supersedes, and collecting both into a set would keep whichever arrived first, which is
     * the old one. The recomputed values have to win.
     */
    static List<Statistics> mergeStats(@Nullable Collection<Statistics> existing,
                                       @NotNull Collection<? extends Statistics> recomputed) {
        Map<Statistics, Statistics> bySlot = new LinkedHashMap<>();
        if (existing != null)
            existing.forEach(stat -> bySlot.put(stat, stat));
        // replaces the value of an equal slot, so a recomputed fold change supersedes the one it repeats
        recomputed.forEach(stat -> bySlot.put(stat, stat));
        return new ArrayList<>(bySlot.values());
    }

    /**
     * Drops the fold changes this job is about to recompute, so a rerun replaces them instead of adding a
     * second set next to them.
     * <p>
     * The record names its groups {@code leftGroup} and {@code rightGroup}, and stores its enums as their
     * names - so the fields have to be addressed by those names, and compared against strings. Both details
     * are easy to get wrong in a way nothing reports: a filter over a field the record does not have, or over
     * an enum it stores as text, simply matches no document and deletes nothing.
     * <p>
     * {@code in()} over the enum names rather than one {@code removeAll} per combination: none of these four
     * fields is indexed - a fold change repository indexes only its foreign key - so every variant of this is a
     * full scan that deserialises the collection, and the only question is how many times. The standing
     * objection to {@code in()} is that it scans an index instead of doing point lookups, which is not a
     * distinction here.
     */
    protected void cleanupFoldChanges() throws IOException {
        project.storage().removeAll(
                Filter.and(
                        Filter.where("leftGroup").eq(jobSubmission.getLeftRunGroup()),
                        Filter.where("rightGroup").eq(jobSubmission.getRightRunGroup()),
                        Filter.where("aggregation").in(names(jobSubmission.getAggregationTypes())),
                        Filter.where("quantification").in(names(jobSubmission.getQuantificationMeasures()))
                ),
                statsTarget.getProjectFoldChangeClass()
        );
    }

    /** The names an enum collection is stored under, which is what a filter over it has to compare against. */
    private static String[] names(@NotNull Collection<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).toArray(String[]::new);
    }
}
