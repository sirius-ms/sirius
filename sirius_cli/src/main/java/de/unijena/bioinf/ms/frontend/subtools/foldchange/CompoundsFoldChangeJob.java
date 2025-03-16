package de.unijena.bioinf.ms.frontend.subtools.foldchange;

import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CompoundsFoldChangeJob extends FoldChangeSubToolJJob<FoldChange.CompoundFoldChange> {
    protected final List<Compound> compounds;

    public CompoundsFoldChangeJob(SiriusProjectDocumentDatabase<? extends Database<?>> project, String leftGroupName, LongSet leftRuns, String rightGroupName, LongSet rightRuns, QuantMeasure quantMeasure, AggregationType aggregationType, List<Compound> compounds) {
        super(project, leftGroupName, leftRuns, rightGroupName, rightRuns, quantMeasure, aggregationType);
        this.compounds = compounds;
    }

    public CompoundsFoldChangeJob(SiriusProjectDocumentDatabase<? extends Database<?>> project, String leftGroupName, LongSet leftRuns, String rightGroupName, LongSet rightRuns, EnumSet<QuantMeasure> quantMeasures, EnumSet<AggregationType> aggregationTypes, List<Compound> compounds) {
        super(project, leftGroupName, leftRuns, rightGroupName, rightRuns, quantMeasures, aggregationTypes);
        this.compounds = compounds;
    }

    @Override
    protected List<FoldChange.CompoundFoldChange> compute() throws Exception {
        List<FoldChange.CompoundFoldChange> foldChanges = new ArrayList<>();
        for (Compound c : compounds) {
            Long2ObjectMap<List<Feature>> leftFeatures = new Long2ObjectOpenHashMap<>(leftRuns.size());
            Long2ObjectMap<List<Feature>> rightFeatures = new Long2ObjectOpenHashMap<>(rightRuns.size());
            project.fetchAdductFeatures(c);
            if (c.getAdductFeatures().isPresent()) {
                for (AlignedFeatures af : c.getAdductFeatures().get()) {
                    project.fetchFeatures(af);
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

            for (AggregationType aggregationType : aggregationTypes) {
                for (QuantMeasure quantMeasure : quantMeasures) {
                    double leftval = aggregate(quantify(leftFeatures, quantMeasure), aggregationType);
                    double rightval = aggregate(quantify(rightFeatures, quantMeasure), aggregationType);
                    double foldChange = rightval > 0 ? leftval / rightval : 0.0;

                    foldChanges.add(FoldChange.CompoundFoldChange
                            .builder()
                            .compoundId(c.getCompoundId())
                            .foldChange(foldChange)
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
