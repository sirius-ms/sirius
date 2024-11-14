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
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantificationType;
import de.unijena.bioinf.ms.persistence.model.core.tags.Tag;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagCategoryGroup;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;

public class FoldChangeWorkflow implements Workflow, ProgressSupport {

    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);

    private final NoSQLProjectSpaceManager psm;

    private final String left;

    private final String right;

    private final AggregationType aggregation;

    private final QuantificationType quantification;

    private final Class<?> target;

    public FoldChangeWorkflow(ProjectSpaceManager psm, String left, String right, AggregationType aggregation, QuantificationType quantification, Class<?> target) {
        this.target = target;
        if (!(psm instanceof NoSQLProjectSpaceManager)) {
            throw new IllegalArgumentException("Project space type not supported!");
        }
        this.psm = (NoSQLProjectSpaceManager) psm;
        this.left = left;
        this.right = right;
        this.aggregation = aggregation;
        this.quantification = quantification;
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

    // TODO maybe return list of affected instances (see BackgroundRuns.cleanup())

    @Override
    public void run() {
        try {
            BasicMasterJJob<Boolean> scheduler = new BasicMasterJJob<>(JJob.JobType.SCHEDULER) {
                private final AtomicLong total = new AtomicLong(0);
                private final AtomicLong progress = new AtomicLong(0);

                @Override
                protected Boolean compute() throws Exception {
                    TagCategoryGroup leftGroup = psm.getProject().getStorage()
                            .findStr(Filter.where("name").eq(left), TagCategoryGroup.class)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("No such tag category group: " + left));
                    TagCategoryGroup rightGroup = psm.getProject().getStorage()
                            .findStr(Filter.where("name").eq(right), TagCategoryGroup.class)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("No such tag category group: " + right));

                    LongSet leftRuns = new LongRBTreeSet();
                    LongSet rightRuns = new LongRBTreeSet();

                    Iterable<LCMSRun> runs = psm.getProject().getStorage().findAll(LCMSRun.class);
                    runLoop:
                    for (LCMSRun run : runs) {
                        for (String gname : leftGroup.getCategories()) {
                            Optional<Tag> tag = psm.getProject().getStorage().findStr(
                                    Filter.and(
                                            Filter.where("taggedObjectId").eq(run.getRunId()),
                                            Filter.where("category").eq(gname)
                                    ),
                                    Tag.class
                            ).findFirst();
                            if (tag.isPresent()) {
                                leftRuns.add(run.getRunId());
                                continue runLoop;
                            }
                        }
                        for (String gname : rightGroup.getCategories()) {
                            Optional<Tag> tag = psm.getProject().getStorage().findStr(
                                    Filter.and(
                                            Filter.where("taggedObjectId").eq(run.getRunId()),
                                            Filter.where("category").eq(gname)
                                    ),
                                    Tag.class
                            ).findFirst();
                            if (tag.isPresent()) {
                                rightRuns.add(run.getRunId());
                            }
                        }
                    }

                    AtomicReference<List<BasicJJob<?>>> jobs = new AtomicReference<>(new ArrayList<>());

                    if (AlignedFeature.class.equals(target)) {
                        AtomicReference<List<AlignedFeatures>> features = new AtomicReference<>(new ArrayList<>());
                        psm.getProject().getAllAlignedFeatures().forEach(af -> {
                            if (features.get().size() == 100) {
                                jobs.get().add(submitAlignedFeaturesComputation(features.get(), leftRuns, rightRuns));
                                features.get().clear();
                            }
                        });
                        jobs.get().add(submitAlignedFeaturesComputation(features.get(), leftRuns, rightRuns));
                    } else if (Compound.class.equals(target)) {
                        AtomicReference<List<Compound>> compounds = new AtomicReference<>(new ArrayList<>());
                        psm.getProject().getAllCompounds().forEach(c -> {
                            jobs.get().add(submitCompoundComputation(compounds.get(), leftRuns, rightRuns));
                            compounds.get().clear();
                        });
                        jobs.get().add(submitCompoundComputation(compounds.get(), leftRuns, rightRuns));
                    } else {
                        throw new IllegalArgumentException("Invalid target: " + target);
                    }

                    for (BasicJJob<?> job : jobs.get()) {
                        job.awaitResult();
                    }
                    return true;
                }

                private BasicJJob<Boolean> submitCompoundComputation(List<Compound> compounds, LongSet leftRuns, LongSet rightRuns) {
                    updateProgress(total.addAndGet(compounds.size() + 1), progress.get());
                    return new BasicJJob<>() {
                        @Override
                        protected Boolean compute() throws Exception {
                            List<FoldChange.CompoundFoldChange> foldChanges = new ArrayList<>();
                            for (Compound c : compounds) {
                                List<Feature> leftFeatures = new ArrayList<>();
                                List<Feature> rightFeatures = new ArrayList<>();
                                psm.getProject().fetchAdductFeatures(c);
                                if (c.getAdductFeatures().isPresent()) {
                                    for (AlignedFeatures af : c.getAdductFeatures().get()) {
                                        psm.getProject().fetchFeatures(af);
                                        if (af.getFeatures().isPresent()) {
                                            for (Feature f : af.getFeatures().get()) {
                                                if (leftRuns.contains(f.getRunId())) {
                                                    leftFeatures.add(f);
                                                } else if (rightRuns.contains(f.getRunId())) {
                                                    rightFeatures.add(f);
                                                }
                                            }
                                        }
                                    }
                                }
                                updateProgress(total.get(), progress.addAndGet(1));
                                if (leftFeatures.isEmpty() || rightFeatures.isEmpty()) {
                                    continue;
                                }

                                double leftval = aggregate(quantify(leftFeatures));
                                double rightval = aggregate(quantify(rightFeatures));
                                double foldChange = rightval > 0 ? leftval / rightval : 0.0;

                                foldChanges.add(FoldChange.CompoundFoldChange
                                        .builder()
                                        .foreignId(c.getCompoundId())
                                        .foldChange(foldChange)
                                        .leftGroup(left)
                                        .rightGroup(right)
                                        .aggregation(aggregation)
                                        .quantification(quantification)
                                        .build()
                                );
                            }
                            upsertFoldChanges(foldChanges, FoldChange.CompoundFoldChange.class);
                            updateProgress(total.get(), progress.addAndGet(1));
                            return null;
                        }
                    };
                }

                private BasicJJob<Boolean> submitAlignedFeaturesComputation(List<AlignedFeatures> alignedFeatures, LongSet leftRuns, LongSet rightRuns) {
                    updateProgress(total.addAndGet(alignedFeatures.size() + 1), progress.get());
                    return new BasicJJob<>() {
                        @Override
                        protected Boolean compute() throws Exception {
                            List<FoldChange.AlignedFeaturesFoldChange> foldChanges = new ArrayList<>();
                            for (AlignedFeatures af : alignedFeatures) {
                                List<Feature> leftFeatures = new ArrayList<>();
                                List<Feature> rightFeatures = new ArrayList<>();
                                psm.getProject().fetchFeatures(af);
                                if (af.getFeatures().isPresent()) {
                                    for (Feature f : af.getFeatures().get()) {
                                        if (leftRuns.contains(f.getRunId())) {
                                            leftFeatures.add(f);
                                        } else if (rightRuns.contains(f.getRunId())) {
                                            rightFeatures.add(f);
                                        }
                                    }
                                }
                                updateProgress(total.get(), progress.addAndGet(1));
                                if (leftFeatures.isEmpty() || rightFeatures.isEmpty()) {
                                    continue;
                                }

                                double leftval = aggregate(quantify(leftFeatures));
                                double rightval = aggregate(quantify(rightFeatures));
                                double foldChange = rightval > 0 ? leftval / rightval : 0.0;

                                foldChanges.add(FoldChange.AlignedFeaturesFoldChange
                                        .builder()
                                        .foreignId(af.getAlignedFeatureId())
                                        .foldChange(foldChange)
                                        .leftGroup(left)
                                        .rightGroup(right)
                                        .aggregation(aggregation)
                                        .quantification(quantification)
                                        .build()
                                );
                            }
                            upsertFoldChanges(foldChanges, FoldChange.AlignedFeaturesFoldChange.class);
                            updateProgress(total.get(), progress.addAndGet(1));
                            return null;
                        }
                    };
                }

                private DoubleStream quantify(List<Feature> features) {
                    return features.stream().mapToDouble(feature -> switch (quantification) {
                        case APEX_INTENSITY -> feature.getApexIntensity();
                        case AREA_UNDER_CURVE -> feature.getAreaUnderCurve();
                        case APEX_MASS -> feature.getApexMass();
                        case AVERAGE_MASS -> feature.getAverageMass();
                        case APEX_RT -> feature.getRetentionTime().getMiddleTime();
                        case FULL_WIDTH_HALF_MAX -> feature.getFwhm();
                    });
                }

                private double aggregate(DoubleStream values) {
                    return switch (aggregation) {
                        case AVG -> values.average().orElse(0.0);
                        case MIN -> values.min().orElse(0.0);
                        case MAX -> values.max().orElse(0.0);
                        case MEDIAN -> new Median().evaluate(values.toArray());
                    };
                }

                private <F extends FoldChange> void upsertFoldChanges(List<F> foldChanges, Class<F> clazz) {
                    // TODO how to efficiently check if the fc already exists? -> maybe I should check before running the computation?
                    // TODO maybe also overwrite option?
                }

            };
            scheduler.addJobProgressListener(progressSupport);

            SiriusJobs.getGlobalJobManager().submitJob(scheduler).awaitResult();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
