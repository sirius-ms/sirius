package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.util.Arrays;

public class MergedPeak implements Peak {

    private final double mass;
    private final float intensity;
    private final TrackablePeak[] peaks;

    public MergedPeak(MergedPeak peak, TrackablePeak... toMerge) {
        this(concat(peak.peaks, toMerge));
    }

    public MergedPeak(TrackablePeak[] peaks) {
        this.peaks = peaks;
        double intensity = Arrays.stream(peaks).mapToDouble(TrackablePeak::getWeightedIntensity).sum();
        this.mass = Arrays.stream(peaks).mapToDouble(x->x.getWeightedIntensity()*x.getMass()).sum()/intensity;
        this.intensity = (float)intensity;
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public double getIntensity() {
        return intensity;
    }

    private static TrackablePeak[] concat(TrackablePeak[] peaks, TrackablePeak[] toMerge) {
        TrackablePeak[] trackablePeaks = Arrays.copyOf(peaks, peaks.length + toMerge.length);
        System.arraycopy(toMerge, 0, trackablePeaks, peaks.length, toMerge.length);
        return trackablePeaks;
    }
}
