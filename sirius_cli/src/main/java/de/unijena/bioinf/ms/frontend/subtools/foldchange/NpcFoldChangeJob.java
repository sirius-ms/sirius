package de.unijena.bioinf.ms.frontend.subtools.foldchange;

import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.*;
import java.util.stream.Collectors;

public class NpcFoldChangeJob extends FoldChangeSubToolJJob<FoldChange.NpcFoldChange> {
    protected final List<AlignedFeatures> alignedFeatures;
    protected final double probabilityCutoff;

    public NpcFoldChangeJob(SiriusProjectDocumentDatabase<? extends Database<?>> project,
                            String leftGroupName, LongSet leftRuns,
                            String rightGroupName, LongSet rightRuns,
                            EnumSet<QuantMeasure> quantMeasures,
                            EnumSet<AggregationType> aggregationTypes,
                            List<AlignedFeatures> alignedFeatures,
                            double probabilityCutoff) {
        super(project, leftGroupName, leftRuns, rightGroupName, rightRuns, quantMeasures, aggregationTypes);
        this.alignedFeatures = alignedFeatures;
        this.probabilityCutoff = probabilityCutoff;
    }

    @Override
    protected List<FoldChange.NpcFoldChange> compute() throws Exception {
        Map<Long, List<NPCFingerprintVersion.NPCProperty>> propertiesMap = new HashMap<>();
        for (List<AlignedFeatures> batch : Partition.ofSize(alignedFeatures, 500)) {
            Long[] ids = batch.stream().map(AlignedFeatures::getAlignedFeatureId).toArray(Long[]::new);
            Map<Long, FormulaCandidate> topFormulas = project.getStorage().findStr(
                    Filter.and(Filter.where("alignedFeatureId").in(ids), Filter.where("formulaRank").eq(1)),
                    FormulaCandidate.class
            ).collect(Collectors.toMap(FormulaCandidate::getAlignedFeatureId, f -> f, (a, b) -> a));

            if (!topFormulas.isEmpty()) {
                project.getStorage().findStr(
                        Filter.where("formulaId").in(topFormulas.values().stream().map(FormulaCandidate::getFormulaId).toArray(Long[]::new)),
                        CanopusPrediction.class
                ).forEach(res -> {
                    List<NPCFingerprintVersion.NPCProperty> props = new ArrayList<>();
                    for (FPIter fpIter : res.getNpcFingerprint())
                        if (fpIter.getProbability() >= probabilityCutoff)
                            props.add((NPCFingerprintVersion.NPCProperty) fpIter.getMolecularProperty());
                    propertiesMap.put(res.getAlignedFeatureId(), props);
                });
            }
        }

        Map<Integer, List<AlignedFeatures>> classFeaturesMap = new HashMap<>();
        for (AlignedFeatures af : alignedFeatures) {
            List<NPCFingerprintVersion.NPCProperty> props = propertiesMap.get(af.getAlignedFeatureId());
            if (props != null) {
                for (NPCFingerprintVersion.NPCProperty p : props)
                    classFeaturesMap.computeIfAbsent(p.npcIndex, k -> new ArrayList<>()).add(af);
            }
        }

        total.set(classFeaturesMap.size());

        List<FoldChange.NpcFoldChange> foldChanges = new ArrayList<>();
        for (Map.Entry<Integer, List<AlignedFeatures>> entry : classFeaturesMap.entrySet()) {
            int npcIndex = entry.getKey();
            Long2ObjectMap<List<Feature>> leftFeatures = new Long2ObjectOpenHashMap<>(leftRuns.size());
            Long2ObjectMap<List<Feature>> rightFeatures = new Long2ObjectOpenHashMap<>(rightRuns.size());

            for (AlignedFeatures af : entry.getValue()) {
                project.fetchFeatures(af);
                if (af.getFeatures().isPresent()) {
                    for (Feature f : af.getFeatures().get()) {
                        if (leftRuns.contains((long) f.getRunId()))
                            leftFeatures.computeIfAbsent(f.getRunId(), k -> new ArrayList<>()).add(f);
                        else if (rightRuns.contains((long) f.getRunId()))
                            rightFeatures.computeIfAbsent(f.getRunId(), k -> new ArrayList<>()).add(f);
                    }
                }
            }

            updateProgress(total.get(), progress.addAndGet(1));


            for (AggregationType aggregationType : aggregationTypes) {
                for (QuantMeasure quantMeasure : quantMeasures) {
                    double leftval = aggregate(quantify(leftFeatures, quantMeasure), aggregationType);
                    double rightval = aggregate(quantify(rightFeatures, quantMeasure), aggregationType);
                    double foldChange = (rightval > 0) ? (leftval / rightval) : (leftval > 0 ? Double.POSITIVE_INFINITY : 1.0);

                    foldChanges.add(FoldChange.NpcFoldChange.builder()
                            .npcIndex(npcIndex)
                            .foldChange(foldChange)
                            .leftGroup(leftGroupName)
                            .rightGroup(rightGroupName)
                            .aggregation(aggregationType)
                            .quantification(quantMeasure)
                            .build());
                }
            }
        }
        project.getStorage().insertAll(foldChanges);
        updateProgress(total.get(), progress.addAndGet(1));
        return foldChanges;
    }
}
