package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Picks the precursor features that stand in for "this fragment does not belong here".
 * <p>
 * A fragment observed in the scans of one channel cannot come from a precursor outside that channel's isolation
 * windows: those ions were never in the collision cell. Pairing such a precursor with the channel's fragments
 * therefore yields edges that are null by construction, while the co-elution structure that makes a correlation
 * possible in the first place is left untouched. That is what a decoy has to be here - an edge that is wrong for
 * the one reason under test and otherwise indistinguishable from a real one.
 * <p>
 * The decoy features are deliberately handed to the very same {@link DiaAssignmentGraph#build} call as the real
 * ones, so they cannot drift apart from the structural criteria they are meant to calibrate. That also produces
 * decoy values for every signal an edge carries, correlation and specificity alike, in one pass.
 * <p>
 * Two things have to be kept in mind when choosing them:
 * <ul>
 *     <li><b>Isolation windows leak at their edges.</b> A precursor just outside a window can genuinely be
 *     co-isolated and contribute fragments, and neighbouring channels share a boundary. Decoys are therefore
 *     taken at a margin away from the window, or real edges would end up in the null distribution and make it too
 *     strict.</li>
 *     <li><b>The mass distribution has to match.</b> A candidate edge requires the fragment to be no heavier than
 *     its precursor, so decoys drawn from a much lighter part of the m/z range would produce a decoy population
 *     biased towards light fragments. They are taken from a band around the channel's own range instead.</li>
 * </ul>
 */
public final class DiaDecoys {

    /** how far outside the isolation window a decoy precursor has to lie, in m/z */
    public static final double DEFAULT_WINDOW_MARGIN = 5d;

    /**
     * How far from the channel's own m/z range a decoy precursor may be taken, as a multiple of the channel's
     * width. Keeps the decoy population comparable in mass to the real one.
     */
    public static final double DEFAULT_MASS_BAND_FACTOR = 2d;

    /** enough decoy precursors for a stable null distribution without spending real time on it */
    public static final int DEFAULT_MAX_DECOY_PRECURSORS = 200;

    private DiaDecoys() {
    }

    public static List<PrecursorFeature> select(DiaChannel channel, Iterable<ContiguousTrace> ms1Traces) {
        return select(channel, ms1Traces, DEFAULT_WINDOW_MARGIN, DEFAULT_MASS_BAND_FACTOR, DEFAULT_MAX_DECOY_PRECURSORS);
    }

    /**
     * @param windowMargin     how far outside the channel's isolation windows a decoy must lie
     * @param massBandFactor   how far beyond the channel's m/z range decoys may be taken, in channel widths
     * @param maxDecoys        upper bound on the number of decoy features returned
     * @return decoy precursor features on this channel's axis, spread across the retention time range
     */
    public static List<PrecursorFeature> select(DiaChannel channel, Iterable<ContiguousTrace> ms1Traces,
                                                double windowMargin, double massBandFactor, int maxDecoys) {
        final List<PrecursorFeature> candidates = new ArrayList<>();
        for (ContiguousTrace trace : ms1Traces) {
            if (trace.getSegments() == null) continue;
            if (!isEligible(channel, trace.averagedMz(), windowMargin, massBandFactor)) continue;
            for (TraceSegment segment : trace.getSegments()) {
                PrecursorFeature.decoy(channel, trace, segment).ifPresent(candidates::add);
            }
        }
        return thin(candidates, maxDecoys);
    }

    /**
     * @return true if a precursor of this m/z is a safe decoy for the channel: far enough outside every isolation
     * window not to have leaked in, and close enough in mass for the decoy population to be comparable
     */
    public static boolean isEligible(DiaChannel channel, double mz, double windowMargin, double massBandFactor) {
        final double lowest = channel.lowestMz(), highest = channel.highestMz();
        final double band = (highest - lowest) * massBandFactor;
        if (mz < lowest - band || mz > highest + band) return false;
        return isOutsideEveryWindow(channel, mz, windowMargin);
    }

    public static boolean isEligible(DiaChannel channel, double mz) {
        return isEligible(channel, mz, DEFAULT_WINDOW_MARGIN, DEFAULT_MASS_BAND_FACTOR);
    }

    /**
     * Thins a list of candidates down to at most {@code maxDecoys}, evenly along retention time, so the decoys
     * span the run the way the real features do. Taking every n-th of a sorted list keeps this deterministic,
     * which random sampling would not: the null distribution has to come out the same on a rerun of the same file.
     */
    public static List<PrecursorFeature> thin(List<PrecursorFeature> candidates, int maxDecoys) {
        if (candidates.size() <= maxDecoys) return candidates;
        candidates.sort(Comparator.comparingDouble(PrecursorFeature::getRetentionTime));
        final List<PrecursorFeature> thinned = new ArrayList<>(maxDecoys);
        final double stride = candidates.size() / (double) maxDecoys;
        for (int k = 0; k < maxDecoys; ++k) {
            thinned.add(candidates.get(Math.min(candidates.size() - 1, (int) (k * stride))));
        }
        return thinned;
    }

    /**
     * @return true if the m/z is outside every isolation window of the channel by at least the margin
     */
    private static boolean isOutsideEveryWindow(DiaChannel channel, double mz, double margin) {
        for (int w = 0; w < channel.numberOfWindows(); ++w) {
            if (mz >= channel.windowLowerBound(w) - margin && mz <= channel.windowUpperBound(w) + margin) return false;
        }
        return true;
    }
}
