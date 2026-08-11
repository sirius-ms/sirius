package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Correlating a precursor with a fragment, or two fragments with each other.
 * <p>
 * The guards are the point of these tests. A channel samples a peak four to nine times, so a correlation is
 * cheap to obtain by accident, and the shapes we compare are of very different length and sampling rate. The
 * traps encoded here are the ones {@code adducts.Scorer.correlateLargerWithSmaller} already learned about.
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

    /** a gaussian peak on the given axis, centred at centerRt with the given width */
    private static ContiguousTrace gaussian(ScanPointMapping ax, double centerRt, double sigma, float apex, double mz) {
        int from = -1, to = -1;
        for (int k = 0; k < ax.length(); ++k) {
            double v = apex * Math.exp(-0.5 * Math.pow((ax.getRetentionTimeAt(k) - centerRt) / sigma, 2));
            if (v >= apex / 1000d) {
                if (from < 0) from = k;
                to = k;
            }
        }
        if (from < 0) throw new IllegalStateException("peak does not fit on this axis");
        final double[] masses = new double[to - from + 1];
        final float[] intensities = new float[masses.length];
        for (int k = from; k <= to; ++k) {
            masses[k - from] = mz;
            intensities[k - from] = (float) (apex * Math.exp(-0.5 * Math.pow((ax.getRetentionTimeAt(k) - centerRt) / sigma, 2)));
        }
        return new ContiguousTrace(ax, from, to, masses, intensities);
    }

    /** a long, almost flat trace, as a background mass produces */
    private static ContiguousTrace plateau(ScanPointMapping ax, int from, int to, float intensity, double mz) {
        final double[] masses = new double[to - from + 1];
        final float[] intensities = new float[masses.length];
        for (int k = 0; k < masses.length; ++k) {
            masses[k] = mz;
            intensities[k] = intensity * (1f + 0.01f * (float) Math.sin(k));
        }
        return new ContiguousTrace(ax, from, to, masses, intensities);
    }

    @Test
    public void aFragmentThatCoElutesWithItsPrecursorCorrelatesAlmostPerfectly() {
        DiaChannel channel = channel();
        ContiguousTrace precursor = gaussian(MS1, 60d, 4d, 10000f, 400.2);
        ContiguousTrace fragment = gaussian(CHANNEL_AXIS, 60d, 4d, 2000f, 150.1);

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.precursorWithFragment(precursor, fragment, channel);

        assertTrue(r.isDefined(), "co-eluting traces must be comparable");
        assertTrue(r.correlation() > 0.95, "expected a high correlation but got " + r.correlation());
        assertTrue(r.datapoints() >= 3, "and it must report how many points it is based on");
    }

    @Test
    public void aFragmentElutingElsewhereDoesNotCorrelate() {
        DiaChannel channel = channel();
        ContiguousTrace precursor = gaussian(MS1, 60d, 4d, 10000f, 400.2);
        ContiguousTrace fragment = gaussian(CHANNEL_AXIS, 140d, 4d, 2000f, 150.1);

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.precursorWithFragment(precursor, fragment, channel);

        assertFalse(r.isDefined() && r.correlation() > 0.5,
                "a fragment eluting a minute later must not look correlated, got " + r.correlation());
    }

    /**
     * The trap: a long flat trace against a short peak. Padding the short one with zeros makes it look
     * rectangular too, and two rectangles correlate beautifully while having nothing to do with each other.
     */
    @Test
    public void aBackgroundPlateauIsNotComparedWithAShortPeak() {
        DiaChannel channel = channel();
        ContiguousTrace precursor = gaussian(MS1, 60d, 2d, 10000f, 400.2);
        ContiguousTrace background = plateau(CHANNEL_AXIS, 0, 89, 500f, 150.1);

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.precursorWithFragment(precursor, background, channel);

        assertFalse(r.isDefined(), "traces of wildly different duration must not be correlated at all");
    }

    @Test
    public void apexOfTheSmallerTraceMustLieWithinTheLargerOne() {
        DiaChannel channel = channel();
        ContiguousTrace precursor = gaussian(MS1, 60d, 4d, 10000f, 400.2);
        // same duration, but shifted so that its apex sits outside the precursor's peak
        ContiguousTrace fragment = gaussian(CHANNEL_AXIS, 78d, 4d, 2000f, 150.1);

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.precursorWithFragment(precursor, fragment, channel);

        assertFalse(r.isDefined() && r.correlation() > 0.5,
                "apex outside the other peak must not give a high correlation, got " + r.correlation());
    }

    @Test
    public void tracesThatDoNotOverlapAtAllAreUndefined() {
        DiaChannel channel = channel();
        ContiguousTrace precursor = gaussian(MS1, 20d, 3d, 10000f, 400.2);
        ContiguousTrace fragment = gaussian(CHANNEL_AXIS, 160d, 3d, 2000f, 150.1);

        assertFalse(DiaTraceCorrelation.precursorWithFragment(precursor, fragment, channel).isDefined());
    }

    @Test
    public void twoFragmentsOfTheSameCompoundCorrelate() {
        ContiguousTrace a = gaussian(CHANNEL_AXIS, 60d, 4d, 2000f, 150.1);
        ContiguousTrace b = gaussian(CHANNEL_AXIS, 60d, 4d, 800f, 233.4);

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.fragmentWithFragment(a, b);

        assertTrue(r.isDefined());
        assertTrue(r.correlation() > 0.95, "got " + r.correlation());
    }

    @Test
    public void twoFragmentsOfDifferentCompoundsDoNotCorrelate() {
        ContiguousTrace a = gaussian(CHANNEL_AXIS, 60d, 4d, 2000f, 150.1);
        ContiguousTrace b = gaussian(CHANNEL_AXIS, 100d, 4d, 800f, 233.4);

        DiaTraceCorrelation.Result r = DiaTraceCorrelation.fragmentWithFragment(a, b);

        assertFalse(r.isDefined() && r.correlation() > 0.5, "got " + r.correlation());
    }

    /** The number of points is what the p-value calibration is binned by, so it must be truthful. */
    @Test
    public void theNumberOfPointsReflectsTheComparisonWindow() {
        DiaChannel channel = channel();
        ContiguousTrace narrow = gaussian(MS1, 60d, 2d, 10000f, 400.2);
        ContiguousTrace wide = gaussian(MS1, 60d, 8d, 10000f, 400.2);
        ContiguousTrace fragmentNarrow = gaussian(CHANNEL_AXIS, 60d, 2d, 2000f, 150.1);
        ContiguousTrace fragmentWide = gaussian(CHANNEL_AXIS, 60d, 8d, 2000f, 150.1);

        DiaTraceCorrelation.Result small = DiaTraceCorrelation.precursorWithFragment(narrow, fragmentNarrow, channel);
        DiaTraceCorrelation.Result large = DiaTraceCorrelation.precursorWithFragment(wide, fragmentWide, channel);

        assertTrue(small.isDefined() && large.isDefined());
        assertTrue(large.datapoints() > small.datapoints(),
                "a broader peak gives more points: " + small.datapoints() + " vs " + large.datapoints());
    }

    @Test
    public void aFragmentSeenInTooFewScansIsUndefined() {
        DiaChannel channel = channel();
        ContiguousTrace precursor = gaussian(MS1, 60d, 4d, 10000f, 400.2);
        // only two scan points of the channel
        ContiguousTrace fragment = new ContiguousTrace(CHANNEL_AXIS, 26, 27,
                new double[]{150.1, 150.1}, new float[]{500f, 400f});

        assertFalse(DiaTraceCorrelation.precursorWithFragment(precursor, fragment, channel).isDefined());
    }
}
