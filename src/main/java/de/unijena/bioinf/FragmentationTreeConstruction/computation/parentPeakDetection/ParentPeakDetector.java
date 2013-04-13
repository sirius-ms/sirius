package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * @author Kai Dührkop
 */
public interface ParentPeakDetector {

    public Detection detectParentPeak(ProcessedInput input, List<ProcessedPeak> peaks);

}
