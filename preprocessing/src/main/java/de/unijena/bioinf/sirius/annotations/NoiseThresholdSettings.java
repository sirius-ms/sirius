package de.unijena.bioinf.sirius.annotations;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class NoiseThresholdSettings implements Ms2ExperimentAnnotation  {

    @DefaultProperty public final double intensityThreshold;
    @DefaultProperty public final int maximalNumberOfPeaks;

    public NoiseThresholdSettings(double intensityThreshold, int maximalNumberOfPeaks) {
        this.intensityThreshold = intensityThreshold;
        this.maximalNumberOfPeaks = maximalNumberOfPeaks;
    }

    NoiseThresholdSettings() {
        this(0,0);
    }
}
