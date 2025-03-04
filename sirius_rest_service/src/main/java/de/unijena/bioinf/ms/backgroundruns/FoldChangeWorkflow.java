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
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.longs.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;

// todo This is a API/GUI only implementation. We need to change out architecture to brig this to the CLI.
public class FoldChangeWorkflow implements Workflow, ProgressSupport {

    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);

    private final NoSQLProjectImpl project;

    private final String left;

    private final String right;

    private final AggregationType aggregation;

    private final QuantMeasure quantification;

    private final Class<?> target;

    public FoldChangeWorkflow(NoSQLProjectImpl project, String left, String right, AggregationType aggregation, QuantMeasure quantification, Class<?> target) {
        this.target = target;
        this.project = project;
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

    @Override
    public void run() {
        try {
            BasicMasterJJob<Boolean> scheduler = new BasicMasterJJob<>(JJob.JobType.SCHEDULER) {
                private final AtomicLong total = new AtomicLong(0);
                private final AtomicLong progress = new AtomicLong(0);

                @Override
                protected Boolean compute() throws Exception {
                    LongSet leftRuns = project.findRunsByGroup(left, Pageable.unpaged(), EnumSet.noneOf(Run.OptField.class))
                            .stream().map(Run::getRunId).mapToLong(Long::parseLong)
                            .collect(LongOpenHashSet::new, LongSet::add, LongSet::addAll);

                    LongSet rightRuns = project.findRunsByGroup(right, Pageable.unpaged(), EnumSet.noneOf(Run.OptField.class))
                            .stream().map(Run::getRunId).mapToLong(Long::parseLong)
                            .collect(LongOpenHashSet::new, LongSet::add, LongSet::addAll);

                    if (leftRuns.isEmpty()|| rightRuns.isEmpty()) {
                        if (AlignedFeature.class.equals(target)) {
                            cleanupFoldChanges(FoldChange.AlignedFeaturesFoldChange.class);
                        } else if (Compound.class.equals(target)){
                            cleanupFoldChanges(FoldChange.CompoundFoldChange.class);
                        }

                        return true;
                    }


                    AtomicReference<List<BasicJJob<?>>> jobs = new AtomicReference<>(new ArrayList<>());

                    if (AlignedFeature.class.equals(target)) {
                        cleanupFoldChanges(FoldChange.AlignedFeaturesFoldChange.class);

                        AtomicReference<LongSet> features = new AtomicReference<>(new LongArraySet());
                        project.project().getAllAlignedFeatures().forEach(af -> {
                            if (features.get().size() == 100) {
                                jobs.get().add(submitAlignedFeaturesComputation(new LongArraySet(features.get()), leftRuns, rightRuns));
                                features.get().clear();
                            }
                            features.getAndUpdate(aflist -> {
                                aflist.add(af.getAlignedFeatureId());
                                return aflist;
                            });
                        });
                        jobs.get().add(submitAlignedFeaturesComputation(new LongArraySet(features.get()), leftRuns, rightRuns));
                    } else if (Compound.class.equals(target)) {
                        cleanupFoldChanges(FoldChange.CompoundFoldChange.class);

                        AtomicReference<List<Compound>> compounds = new AtomicReference<>(new ArrayList<>());
                        project.project().getAllCompounds().forEach(c -> {
                            if (compounds.get().size() == 100) {
                                jobs.get().add(submitCompoundComputation(new ArrayList<>(compounds.get()), leftRuns, rightRuns));
                                compounds.get().clear();
                            }
                            compounds.getAndUpdate(clist -> {
                                clist.add(c);
                                return clist;
                            });
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
                    BasicJJob<Boolean> job = new BasicJJob<>() {
                        @Override
                        protected Boolean compute() throws Exception {
                            List<FoldChange.CompoundFoldChange> foldChanges = new ArrayList<>();
                            for (Compound c : compounds) {
                                Long2ObjectMap<List<Feature>> leftFeatures = new Long2ObjectOpenHashMap<>(leftRuns.size());
                                Long2ObjectMap<List<Feature>> rightFeatures = new Long2ObjectOpenHashMap<>(rightRuns.size());
                                project.project().fetchAdductFeatures(c);
                                if (c.getAdductFeatures().isPresent()) {
                                    for (AlignedFeatures af : c.getAdductFeatures().get()) {
                                        project.project().fetchFeatures(af);
                                        if (af.getFeatures().isPresent()) {
                                            for (Feature f : af.getFeatures().get()) {
                                                if (leftRuns.contains((long) f.getRunId())) {
                                                    leftFeatures.computeIfAbsent(f.getRunId(), k -> new ArrayList<>()).add(f);
                                                } else if (rightRuns.contains((long) f.getRunId())) {
                                                    rightFeatures.computeIfAbsent(f.getRunId(), k -> new ArrayList<>()).add(f);
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
                                        .compoundId(c.getCompoundId())
                                        .foldChange(foldChange)
                                        .leftGroup(left)
                                        .rightGroup(right)
                                        .aggregation(aggregation)
                                        .quantification(quantification)
                                        .build()
                                );
                            }
                            project.storage().insertAll(foldChanges);
                            updateProgress(total.get(), progress.addAndGet(1));
                            return null;
                        }
                    };
                    SiriusJobs.getGlobalJobManager().submitJob(job);
                    return job;
                }

                private BasicJJob<Boolean> submitAlignedFeaturesComputation(LongSet alignedFeatures, LongSet leftRuns, LongSet rightRuns) {
                    BasicJJob<Boolean> job = new BasicJJob<>() {
                        @Override
                        protected Boolean compute() throws Exception {
                            List<FoldChange.AlignedFeaturesFoldChange> foldChanges = new ArrayList<>();
                            for (long af : alignedFeatures) {
                                Long2ObjectMap<List<Feature>> leftFeatures = new Long2ObjectOpenHashMap<>(leftRuns.size());
                                Long2ObjectMap<List<Feature>> rightFeatures = new Long2ObjectOpenHashMap<>(rightRuns.size());

                                project.storage().findStr(Filter.where("alignedFeatureId").eq(af), Feature.class).forEach(f -> {
                                    if (leftRuns.contains((long)f.getRunId())) {
                                        leftFeatures.put((long) f.getRunId(), List.of(f));
                                    } else if (rightRuns.contains((long)f.getRunId())) {
                                        rightFeatures.put((long) f.getRunId(), List.of(f));
                                    }
                                });
                                updateProgress(total.get(), progress.addAndGet(1));
                                if (leftFeatures.isEmpty() || rightFeatures.isEmpty()) {
                                    continue;
                                }

                                double leftval = aggregate(quantify(leftFeatures));
                                double rightval = aggregate(quantify(rightFeatures));
                                double foldChange = Double.isFinite(rightval) ? leftval / rightval : 0.0;

                                foldChanges.add(FoldChange.AlignedFeaturesFoldChange
                                        .builder()
                                        .alignedFeatureId(af)
                                        .foldChange(foldChange)
                                        .leftGroup(left)
                                        .rightGroup(right)
                                        .aggregation(aggregation)
                                        .quantification(quantification)
                                        .build()
                                );
                            }
                            project.storage().insertAll(foldChanges);
                            updateProgress(total.get(), progress.addAndGet(1));
                            return null;
                        }
                    };
                    SiriusJobs.getGlobalJobManager().submitJob(job);
                    return job;
                }

                private DoubleStream quantify(Long2ObjectMap<List<Feature>> features) {
                    return features.values().stream().mapToDouble(featuresPerRun -> switch (quantification) {
                        case APEX_INTENSITY -> featuresPerRun.stream().mapToDouble(Feature::getApexIntensity).sum();
                        case AREA_UNDER_CURVE -> featuresPerRun.stream().mapToDouble(Feature::getAreaUnderCurve).sum();
                    });
                }

                private double aggregate(DoubleStream values) {
                    return switch (aggregation) {
                        case AVG -> values.average().orElse(Double.POSITIVE_INFINITY);
                        case MIN -> values.min().orElse(Double.POSITIVE_INFINITY);
                        case MAX -> values.max().orElse(Double.POSITIVE_INFINITY);
                    };
                }

                private <F extends FoldChange> void cleanupFoldChanges(Class<F> clazz) throws IOException {
                    project.storage().removeAll(
                            Filter.and(
                                    Filter.where("left").eq(left),
                                    Filter.where("right").eq(right),
                                    Filter.where("aggregation").eq(aggregation),
                                    Filter.where("quantification").eq(quantification)
                            ),
                            clazz
                    );
                }

            };
            scheduler.addJobProgressListener(progressSupport);

            SiriusJobs.getGlobalJobManager().submitJob(scheduler).awaitResult();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
