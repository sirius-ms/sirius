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
import de.unijena.bioinf.ms.middleware.service.lucene.LuceneUtils;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.longs.*;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class FoldChangeWorkflow implements Workflow, ProgressSupport {

    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);

    private final NoSQLProjectSpaceManager psm;

    private final String left;

    private final String right;

    private final AggregationType aggregation;

    private final QuantMeasure quantification;

    private final Class<?> target;

    public FoldChangeWorkflow(ProjectSpaceManager psm, String left, String right, AggregationType aggregation, QuantMeasure quantification, Class<?> target) {
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

    @Override
    public void run() {
        try {
            BasicMasterJJob<Boolean> scheduler = new BasicMasterJJob<>(JJob.JobType.SCHEDULER) {
                private final AtomicLong total = new AtomicLong(0);
                private final AtomicLong progress = new AtomicLong(0);

                @Override
                protected Boolean compute() throws Exception {
                    TagGroup leftGroup = psm.getProject().getStorage()
                            .findStr(Filter.where("groupName").eq(left), TagGroup.class)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("No such tag category group: " + left));
                    TagGroup rightGroup = psm.getProject().getStorage()
                            .findStr(Filter.where("groupName").eq(right), TagGroup.class)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("No such tag category group: " + right));

                    //todo can be make use  definitions from cache here somehow?
                    HashMap<String,TagDefinition> definitionMap = new HashMap<>();
                    Stream<TagDefinition> definitions = psm.getProject().findAllTagDefinitionsStr()
                            .peek(tagDef -> definitionMap.put(tagDef.getTagName(), tagDef));
                    StandardQueryParser parser = LuceneUtils.makeDefaultQueryParser(definitions);


                    LongSet leftRuns = new LongRBTreeSet();
                    LongSet rightRuns = new LongRBTreeSet();

                    Filter leftTagFilter;
                    Filter rightTagFilter;
                    try {
                        leftTagFilter = LuceneUtils.translateTagFilter(leftGroup.getLuceneQuery(), parser, definitionMap);
                        rightTagFilter = LuceneUtils.translateTagFilter(rightGroup.getLuceneQuery(), parser, definitionMap);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Parse error: " + leftGroup.getLuceneQuery());
                    }

                    Long[] leftObjectIds = psm.getProject().getStorage().findStr(Filter.and(
                                    Filter.where("taggedObjectClass").eq(LCMSRun.class.getName()), leftTagFilter
                            ), de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class)
                            .map(de.unijena.bioinf.ms.persistence.model.core.tags.Tag::getTaggedObjectId).toArray(Long[]::new);
                    Long[] rightObjectIds = psm.getProject().getStorage().findStr(Filter.and(
                                    Filter.where("taggedObjectClass").eq(LCMSRun.class.getName()), rightTagFilter
                            ), de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class)
                            .map(de.unijena.bioinf.ms.persistence.model.core.tags.Tag::getTaggedObjectId).toArray(Long[]::new);

                    if (leftObjectIds.length == 0 || rightObjectIds.length == 0) {
                        if (AlignedFeature.class.equals(target)) {
                            cleanupFoldChanges(FoldChange.AlignedFeaturesFoldChange.class);
                        } else {
                            cleanupFoldChanges(FoldChange.CompoundFoldChange.class);
                        }

                        return true;
                    }

                    psm.getProject().getStorage().findStr(Filter.where("runId").in(leftObjectIds), LCMSRun.class).forEach(run -> leftRuns.add(run.getRunId()));
                    psm.getProject().getStorage().findStr(Filter.where("runId").in(rightObjectIds), LCMSRun.class).forEach(run -> rightRuns.add(run.getRunId()));

                    AtomicReference<List<BasicJJob<?>>> jobs = new AtomicReference<>(new ArrayList<>());

                    if (AlignedFeature.class.equals(target)) {
                        cleanupFoldChanges(FoldChange.AlignedFeaturesFoldChange.class);

                        AtomicReference<LongSet> features = new AtomicReference<>(new LongArraySet());
                        psm.getProject().getAllAlignedFeatures().forEach(af -> {
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
                        psm.getProject().getAllCompounds().forEach(c -> {
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
                                psm.getProject().fetchAdductFeatures(c);
                                if (c.getAdductFeatures().isPresent()) {
                                    for (AlignedFeatures af : c.getAdductFeatures().get()) {
                                        psm.getProject().fetchFeatures(af);
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
                            psm.getProject().getStorage().insertAll(foldChanges);
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

                                psm.getProject().getStorage().findStr(Filter.where("alignedFeatureId").eq(af), Feature.class).forEach(f -> {
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
                            psm.getProject().getStorage().insertAll(foldChanges);
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
                    psm.getProject().getStorage().removeAll(
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
