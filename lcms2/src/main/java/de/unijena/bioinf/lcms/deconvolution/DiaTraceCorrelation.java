package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.Trace;

/**
 * Correlates the elution profiles behind two nodes of the assignment graph: a precursor with a fragment, or two
 * fragments of the same channel with each other.
 * <p>
 * This repeats the logic of {@code de.unijena.bioinf.lcms.adducts.Scorer#correlateLargerWithSmaller} rather than
 * calling it, because that one works on the persistence model ({@code AbstractTrace} plus {@code TraceRef}) and
 * we are still inside the sample. The guards it encodes are worth repeating exactly, though:
 * <ul>
 *     <li>correlate over the half maximum window of the <b>larger</b> trace, so a broad neighbour cannot drown
 *     out the shape of a narrow peak,</li>
 *     <li>refuse traces of wildly different duration. Padding a short peak with zeros to the length of a long
 *     flat one makes both look rectangular, and two rectangles correlate beautifully while having nothing to do
 *     with each other,</li>
 *     <li>require the apex of the smaller trace to fall inside the window of the larger one.</li>
 * </ul>
 * Everything is compared on the retention time axis of the channel: the precursor is resampled onto it first
 * (see {@link Ms1TraceResampler}). That also makes the duration guard well defined, since after resampling both
 * profiles are counted in the same scan points again.
 */
public final class DiaTraceCorrelation {

    /** fewer points than this cannot describe a peak shape */
    public static final int MIN_POINTS = 3;

    /** if one trace lasts more than this many times longer than the other, they are not comparable */
    private static final int MAX_LENGTH_RATIO = 4;

    /**
     * @param correlation Pearson correlation of the two elution profiles, NaN if they were not comparable
     * @param datapoints  number of scan points the correlation is based on. The p-value calibration is binned by
     *                    this, so it must be truthful.
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
     */
    public static Result precursorWithFragment(Trace precursor, ContiguousTrace fragment, DiaChannel channel) {
        final ScanPointMapping axis = channel.getAxis();
        final int lo = Math.max(0, Math.min(fragment.startId(), indexOf(axis, precursor.retentionTime(precursor.startId()))));
        final int hi = Math.min(axis.length() - 1, Math.max(fragment.endId(), indexOf(axis, precursor.retentionTime(precursor.endId()))));
        if (hi - lo + 1 < MIN_POINTS) return Result.UNDEFINED;

        final double[] precursorProfile = Ms1TraceResampler.resample(precursor, axis, lo, hi);
        final double[] fragmentProfile = profileOf(fragment, lo, hi);
        return correlateProfiles(precursorProfile, fragmentProfile);
    }

    /**
     * Both traces must live on the axis of the same channel.
     */
    public static Result fragmentWithFragment(ContiguousTrace left, ContiguousTrace right) {
        final int lo = Math.min(left.startId(), right.startId());
        final int hi = Math.max(left.endId(), right.endId());
        if (hi - lo + 1 < MIN_POINTS) return Result.UNDEFINED;
        return correlateProfiles(profileOf(left, lo, hi), profileOf(right, lo, hi));
    }

    /**
     * @param left  elution profile, aligned with {@code right}
     * @param right elution profile, aligned with {@code left}
     */
    static Result correlateProfiles(double[] left, double[] right) {
        final double[] larger, smaller;
        if (apexIntensity(left) >= apexIntensity(right)) {
            larger = left;
            smaller = right;
        } else {
            larger = right;
            smaller = left;
        }
        final int largerApex = apexIndex(larger);
        if (larger[largerApex] <= 0 || smaller[apexIndex(smaller)] <= 0) return Result.UNDEFINED;

        // window: half maximum of the larger trace, widened to at least MIN_POINTS
        final double half = larger[largerApex] / 2d;
        int from = largerApex, to = largerApex;
        while (from > 0 && larger[from - 1] >= half) --from;
        while (to < larger.length - 1 && larger[to + 1] >= half) ++to;
        while (to - from + 1 < MIN_POINTS && (from > 0 || to < larger.length - 1)) {
            if (from > 0) --from;
            if (to < larger.length - 1) ++to;
        }
        if (to - from + 1 < MIN_POINTS) return Result.UNDEFINED;

        // the apex of the smaller trace has to be inside that window
        final int smallerApex = apexIndex(smaller);
        if (smallerApex < from || smallerApex > to) return Result.UNDEFINED;

        // each profile has to describe a peak in its own right. A fragment seen in two scans is not a peak, and
        // neither is a precursor so narrow that the channel only catches it twice - which does happen, since a
        // channel fires an order of magnitude less often than the MS1 scans.
        final int largerLength = supportLength(larger);
        final int smallerLength = supportLength(smaller);
        if (largerLength < MIN_POINTS || smallerLength < MIN_POINTS) return Result.UNDEFINED;

        // and the two must not differ too much in duration
        if (largerLength > MAX_LENGTH_RATIO * smallerLength || smallerLength > MAX_LENGTH_RATIO * largerLength) {
            return Result.UNDEFINED;
        }

        final int n = to - from + 1;
        final double[] xs = new double[n], ys = new double[n];
        System.arraycopy(larger, from, xs, 0, n);
        System.arraycopy(smaller, from, ys, 0, n);
        final double pearson = Statistics.pearson(xs, ys);
        return Double.isFinite(pearson) ? new Result(pearson, n) : Result.UNDEFINED;
    }

    /** intensities of the trace over the given range, zero outside of it */
    private static double[] profileOf(Trace trace, int from, int to) {
        final double[] profile = new double[to - from + 1];
        for (int k = Math.max(from, trace.startId()), n = Math.min(to, trace.endId()); k <= n; ++k) {
            profile[k - from] = trace.intensity(k);
        }
        return profile;
    }

    /** number of scan points where the profile is above a tenth of its apex, i.e. how long it really lasts */
    private static int supportLength(double[] profile) {
        final double threshold = apexIntensity(profile) / 10d;
        int count = 0;
        for (double v : profile) if (v > threshold) ++count;
        return Math.max(1, count);
    }

    private static int apexIndex(double[] profile) {
        int apex = 0;
        for (int k = 1; k < profile.length; ++k) if (profile[k] > profile[apex]) apex = k;
        return apex;
    }

    private static double apexIntensity(double[] profile) {
        return profile[apexIndex(profile)];
    }

    private static int indexOf(ScanPointMapping axis, double retentionTime) {
        return axis.idForRetentionTime(retentionTime);
    }
}
