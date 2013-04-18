package de.unijena.bioinf.FragmentationTreeConstruction.computation.merging;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

public class OldSiriusMerger implements PeakMerger {

    private final double mergeTreshold;

    public OldSiriusMerger() {
        this(0.1d);
    }

    public OldSiriusMerger(double mergeTreshold) {
        this.mergeTreshold = mergeTreshold;
    }

    public double getMergeTreshold() {
        return mergeTreshold;
    }

    @Override
    public void mergePeaks(List<ProcessedPeak> peaks, ProcessedInput input, Merger merger) {
        throw new RuntimeException(); // TODO: implement
    }
}
