package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class ZodiacLibraryScoring implements Ms2ExperimentAnnotation {

    /**
     * Lambda used in the scoring function of spectral library hits. The higher this value the higher are librar hits weighted in ZODIAC scoring.
     */
    @DefaultProperty public final double lambda; //1000

    /**
     * Spectral library hits must have at least this cosine or higher to be considered in scoring. Value must be in [0,1].
     */
    @DefaultProperty public final double minCosine; //0.5

    public ZodiacLibraryScoring() {
        lambda = Double.NaN;
        minCosine = Double.NaN;
    }

    public ZodiacLibraryScoring(double lambda, double minCosine) {
        this.lambda = lambda;
        this.minCosine = minCosine;
    }
}
