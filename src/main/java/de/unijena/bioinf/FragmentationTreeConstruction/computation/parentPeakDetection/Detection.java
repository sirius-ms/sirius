package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

/**
 * @author Kai Dührkop
 */
public class Detection {

    private final ProcessedPeak parentPeak;
    private final boolean synthetic;

    public Detection(ProcessedPeak parentPeak, boolean synthetic) {
        this.parentPeak = parentPeak;
        this.synthetic = synthetic;
    }

    public Detection(ProcessedPeak parentPeak) {
        this.parentPeak = parentPeak;
        this.synthetic = true;
    }

    public ProcessedPeak getParentPeak() {
        return parentPeak;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
