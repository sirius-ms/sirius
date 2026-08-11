package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.spectrum.SpectrumStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

/**
 * Noise level of a single DIA channel.
 * <p>
 * The sample wide MS2 noise level is of little use for DIA: the total ion current of a window that covers the
 * crowded low mass region and one that covers the sparse high mass region differ by orders of magnitude, and a
 * single pooled estimate is too high for the one and too low for the other. Every channel therefore gets its own.
 * <p>
 * The estimate itself follows the sample wide one in
 * {@link de.unijena.bioinf.lcms.statistics.MedianNoiseCollectionStrategy}: an upper quantile of the intensities of
 * a scan, pulled towards the smallest intensity that was still recorded, then robustly averaged over all scans of
 * the channel.
 */
public final class DiaChannelNoise {

    /** scans with fewer peaks than this carry no usable intensity distribution */
    private static final int MIN_PEAKS_PER_SCAN = 10;

    /**
     * Which quantile of a scan's intensities to treat as the noise floor.
     * <p>
     * The sample wide MS2 estimator uses the upper quartile, on the assumption that most peaks of a scan are
     * noise. That does not hold for a DIA window: it holds the fragments of everything co-eluting within a hundred
     * Dalton, so most of its peaks are real signal of some compound and an upper quantile measures signal rather
     * than noise. On the test file the quartile came out between 12000 and 38000, which discarded about half of
     * every scan before a trace could even be attempted.
     */
    public static final double DEFAULT_QUANTILE = 0.25;

    private DiaChannelNoise() {
    }

    /**
     * @return the noise level of the given channel, falling back to the sample wide MS2 noise level if the channel
     * does not contain enough peaks to estimate anything from
     */
    public static float estimate(ProcessedSample sample, DiaChannel channel) {
        return estimate(sample, channel, DEFAULT_QUANTILE);
    }

    public static float estimate(ProcessedSample sample, DiaChannel channel, double quantile) {
        return estimate(sample.getStorage().getSpectrumStorage(), channel,
                sample.getStorage().getStatistics().getMs2NoiseLevel(), quantile);
    }

    public static float estimate(SpectrumStorage spectrumStorage, DiaChannel channel, float fallback) {
        return estimate(spectrumStorage, channel, fallback, DEFAULT_QUANTILE);
    }

    public static float estimate(SpectrumStorage spectrumStorage, DiaChannel channel, float fallback, double quantile) {
        final FloatArrayList noisePerScan = new FloatArrayList(channel.numberOfScans());
        final DoubleArrayList intensities = new DoubleArrayList();
        for (int k = 0; k < channel.numberOfScans(); ++k) {
            final SimpleSpectrum spectrum = spectrumStorage.getMs2Spectrum(channel.ms2UidAt(k));
            if (spectrum == null) continue;
            intensities.clear();
            for (int p = 0; p < spectrum.size(); ++p) {
                // zero and negative intensities occur in the wild and would drag the estimate to zero
                final double intensity = spectrum.getIntensityAt(p);
                if (intensity > 0) intensities.add(intensity);
            }
            if (intensities.size() < MIN_PEAKS_PER_SCAN) continue;
            final double[] xs = intensities.toDoubleArray();
            final int rank = Math.min(xs.length - 1, (int) (quantile * xs.length));
            final double floor = Quickselect.quickselectInplace(xs, 0, xs.length, rank);
            double minimum = Double.POSITIVE_INFINITY;
            for (double x : xs) minimum = Math.min(minimum, x);
            final double noise = (2 * floor + minimum) / 3d;
            if (noise > 0) noisePerScan.add((float) noise);
        }
        if (noisePerScan.isEmpty()) return fallback;
        final double average = Statistics.robustAverage(noisePerScan.toFloatArray());
        return Double.isFinite(average) && average > 0 ? (float) average : fallback;
    }
}
