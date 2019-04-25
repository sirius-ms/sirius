package de.unijena.bioinf.sirius.annotations;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class NoiseThresholdSettings implements Ms2ExperimentAnnotation  {

    public static enum BASE_PEAK {
        LARGEST,
        NOT_PRECURSOR,
        SECOND_LARGEST
    };

    @DefaultProperty public final double intensityThreshold;
    @DefaultProperty public final int maximalNumberOfPeaks;
    @DefaultProperty public final BASE_PEAK basePeak;
    @DefaultProperty public final double absoluteThreshold;

    public NoiseThresholdSettings(double intensityThreshold, int maximalNumberOfPeaks, BASE_PEAK basePeak, double absoluteThreshold) {
        this.intensityThreshold = intensityThreshold;
        this.maximalNumberOfPeaks = maximalNumberOfPeaks;
        this.basePeak = basePeak;
        this.absoluteThreshold = absoluteThreshold;
    }

    NoiseThresholdSettings() {
        this(0,0,BASE_PEAK.NOT_PRECURSOR,0d);
    }

    public NoiseThresholdSettings withPeakLimit(int maximalNumberOfPeaks) {
        return new NoiseThresholdSettings(intensityThreshold,maximalNumberOfPeaks,basePeak,absoluteThreshold);
    }
    public NoiseThresholdSettings withAbsoluteThreshold(int absoluteThreshold) {
        return new NoiseThresholdSettings(intensityThreshold,maximalNumberOfPeaks,basePeak,absoluteThreshold);
    }
    public NoiseThresholdSettings withIntensityThreshold(int intensityThreshold) {
        return new NoiseThresholdSettings(intensityThreshold,maximalNumberOfPeaks,basePeak,intensityThreshold);
    }
    public NoiseThresholdSettings withBasePeak(BASE_PEAK basePeak) {
        return new NoiseThresholdSettings(intensityThreshold,maximalNumberOfPeaks,basePeak,intensityThreshold);
    }
}
