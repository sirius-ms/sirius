package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.ms.Ms2ExperimentAnnotation;

/**
 * Determines how strong the isotope pattern score influences the final scoring
 */
public class IsotopeScoring implements Ms2ExperimentAnnotation {

    private final double weighting;

    public final static IsotopeScoring DEFAULT = new IsotopeScoring(1d), DISABLED = new IsotopeScoring(0d);

    public IsotopeScoring(double weighting) {
        this.weighting = weighting;
    }

    public double getIsotopeScoreWeighting() {
        return weighting;
    }
}
