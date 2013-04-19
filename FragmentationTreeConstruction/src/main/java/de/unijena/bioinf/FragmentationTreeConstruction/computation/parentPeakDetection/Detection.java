package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

/**
 * @author Kai Dührkop
 */
public class Detection {

    private final Peak parentPeak;
    private final boolean synthetic;

    public Detection(Peak parentPeak, boolean synthetic) {
        this.parentPeak = parentPeak;
        this.synthetic = synthetic;
    }

    public Detection(Peak parentPeak) {
        this.parentPeak = parentPeak;
        this.synthetic = true;
    }

    public Peak getParentPeak() {
        return parentPeak;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
