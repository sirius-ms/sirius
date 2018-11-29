package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * Determines how strong the isotope pattern score influences the final scoring
 */
@DefaultProperty
public enum IsotopeScoring implements Ms2ExperimentAnnotation {

    DEFAULT(1d),
    DISABLED(0d);

    private final double weighting;


    IsotopeScoring(double weighting) {
        this.weighting = weighting;
    }

    public double getIsotopeScoreWeighting() {
        return weighting;
    }
}
