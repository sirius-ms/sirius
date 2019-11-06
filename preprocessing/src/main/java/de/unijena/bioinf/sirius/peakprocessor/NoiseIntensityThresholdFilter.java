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
        double base = 0d;
        switch (settings.basePeak) {
            case LARGEST:
                for (ProcessedPeak p : input.getMergedPeaks())
                    base = Math.max(p.getRelativeIntensity(),base);
                break;
            case NOT_PRECURSOR:
                final double pm = (parent.getMass()-0.5d);
                for (ProcessedPeak p : input.getMergedPeaks())
                    if (p.getMass() < pm)
                        base = Math.max(p.getRelativeIntensity(),base);
                break;
            case SECOND_LARGEST:
                double a = Double.NEGATIVE_INFINITY; double b = Double.NEGATIVE_INFINITY;
                for (ProcessedPeak p : input.getMergedPeaks()) {
                    if (p.getRelativeIntensity()>a) {
                        b = a;
                        a = p.getRelativeIntensity();
                    } else if (p.getRelativeIntensity() > b) {
                        b = p.getRelativeIntensity();
                    }
                }
                base = b;
                break;
            default: base = 1d;
        }
        final double scale = base;
        input.getMergedPeaks().removeIf(peak -> peak!=parent && ((peak.getSumIntensity() < settings.absoluteThreshold) || (peak.getRelativeIntensity()/scale) < settings.intensityThreshold));
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
