package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * Ratio of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are forced for each ionization to be considered by ZODIAC.
 * This depends on the number of candidates ZODIAC considers. E.g. if 50 candidates are considered and a ratio of 0.2 is set, at least 10 candidates per ionization will be considered, which might increase the number of candidates above 50.
 */
@DefaultProperty
public class ZodiacRatioOfConsideredCandidatesPerIonization implements Ms2ExperimentAnnotation {
    public final double value;

    public ZodiacRatioOfConsideredCandidatesPerIonization() {
        this.value = 0.2;
    }

    public ZodiacRatioOfConsideredCandidatesPerIonization(double ratioOfCandidatesForcedPerAdduct) {
        this.value = ratioOfCandidatesForcedPerAdduct;
    }
}
