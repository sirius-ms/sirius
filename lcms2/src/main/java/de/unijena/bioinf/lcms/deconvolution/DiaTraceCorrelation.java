package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.lcms.trace.Trace;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.util.List;

/**
 * How well two elution profiles agree over one feature of a precursor: a precursor against a fragment, or two
 * fragments of the same channel against each other.
 * <p>
 * Everything is compared over a {@link ChannelWindow}, the scan points of the channel that cover one preliminary
 * feature of the precursor. That window does the work the guards in
 * {@code de.unijena.bioinf.lcms.adducts.Scorer#correlateLargerWithSmaller} do there: restricting to the half
 * maximum of the larger trace, refusing traces of very different duration and requiring the apex of the smaller
 * one inside the larger exist to <em>find</em> a comparable range between two whole traces. Here the
 * segmentation supplies that range, so those guards have no purpose left — in particular the trap they guard
 * against, a short peak padded with zeros to the length of a long flat one so that both look rectangular,
 * cannot arise when the range is the peak itself.
 * <p>
 * What remains are two requirements: the window must hold enough scan points to describe a shape at all, and
 * both profiles must actually have been measured in it. A fragment seen in only one or two scans of the window
 * yields no shape evidence — which is not the same as yielding no edge, see the graph.
 * <p>
 * The precursor is resampled onto the channel axis first, see {@link Ms1TraceResampler}, since the MS1 scans
 * tick about ten times more often than a channel does.
 */
public final class DiaTraceCorrelation {

    /**
     * Fewer points than this cannot describe a peak shape. Note that this belongs on the channel side: three
     * channel points span 4.5 s at the cadence we measured, which is more than twenty MS1 scan points, so a
     * minimum expressed in MS1 points would be an order of magnitude too permissive.
     */
    public static final int MIN_POINTS = 3;

    /**
     * @param correlation Pearson correlation of the two profiles over the window, NaN if there was no shape to
     *                    compare
     * @param datapoints  number of scan points it is based on. The p-value calibration is binned by this, so it
     *                    must be truthful.
     */
    public record Result(double correlation, int datapoints) {

        public static final Result UNDEFINED = new Result(Double.NaN, 0);

        public boolean isDefined() {
            return Double.isFinite(correlation) && datapoints >= MIN_POINTS;
        }
    }

    private DiaTraceCorrelation() {
    }

    /**
     * @param precursor a trace on the MS1 axis of the sample
     * @param fragment  a trace on the axis of {@code channel}
     * @param window    scan points of {@code channel} covering the precursor feature in question
     */
    public static Result precursorWithFragment(Trace precursor, Trace fragment, DiaChannel channel, ChannelWindow window) {
        final double[] precursorProfile = Ms1TraceResampler.resample(precursor, channel.getAxis(), window.from(), window.to());
        return correlateProfiles(precursorProfile, profileOf(fragment, window));
    }

    /**
     * Both traces must live on the axis of the same channel.
     */
    public static Result fragmentWithFragment(Trace left, Trace right, ChannelWindow window) {
        return correlateProfiles(profileOf(left, window), profileOf(right, window));
    }

    /**
     * @param left  elution profile over the window
     * @param right elution profile over the window, aligned with {@code left}
     */
    static Result correlateProfiles(double[] left, double[] right) {
        final int n = Math.min(left.length, right.length);
        if (n < MIN_POINTS) return Result.UNDEFINED;
        // both profiles have to have been measured in the window. A fragment the channel caught once or twice
        // has no shape, however strong it is.
        if (measuredPoints(left) < MIN_POINTS || measuredPoints(right) < MIN_POINTS) return Result.UNDEFINED;

        final double pearson = Statistics.pearson(left, right);
        return Double.isFinite(pearson) ? new Result(pearson, n) : Result.UNDEFINED;
    }

    /** intensities of the trace over the window, zero where the channel measured nothing */
    /** one sample's view of a candidate pair: both traces, that sample's channel, and the feature's window in it */
    public record SampleWindow(Trace precursor, Trace fragment, DiaChannel channel, ChannelWindow window) {
    }

    /**
     * How many points a single sample has to contribute before it is pooled at all.
     * <p>
     * Below two, normalisation would fabricate agreement rather than measure it: a slice is divided by its own mean,
     * so a single point becomes exactly one in both profiles and contributes a perfectly correlated pair whatever
     * was measured. From two points on, what is left after normalisation is whether the two profiles move in the
     * same direction, which is real if weak information. Note that this is lower than {@link #MIN_POINTS}, and
     * legitimately so: a short slice is not fitted on its own here, it contributes a couple of terms to a sum over
     * all samples, so the degeneracy that makes a two point correlation meaningless does not arise.
     */
    public static final int MIN_POINTS_PER_SAMPLE = 2;

    /**
     * The same correlation, over the windows of several samples at once.
     * <p>
     * This is the point of doing the assignment again after alignment. A channel samples a peak four to nine times
     * and no calibration can make five points say more than five points do; three replicates of that measurement
     * hold three times as many, about the same pair, and pooling them is the only way to get past that floor. The
     * result is a single correlation over all of them, with a truthful point count, so the calibration binned by
     * point count applies unchanged and the extra evidence shows up where it should - in a p-value that a five point
     * correlation could not reach.
     * <p>
     * Each sample's profiles are divided by their own mean before being appended, because the samples have different
     * intensity scales and without that the correlation would partly measure which sample is more intense rather
     * than whether the two profiles have the same shape.
     * <p>
     * Samples where the fragment was not measured in the window are left out rather than appended as zeros. That
     * absence is evidence - the precursor eluted there and the fragment did not - but it is evidence about
     * abundance, not about shape, and it belongs where abundance across samples is judged.
     */
    public static Result pooled(List<SampleWindow> perSample) {
        final DoubleArrayList left = new DoubleArrayList(), right = new DoubleArrayList();
        for (SampleWindow sample : perSample) {
            final double[] precursorProfile = Ms1TraceResampler.resample(sample.precursor(),
                    sample.channel().getAxis(), sample.window().from(), sample.window().to());
            final double[] fragmentProfile = profileOf(sample.fragment(), sample.window());
            if (measuredPoints(precursorProfile) < MIN_POINTS_PER_SAMPLE
                    || measuredPoints(fragmentProfile) < MIN_POINTS_PER_SAMPLE) continue;
            if (!normalizeByMean(precursorProfile) || !normalizeByMean(fragmentProfile)) continue;
            final int n = Math.min(precursorProfile.length, fragmentProfile.length);
            for (int k = 0; k < n; ++k) {
                left.add(precursorProfile[k]);
                right.add(fragmentProfile[k]);
            }
        }
        return correlateProfiles(left.toDoubleArray(), right.toDoubleArray());
    }

    /** @return false if there was nothing to normalise, i.e. the profile is empty */
    private static boolean normalizeByMean(double[] profile) {
        double sum = 0d;
        for (double v : profile) sum += v;
        if (!(sum > 0d)) return false;
        final double mean = sum / profile.length;
        for (int k = 0; k < profile.length; ++k) profile[k] /= mean;
        return true;
    }

    private static double[] profileOf(Trace trace, ChannelWindow window) {
        final double[] profile = new double[window.length()];
        for (int k = Math.max(window.from(), trace.startId()), end = Math.min(window.to(), trace.endId()); k <= end; ++k) {
            profile[k - window.from()] = trace.intensity(k);
        }
        return profile;
    }

    private static int measuredPoints(double[] profile) {
        int count = 0;
        for (double v : profile) if (v > 0) ++count;
        return count;
    }
}
