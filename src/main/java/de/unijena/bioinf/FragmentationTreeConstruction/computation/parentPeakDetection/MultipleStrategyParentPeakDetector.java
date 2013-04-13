package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * @author Kai Dührkop
 */
public class MultipleStrategyParentPeakDetector implements ParentPeakDetector {

    private final MultipleStrategyParentPeakDetector[] detectors;

    public MultipleStrategyParentPeakDetector(MultipleStrategyParentPeakDetector... detectors) {
        this.detectors = detectors;
    }


    @Override
    public Detection detectParentPeak(ProcessedInput input, List<ProcessedPeak> peaks) {
        Detection d = null;
        for (ParentPeakDetector detector : detectors) {
            final Detection det = detector.detectParentPeak(input, peaks);
            if (!det.isSynthetic()) return det;
            if (d == null) d = det;
        }
        return d;
    }
}
