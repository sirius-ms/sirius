package de.unijena.bioinf.ms.frontend.subtools.foldchange;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class AlignedFeaturesFoldChangeJob extends FoldChangeSubToolJJob<FoldChange.AlignedFeaturesFoldChange> {
    LongSet alignedFeatureIds;

    public AlignedFeaturesFoldChangeJob(SiriusProjectDocumentDatabase<? extends Database<?>> project, String leftGroupName, LongSet leftRuns, String rightGroupName, LongSet rightRuns, QuantMeasure quantMeasure, AggregationType aggregationType, List<AlignedFeatures> alignedFeatures) {
        super(project, leftGroupName, leftRuns, rightGroupName, rightRuns, quantMeasure, aggregationType);
        this.alignedFeatureIds = alignedFeatures.stream().map(AlignedFeatures::getAlignedFeatureId).collect(Collectors.toCollection(LongOpenHashSet::new));;
    }

    public AlignedFeaturesFoldChangeJob(SiriusProjectDocumentDatabase<? extends Database<?>> project, String leftGroupName, LongSet leftRuns, String rightGroupName, LongSet rightRuns, EnumSet<QuantMeasure> quantMeasures, EnumSet<AggregationType> aggregationTypes, List<AlignedFeatures> alignedFeatures) {
        super(project, leftGroupName, leftRuns, rightGroupName, rightRuns, quantMeasures, aggregationTypes);
        this.alignedFeatureIds = alignedFeatures.stream().map(AlignedFeatures::getAlignedFeatureId).collect(Collectors.toCollection(LongOpenHashSet::new));
    }

    public AlignedFeaturesFoldChangeJob(SiriusProjectDocumentDatabase<? extends Database<?>> project, String leftGroupName, LongSet leftRuns, String rightGroupName, LongSet rightRuns, QuantMeasure quantMeasure, AggregationType aggregationType, LongSet alignedFeatureIds) {
        super(project, leftGroupName, leftRuns, rightGroupName, rightRuns, quantMeasure, aggregationType);
        this.alignedFeatureIds = alignedFeatureIds;
    }

    public AlignedFeaturesFoldChangeJob(SiriusProjectDocumentDatabase<? extends Database<?>> project, String leftGroupName, LongSet leftRuns, String rightGroupName, LongSet rightRuns, EnumSet<QuantMeasure> quantMeasures, EnumSet<AggregationType> aggregationTypes, LongSet alignedFeatureIds) {
        super(project, leftGroupName, leftRuns, rightGroupName, rightRuns, quantMeasures, aggregationTypes);
        this.alignedFeatureIds = alignedFeatureIds;
    }

    @Override
    protected List<FoldChange.AlignedFeaturesFoldChange> compute() throws Exception {
        total.set(alignedFeatureIds.size());
        List<FoldChange.AlignedFeaturesFoldChange> foldChanges = new ArrayList<>();
        for (long af : alignedFeatureIds) {
            Long2ObjectMap<List<Feature>> leftFeatures = new Long2ObjectOpenHashMap<>(leftRuns.size());
            Long2ObjectMap<List<Feature>> rightFeatures = new Long2ObjectOpenHashMap<>(rightRuns.size());

            project.findByFeatureId(af, Feature.class).forEach(f -> {
                if (leftRuns.contains((long) f.getRunId())) {
                    leftFeatures.put((long) f.getRunId(), List.of(f));
                } else if (rightRuns.contains((long) f.getRunId())) {
                    rightFeatures.put((long) f.getRunId(), List.of(f));
                }
            });

            updateProgress(total.get(), progress.addAndGet(1));

            for (AggregationType aggregationType : aggregationTypes) {
                for (QuantMeasure quantMeasure : quantMeasures) {
                    double leftAbundance = aggregate(quantify(leftFeatures, quantMeasure), aggregationType);
                    double rightAbundance = aggregate(quantify(rightFeatures, quantMeasure), aggregationType);

                    foldChanges.add(FoldChange.AlignedFeaturesFoldChange
                            .builder()
                            .alignedFeatureId(af)
                            .leftAbundance(leftAbundance)
                            .rightAbundance(rightAbundance)
                            .leftGroup(leftGroupName)
                            .rightGroup(rightGroupName)
                            .aggregation(aggregationType)
                            .quantification(quantMeasure)
                            .build()
                    );
                }
            }
        }
        project.getStorage().insertAll(foldChanges);
        updateProgress(total.get(), progress.addAndGet(1));
        return foldChanges;
    }
}
