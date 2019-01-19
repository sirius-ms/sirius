package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.Initializable;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.List;

public class NoIsotopePatternPenalty implements PeakScorer, Initializable {

    protected double lambda;

    public NoIsotopePatternPenalty(double lambda) {
        this.lambda = lambda;
    }

    public NoIsotopePatternPenalty() {
        this(2);
    }

    private FragmentationPatternAnalysis hack;

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        if (hack.isScoringIsotopes(input)) {
            double max = 0d;
            for (ProcessedPeak peak : peaks) {
                max = Math.max(peak.getRelativeIntensity(), max);
            }
            for (int k=0; k < peaks.size(); ++k) {
                scores[k] -= lambda*(peaks.get(k).getRelativeIntensity()/max);
            }
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

    @Override
    public void initialize(FragmentationPatternAnalysis analysis) {
        hack = analysis;
    }
}
