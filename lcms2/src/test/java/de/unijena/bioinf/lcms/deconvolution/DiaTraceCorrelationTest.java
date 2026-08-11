package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Shape agreement between a precursor and a fragment over one feature of the precursor.
 * <p>
 * The interesting cases are the ones a whole-trace correlation gets wrong: a precursor trace carrying two
 * features, and a fragment that belongs to only one of them.
 */
public class DiaTraceCorrelationTest {

    private static final double MS1_STEP = 0.2;
    private static final double CHANNEL_STEP = 2.24;

    private static ScanPointMapping axis(double step, int length) {
        final double[] rts = new double[length];
        final int[] ids = new int[length];
        for (int k = 0; k < length; ++k) {
            rts[k] = k * step;
            ids[k] = k;
        }
        return new ScanPointMapping(rts, ids, null);
    }

    private static final ScanPointMapping MS1 = axis(MS1_STEP, 1000);
    private static final ScanPointMapping CHANNEL_AXIS = axis(CHANNEL_STEP, 90);

    private static DiaChannel channel() {
        final int[] toMs1 = new int[CHANNEL_AXIS.length()];
        for (int k = 0; k < toMs1.length; ++k) toMs1[k] = (int) Math.round(CHANNEL_AXIS.getRetentionTimeAt(k) / MS1_STEP);
        return new DiaChannel(0, new float[]{350f}, new float[]{450f}, new CollisionEnergy(35), CHANNEL_AXIS, toMs1);
    }

    /** sum of gaussians, so a trace can carry more than one feature */
    private static ContiguousTrace peaks(ScanPointMapping ax, double mz, double baseline, double[]... centreSigmaApex) {
        final double[] masses = new double[ax.length()];
        final float[] intensities = new float[ax.length()];
        for (int k = 0; k < ax.length(); ++k) {
            double v = baseline;
            for (double[] p : centreSigmaApex) {
                v += p[2] * Math.exp(-0.5 * Math.pow((ax.getRetentionTimeAt(k) - p[0]) / p[1], 2));
            }
            masses[k] = mz;
            intensities[k] = (float) v;
        }
        return new ContiguousTrace(ax, 0, ax.length() - 1, masses, intensities);
    }

    private static ChannelWindow windowAround(double centreRt, double halfWidthRt) {
        DiaChannel c = channel();
        return new ChannelWindow(c.channelIndexForRetentionTime(centreRt - halfWidthRt),
                c.channelIndexForRetentionTime(centreRt + halfWidthRt));
    }

    @Test
    public void aFragmentThatCoElutesWithTheFeatureCorrelatesAlmostPerfectly() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        ContiguousTrace fragment = peaks(CHANNEL_AXIS, 150.1, 0d, new double[]{60d, 4d, 2000d});

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.precursorWithFragment(
                precursor, fragment, channel(), windowAround(60d, 8d));

        assertTrue(r.isDefined());
        assertTrue(r.correlation() > 0.95, "expected a high correlation, got " + r.correlation());
        assertTrue(r.datapoints() >= DiaTraceCorrelation.MIN_POINTS);
    }

    /**
     * The reason windows exist. One precursor trace, two features; a fragment belonging to the first must
     * correlate there and not at the second. A whole-trace correlation would centre on whichever feature
     * happens to be more intense and answer the wrong question for the other one.
     */
    @Test
    public void thesameFragmentIsJudgedPerFeatureOfThePrecursor() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d,
                new double[]{40d, 4d, 10000d}, new double[]{100d, 4d, 12000d});
        // The fragment belongs to the first feature only. It is present across the whole run as background, and
        // that background drifts - a perfectly constant profile would have no variance and therefore no
        // correlation at all, which would prove nothing about the second window.
        ContiguousTrace fragment = peaks(CHANNEL_AXIS, 150.1, 50d,
                new double[]{40d, 4d, 2000d}, new double[]{130d, 20d, 300d});

        DiaTraceCorrelation.Result atFirst = DiaTraceCorrelation.precursorWithFragment(
                precursor, fragment, channel(), windowAround(40d, 8d));
        DiaTraceCorrelation.Result atSecond = DiaTraceCorrelation.precursorWithFragment(
                precursor, fragment, channel(), windowAround(100d, 8d));

        assertTrue(atFirst.isDefined() && atSecond.isDefined(), "both windows hold enough scan points");
        assertTrue(atFirst.correlation() > 0.95, "at its own feature, got " + atFirst.correlation());
        assertTrue(atFirst.correlation() > atSecond.correlation() + 0.5,
                "the fragment must look far better at its own feature: " + atFirst.correlation()
                        + " vs " + atSecond.correlation());
    }

    @Test
    public void aFragmentThatWasNotMeasuredInTheWindowHasNoShape() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        // a fragment confined to a completely different part of the run
        ContiguousTrace fragment = new ContiguousTrace(CHANNEL_AXIS, 4, 10,
                new double[]{150.1, 150.1, 150.1, 150.1, 150.1, 150.1, 150.1},
                new float[]{10f, 50f, 200f, 400f, 200f, 50f, 10f});

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.precursorWithFragment(
                precursor, fragment, channel(), windowAround(60d, 8d));

        assertFalse(r.isDefined(), "nothing was measured here, so there is no shape to compare");
    }

    /**
     * A fragment the channel caught once or twice carries no shape. That is not the same as carrying no
     * information at all - the graph keeps such an edge and scores it by specificity instead - but this class
     * has nothing to say about it.
     */
    @Test
    public void aFragmentSeenInTooFewScansOfTheWindowHasNoShape() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        final int apex = channel().channelIndexForRetentionTime(60d);
        ContiguousTrace fragment = new ContiguousTrace(CHANNEL_AXIS, apex, apex + 1,
                new double[]{150.1, 150.1}, new float[]{500f, 400f});

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.precursorWithFragment(
                precursor, fragment, channel(), windowAround(60d, 8d));

        assertFalse(r.isDefined());
        assertEquals(0, r.datapoints());
    }

    @Test
    public void twoFragmentsOfTheSameCompoundCorrelate() {
        ContiguousTrace a = peaks(CHANNEL_AXIS, 150.1, 0d, new double[]{60d, 4d, 2000d});
        ContiguousTrace b = peaks(CHANNEL_AXIS, 233.4, 0d, new double[]{60d, 4d, 800d});

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.fragmentWithFragment(a, b, windowAround(60d, 8d));

        assertTrue(r.isDefined());
        assertTrue(r.correlation() > 0.95, "got " + r.correlation());
    }

    @Test
    public void twoFragmentsOfDifferentCompoundsDoNotCorrelate() {
        ContiguousTrace a = peaks(CHANNEL_AXIS, 150.1, 20d, new double[]{50d, 4d, 2000d});
        ContiguousTrace b = peaks(CHANNEL_AXIS, 233.4, 20d, new double[]{70d, 4d, 800d});

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.fragmentWithFragment(a, b, windowAround(60d, 8d));

        assertTrue(r.isDefined(), "both were measured across this window");
        assertTrue(r.correlation() < 0.5, "two peaks either side of the window must not agree, got " + r.correlation());
    }

    @Test
    public void theNumberOfPointsIsTheSizeOfTheWindow() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 8d, 10000d});
        ContiguousTrace fragment = peaks(CHANNEL_AXIS, 150.1, 10d, new double[]{60d, 8d, 2000d});

        ChannelWindow narrow = windowAround(60d, 6d);
        ChannelWindow wide = windowAround(60d, 14d);

        assertEquals(narrow.length(), DiaTraceCorrelation.precursorWithFragment(precursor, fragment, channel(), narrow).datapoints());
        assertEquals(wide.length(), DiaTraceCorrelation.precursorWithFragment(precursor, fragment, channel(), wide).datapoints());
    }

    @Test
    public void aWindowTooSmallToHoldAShapeIsUndefined() {
        assertFalse(DiaTraceCorrelation.correlateProfiles(new double[]{1, 2}, new double[]{1, 2}).isDefined());
    }

    /**
     * Both profiles constant means no variance and therefore no correlation. Note that a <em>nearly</em>
     * constant fragment can still produce a large correlation by chance over four or five points - that is what
     * the decoy calibration in {@link DiaCorrelationPValues} is for, and no longer something this class tries to
     * catch with a guard.
     */
    @Test
    public void constantProfilesHaveNoCorrelation() {
        assertFalse(DiaTraceCorrelation.correlateProfiles(
                new double[]{5, 5, 5, 5, 5}, new double[]{1, 2, 3, 2, 1}).isDefined());
    }
}
