package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.Arrays;
import java.util.List;

public class DynamicTreeSizeScorer implements PeakScorer {

    private double signalThreshold;


    public DynamicTreeSizeScorer(double signalThreshold) {
        this.signalThreshold = signalThreshold;
    }

    public DynamicTreeSizeScorer() {
        this(0.025);
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        final double p = signalPropability(peaks);
        final double score = Math.log(p) - Math.log(1 - p);
        System.err.println(score);
        for (int i=0; i < scores.length; ++i)
            scores[i] += score;
    }

    private double signalPropability(List<ProcessedPeak> peaks) {
        int peaksAbove = 0;
        int peaksBelow = 0;
        for (ProcessedPeak p : peaks) {
            if (p.getRelativeIntensity() > signalThreshold) {
                ++peaksAbove;
            } else {
                ++peaksBelow;
            }
        }
        return Math.max(0.2d, Math.min(0.8d, (double) peaksAbove / (double) (peaksAbove + peaksBelow)));
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        signalThreshold = document.getDoubleFromDictionary(dictionary, "signalThreshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "signalThreshold", signalThreshold);
    }
}
