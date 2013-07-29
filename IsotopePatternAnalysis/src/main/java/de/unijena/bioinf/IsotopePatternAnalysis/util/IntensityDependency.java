package de.unijena.bioinf.IsotopePatternAnalysis.util;

import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;

public interface IntensityDependency extends ImmutableParameterized<IntensityDependency> {

    public double getValueAt(double intensity);

}

