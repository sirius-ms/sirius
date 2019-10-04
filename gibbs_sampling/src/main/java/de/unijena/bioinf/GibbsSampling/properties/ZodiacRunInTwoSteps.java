package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * As default ZODIAC runs a 2-step approach. First running 'good quality compounds' only, and afterwards including the remaining.
 */
@DefaultProperty
public class ZodiacRunInTwoSteps implements Ms2ExperimentAnnotation {
    public final boolean runTwoStep;

    private ZodiacRunInTwoSteps() {
        runTwoStep = true;
    }

    public ZodiacRunInTwoSteps(boolean runTwoStep) {
        this.runTwoStep = runTwoStep;
    }
}
