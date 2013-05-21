package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.List;

public class NoiseThresholdFilter implements PostProcessor {

    private double threshold;

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
}
