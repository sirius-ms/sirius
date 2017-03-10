package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

public class NoIsotopePatternPenalty implements PeakScorer {

    protected double lambda;

    public NoIsotopePatternPenalty(double lambda) {
        this.lambda = lambda;
    }

    public NoIsotopePatternPenalty() {
        this(2);
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        for (int k=0; k < peaks.size(); ++k) {
            scores[k] -= lambda*peaks.get(k).getRelativeIntensity();
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.lambda = document.getDoubleFromDictionary(dictionary, "lambda");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "lambda", lambda);
    }
}
