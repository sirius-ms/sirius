package de.unijena.bioinf.lcms.deconvolution;

import java.util.Arrays;

/**
 * Turns a trace correlation into a log p-value, calibrated against decoys.
 * <p>
 * A DIA channel samples a chromatographic peak only four to nine times, so a correlation of 0.9 is easy to
 * obtain by accident, and how easy depends on how many points it came from. Rather than picking a threshold,
 * the null distribution is measured: decoy pairs - a fragment together with a precursor it cannot belong to -
 * are correlated the same way the real pairs are, and a real correlation is then reported as the log fraction of
 * decoys that reached at least that value. This mirrors {@code AdductNetwork.PValueStats} and its
 * {@code CorrelationValue}, including the coarse split by the number of data points.
 * <p>
 * Values are natural logs of probabilities, so they are at most zero and more negative means more significant.
 * Zero means "no evidence at all", which is also what an undefined correlation and an uncalibrated instance
 * without decoys return, so a missing null distribution can never make something look significant.
 */
public class DiaCorrelationPValues {

    /**
     * Correlation bins, denser towards one because that is where all the discrimination sits. Taken from
     * {@code AdductNetwork.ProbabilityValue}.
     */
    private static final double[] BINS = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.75, 0.8, 0.85, 0.9, 0.92,
            0.94, 0.96, 0.97, 0.98, 0.99, 0.995};

    /**
     * Correlations from fewer points than this are calibrated separately. Our point counts run from three to
     * roughly ten, so the split sits in the middle of that range rather than at the eight
     * {@code AdductNetwork} uses for the much more finely sampled MS1 traces.
     */
    static final int FEW_POINTS_THRESHOLD = 6;

    private final Histogram fewPoints = new Histogram();
    private final Histogram manyPoints = new Histogram();

    private boolean calibrated = false;

    /**
     * Adds a decoy correlation to the null distribution. Undefined correlations are ignored: they carry no
     * information about how often an accidental correlation reaches a given value.
     * <p>
     * Synchronized, since decoys are typically collected from parallel jobs.
     */
    public synchronized void addDecoy(DiaTraceCorrelation.Result decoy) {
        if (calibrated) throw new IllegalStateException("this null distribution has already been calibrated");
        if (!decoy.isDefined()) return;
        histogramFor(decoy.datapoints()).add(decoy.correlation());
    }

    /**
     * Builds the lookup tables. Must be called before {@link #logPvalue}, and no decoys may be added afterwards.
     */
    public synchronized void calibrate() {
        fewPoints.done();
        manyPoints.done();
        calibrated = true;
    }

    public boolean isCalibrated() {
        return calibrated;
    }

    public int numberOfDecoys() {
        return fewPoints.total() + manyPoints.total();
    }

    /**
     * @return natural log of the fraction of decoys that reached at least this correlation, using the null
     * distribution for this number of data points. At most zero; zero means no evidence.
     */
    public double logPvalue(DiaTraceCorrelation.Result result) {
        if (!calibrated) throw new IllegalStateException("call calibrate() before asking for p-values");
        if (!result.isDefined()) return 0d;
        return histogramFor(result.datapoints()).logPvalue(result.correlation());
    }

    private Histogram histogramFor(int datapoints) {
        return datapoints < FEW_POINTS_THRESHOLD ? fewPoints : manyPoints;
    }

    private static int binFor(double correlation) {
        final int index = Arrays.binarySearch(BINS, correlation);
        return index >= 0 ? index : -(index + 1);
    }

    /**
     * Histogram of decoy correlations plus the log survival function derived from it.
     */
    private static final class Histogram {

        private final int[] counts = new int[BINS.length + 1];

        /** logPvalues[i] = log fraction of decoys in bin i or above */
        private double[] logPvalues;

        void add(double correlation) {
            ++counts[binFor(correlation)];
        }

        int total() {
            int sum = 0;
            for (int c : counts) sum += c;
            return sum;
        }

        void done() {
            final int total = total();
            if (total == 0) {
                // no null distribution: refuse to call anything significant
                logPvalues = new double[counts.length];
                return;
            }
            // A pseudo count of one keeps the log finite and bounds the claim we can make: with n decoys the
            // strongest statement possible is log(1/(n+1)).
            final double base = Math.log(total + 1);
            logPvalues = new double[counts.length];
            int cumulative = 1;
            for (int bin = counts.length - 1; bin >= 0; --bin) {
                // the decoys of the bin itself count towards its own p-value: a correlation is only significant
                // if few decoys reached *at least* that value. Leaving them out would overstate significance.
                cumulative += counts[bin];
                logPvalues[bin] = Math.log(cumulative) - base;
            }
        }

        double logPvalue(double correlation) {
            return logPvalues[binFor(correlation)];
        }
    }
}
