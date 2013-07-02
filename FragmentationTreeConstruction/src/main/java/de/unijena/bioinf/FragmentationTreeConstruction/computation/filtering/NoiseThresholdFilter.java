package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.ParameterHelper;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.List;

public class NoiseThresholdFilter implements PostProcessor {

    private double threshold;

    public NoiseThresholdFilter() {
        this(0d);
    }

    public NoiseThresholdFilter(double threshold) {
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public ProcessedInput process(ProcessedInput input) {
        final List<ProcessedPeak> peaks = input.getMergedPeaks();
        final List<ProcessedPeak> filtered = new ArrayList<ProcessedPeak>(peaks.size());
        final ProcessedPeak parent = input.getParentPeak();
        for (ProcessedPeak p : peaks)
            if (p.getRelativeIntensity() >= threshold || p.isSynthetic() || p == parent)
                filtered.add(p);
        return new ProcessedInput(input.getExperimentInformation(), filtered, input.getParentPeak(), input.getParentMassDecompositions(),
                input.getPeakScores(), input.getPeakPairScores());
    }

    @Override
    public Stage getStage() {
        return Stage.AFTER_MERGING;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        threshold = document.getDoubleFromDictionary(dictionary, "threshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "threshold", threshold);
    }

}
