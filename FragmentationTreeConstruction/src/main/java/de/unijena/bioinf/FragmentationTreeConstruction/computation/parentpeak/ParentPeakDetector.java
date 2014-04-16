package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentpeak;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

/**
 * Detects the parent peak in the given experiment.
 * - if the parent peak does not exist, create a synthetic one
 * - if there are multiple peaks that origin the parent peak, decide for one (or merge them). The real peak merging
 * is later done by the MergeStrategy
 * - if there are multiple peaks with different ionizations, decide for one ionization (usually preferring [M+H]+)
 */
public interface ParentPeakDetector {

    public ProcessedPeak detectParentPeak(Ms2Experiment experiment);

}
