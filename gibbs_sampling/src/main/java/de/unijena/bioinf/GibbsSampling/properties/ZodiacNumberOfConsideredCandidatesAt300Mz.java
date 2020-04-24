package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC.
 * This is the threshold used for all compounds with mz below 300 m/z and is used to interpolate the number of candidates for larger compounds.
 * If lower than 0, all available candidates are considered.
 */
@DefaultProperty
public class ZodiacNumberOfConsideredCandidatesAt300Mz implements Ms2ExperimentAnnotation {
    public final int value;

    public ZodiacNumberOfConsideredCandidatesAt300Mz() {
        this.value = 10;
    }

    public ZodiacNumberOfConsideredCandidatesAt300Mz(int numberOfCandidates) {
        this.value = numberOfCandidates;
    }
}
