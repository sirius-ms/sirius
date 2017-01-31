package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class SmallestLossScorer implements LossScorer<Object>{

    private DensityFunction distribution;
    private double expectationValue;
    private double normalization;

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.distribution = (DensityFunction)helper.unwrap(document, document.getFromDictionary(dictionary, "distribution"));
        this.expectationValue = document.getDoubleFromDictionary(dictionary, "expectationValue");
        this.normalization = document.getDoubleFromDictionary(dictionary, "normalization");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "distribution", helper.wrap(document, distribution));
        document.addToDictionary(dictionary, "normalization", normalization);
        document.addToDictionary(dictionary, "expectationValue", expectationValue);
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        if (loss.getFormula().getMass() < 2.5) return 0d;
        final Fragment target = loss.getTarget();
        double minMass = Double.POSITIVE_INFINITY;
        for (Loss l : target.getIncomingEdges()) {
            final double m = l.getFormula().getMass();
            if (m > 2.5 && input.getMergedPeaks().get(l.getSource().getColor()).getRelativeIntensity() >= 0.02) // ignore H and H2 losses as well as peaks with intensity below 2 %
                minMass = Math.min(minMass, m);
        }
        if (Double.isInfinite(minMass)) return 0d;
        minMass = Math.max(minMass, expectationValue);
        final double penalty = Math.log(Math.max(1e-12, distribution.getDensity(minMass))) - normalization;
        if (penalty < -1) return (-1-0.9*penalty);
        else return 0d;
    }
}
