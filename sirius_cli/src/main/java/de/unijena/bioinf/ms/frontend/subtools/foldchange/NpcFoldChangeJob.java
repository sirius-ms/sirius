package de.unijena.bioinf.ms.frontend.subtools.foldchange;

import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.*;

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
        Map<Integer, List<AlignedFeatures>> classFeaturesMap = new HashMap<>();
        for (AlignedFeatures af : alignedFeatures) {
            for (NPCFingerprintVersion.NPCProperty p : getNPCProperties(af))
                classFeaturesMap.computeIfAbsent(p.npcIndex, k -> new ArrayList<>()).add(af);
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
            if (leftFeatures.isEmpty() || rightFeatures.isEmpty()) continue;

            for (AggregationType aggregationType : aggregationTypes) {
                for (QuantMeasure quantMeasure : quantMeasures) {
                    double leftval = aggregate(quantify(leftFeatures, quantMeasure), aggregationType);
                    double rightval = aggregate(quantify(rightFeatures, quantMeasure), aggregationType);
                    double foldChange = (rightval > 0) ? (leftval / rightval) : (leftval > 0 ? Double.POSITIVE_INFINITY : 1.0);
                    foldChanges.add(FoldChange.NpcFoldChange.builder()
                            .npcIndex(npcIndex)
                            .foldChange(foldChange)
                            .leftAbundance(leftval)
                            .rightAbundance(rightval)
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

    private List<NPCFingerprintVersion.NPCProperty> getNPCProperties(AlignedFeatures af) {
        List<NPCFingerprintVersion.NPCProperty> propertyList = new ArrayList<>();
        Optional<FormulaCandidate> topFormula = project.findTopFormulaCandidateByFeatureId(af.getAlignedFeatureId());
        if (topFormula.isEmpty()) return propertyList;
        project.findCanopusResult(af.getAlignedFeatureId(), topFormula.get().getFormulaId()).findFirst()
                .ifPresent(result -> {
                    for (FPIter fpIter : result.getNpcFingerprint())
                        if (fpIter.getProbability() >= probabilityCutoff)
                            propertyList.add((NPCFingerprintVersion.NPCProperty) fpIter.getMolecularProperty());
                });
        return propertyList;
    }
}