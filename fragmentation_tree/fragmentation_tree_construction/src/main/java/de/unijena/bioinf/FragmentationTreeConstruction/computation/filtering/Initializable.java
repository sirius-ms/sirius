package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;

public interface Initializable {
    /**
     * is called after parsing all scorer and preprocessor such that each of them can access each other
     * and do some special initialization stuff.
     */
    void initialize(FragmentationPatternAnalysis analysis);

}
