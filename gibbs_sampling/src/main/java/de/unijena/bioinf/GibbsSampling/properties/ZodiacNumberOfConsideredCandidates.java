package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC.
 * If lower than 0, all available candidates are considered.
 */
@DefaultProperty
public class ZodiacNumberOfConsideredCandidates implements Ms2ExperimentAnnotation {
    public final int value;

    public ZodiacNumberOfConsideredCandidates() {
        this.value = -1;
    }

    public ZodiacNumberOfConsideredCandidates(int numberOfCandidates) {
        this.value = numberOfCandidates;
    }
}
