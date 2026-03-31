package de.unijena.bioinf.ms.frontend.subtools.foldchange;

import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
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

public class ClassyfireFoldChangeJob extends FoldChangeSubToolJJob<FoldChange.ClassyfireFoldChange> {
    protected final List<AlignedFeatures> alignedFeatures;
    protected final double probabilityCutoff;

    public ClassyfireFoldChangeJob(SiriusProjectDocumentDatabase<? extends Database<?>> project,
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
    protected List<FoldChange.ClassyfireFoldChange> compute() throws Exception {
        Map<Integer, List<AlignedFeatures>> classFeaturesMap = new HashMap<>();
        for (AlignedFeatures af : alignedFeatures) {
            for (ClassyfireProperty p : getClassyfireProperties(af))
                classFeaturesMap.computeIfAbsent(p.getChemOntId(), k -> new ArrayList<>()).add(af);
        }

        List<FoldChange.ClassyfireFoldChange> foldChanges = new ArrayList<>();
        for (Map.Entry<Integer, List<AlignedFeatures>> entry : classFeaturesMap.entrySet()) {
            int cfIndex = entry.getKey();
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
                    double leftAbundance = aggregate(quantify(leftFeatures, quantMeasure), aggregationType);
                    double rightAbundance = aggregate(quantify(rightFeatures, quantMeasure), aggregationType);

                    foldChanges.add(FoldChange.ClassyfireFoldChange.builder()
                            .classyfireIndex(cfIndex)
                            .leftAbundance(leftAbundance)
                            .rightAbundance(rightAbundance)
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

    private List<ClassyfireProperty> getClassyfireProperties(AlignedFeatures af) {
        List<ClassyfireProperty> propertyList = new ArrayList<>();
        Optional<FormulaCandidate> topFormula = project.findTopFormulaCandidateByFeatureId(af.getAlignedFeatureId());
        if (topFormula.isEmpty()) return propertyList;
        project.findCanopusResult(af.getAlignedFeatureId(), topFormula.get().getFormulaId()).findFirst()
                .ifPresent(result -> {
                    for (FPIter fpIter : result.getCfFingerprint())
                        if (fpIter.getProbability() >= probabilityCutoff)
                            propertyList.add((ClassyfireProperty) fpIter.getMolecularProperty());
                });
        return propertyList;
    }
}