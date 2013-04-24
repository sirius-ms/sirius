package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.legacy;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NoiseThresholdFilter implements PostProcessedNoisePeakFilter {

    private final double treshold;
    private final String reason;

    public NoiseThresholdFilter(double treshold) {
        this.treshold = treshold;
        this.reason = " has intensity below " + treshold;
    }

    @Override
    public List<ProcessedPeak> filter(ProcessedInput input, MSExperimentInformation informations, List<ProcessedPeak> peaks, NoisePeakCallback callback) {
        final Iterator<ProcessedPeak> iter = peaks.iterator();
        final ArrayList<ProcessedPeak> noise = new ArrayList<ProcessedPeak>();
        while (iter.hasNext()) {
            final ProcessedPeak peak = iter.next();
            if (peak.getRelativeIntensity() <= treshold) {
                noise.add(peak);
                for (MS2Peak p : peak.getOriginalPeaks()) callback.reportNoise(p, reason);
            }
        }
        return noise;
    }
}
