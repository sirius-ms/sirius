package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

public class SmallestLossScorer2 implements PeakPairScorer{

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
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
        for (int fragment=0; fragment < peaks.size(); ++fragment) {
            final double fragmentMass = peaks.get(fragment).getMass();
            double smallestPenalty = Double.NEGATIVE_INFINITY;
            for (int parent=fragment+1; parent < peaks.size(); ++parent) {
                if (peaks.get(parent).getRelativeIntensity() < 0.03) continue;
                final double parentMass = peaks.get(parent).getMass();
                final double diff = parentMass-fragmentMass;
                if (diff < 3) continue;
                smallestPenalty = Math.max(smallestPenalty,  Math.log(Math.max(1e-12, distribution.getDensity(diff))) - normalization);
            }
            if (!Double.isInfinite(smallestPenalty) && smallestPenalty < -0.75) {
                final double score = Math.max(0, -0.2 + -0.9*smallestPenalty);
                for (int parent=fragment+1; parent < peaks.size(); ++parent) {
                    scores[parent][fragment] += score;
                }
            }
        }
    }
}
