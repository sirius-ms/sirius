package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.lcms.trace.Trace;

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
