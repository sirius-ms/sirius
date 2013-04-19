package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * An algorithm which detects a parent peak in the given MS2 experiment.
 */
public interface ParentPeakDetector {

    public Detection detectParentPeak(Ms2Experiment experiment);

}
