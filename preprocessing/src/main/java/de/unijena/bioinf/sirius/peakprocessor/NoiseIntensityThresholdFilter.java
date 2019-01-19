package de.unijena.bioinf.sirius.peakprocessor;

import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.NoiseThresholdSettings;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * Keeps only the K most intensive peaks and delete all peaks with intensity below given threshold
 */
public class NoiseIntensityThresholdFilter implements MergedSpectrumProcessor {

    @Override
    public void process(ProcessedInput input) {

        final NoiseThresholdSettings settings = input.getAnnotationOrDefault(NoiseThresholdSettings.class);
        final ProcessedPeak parent = input.getParentPeak();
        input.getMergedPeaks().removeIf(peak -> peak!=parent && peak.getRelativeIntensity() < settings.intensityThreshold);
        if (input.getMergedPeaks().size()>settings.maximalNumberOfPeaks) {
            final TDoubleArrayList intensities = new TDoubleArrayList(input.getMergedPeaks().size());
            for (ProcessedPeak p : input.getMergedPeaks()) intensities.add(p.getRelativeIntensity());
            intensities.sort();
            double threshold = intensities.get(intensities.size()-settings.maximalNumberOfPeaks);
            input.getMergedPeaks().removeIf(peak -> peak!=parent && peak.getRelativeIntensity() <= threshold);
        }

        input.resetIndizes();

    }
}
