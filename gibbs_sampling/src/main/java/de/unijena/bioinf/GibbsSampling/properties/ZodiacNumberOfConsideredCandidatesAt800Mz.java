package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC.
 * This is the threshold used for all compounds with mz above 800 m/z and is used to interpolate the number of candidates for smaller compounds.
 * If lower than 0, all available candidates are considered.
 */
@DefaultProperty
public class ZodiacNumberOfConsideredCandidatesAt800Mz implements Ms2ExperimentAnnotation {
    public final int value;

    public ZodiacNumberOfConsideredCandidatesAt800Mz() {
        this.value = 50;
    }

    public ZodiacNumberOfConsideredCandidatesAt800Mz(int numberOfCandidates) {
        this.value = numberOfCandidates;
    }
}
