package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Apportioning one fragment between the precursors that could have produced it, and what happens when a measurement
 * says which of them actually did.
 */
public class FragmentIntensitySharesTest {

    private static final double MS1_STEP = 0.2;
    private static final int CHANNEL_LENGTH = 60;
    private static final ScanPointMapping MS1 = axis(MS1_STEP, 1000);
    private static final ScanPointMapping CHANNEL = axis(2d, CHANNEL_LENGTH);

    private static ScanPointMapping axis(double step, int n) {
        final double[] rts = new double[n];
        final int[] ids = new int[n];
        for (int k = 0; k < n; ++k) { rts[k] = k * step; ids[k] = k; }
        return new ScanPointMapping(rts, ids, null);
    }

    private static DiaChannel channel() {
        final int[] toMs1 = new int[CHANNEL_LENGTH];
        for (int k = 0; k < CHANNEL_LENGTH; ++k)
            toMs1[k] = Math.min(MS1.length() - 1, (int) Math.round(CHANNEL.getRetentionTimeAt(k) / MS1_STEP));
        return new DiaChannel(0, new float[]{350f}, new float[]{450f}, new CollisionEnergy(35), CHANNEL, toMs1);
    }

    private static double gaussian(double rt, double centre, double sigma) {
        return Math.exp(-0.5 * Math.pow((rt - centre) / sigma, 2));
    }

    private static ContiguousTrace ms1Trace(double mz, double centre, double apex) {
        final double[] masses = new double[MS1.length()];
        final float[] intensities = new float[MS1.length()];
        for (int k = 0; k < masses.length; ++k) {
            masses[k] = mz;
            intensities[k] = (float) (apex * gaussian(MS1.getRetentionTimeAt(k), centre, 3d));
        }
        return new ContiguousTrace(MS1, 0, MS1.length() - 1, masses, intensities);
    }

    private static ContiguousTrace fragmentTrace(double mz, double centre, double apex) {
        final double[] masses = new double[CHANNEL_LENGTH];
        final float[] intensities = new float[CHANNEL_LENGTH];
        for (int k = 0; k < CHANNEL_LENGTH; ++k) {
            masses[k] = mz;
            intensities[k] = (float) (apex * gaussian(CHANNEL.getRetentionTimeAt(k), centre, 3d));
        }
        return new ContiguousTrace(CHANNEL, 0, CHANNEL_LENGTH - 1, masses, intensities);
    }

    private static PrecursorFeature precursor(double mz, double centre, double apex) {
        final TraceSegment segment = new TraceSegment((int) Math.round(centre / MS1_STEP),
                (int) Math.round((centre - 10d) / MS1_STEP), (int) Math.round((centre + 10d) / MS1_STEP));
        return PrecursorFeature.of(channel(), ms1Trace(mz, centre, apex), segment).orElseThrow();
    }

    /** two precursors eluting at the same time both get a share of a fragment neither can claim alone */
    @Test
    public void coElutingPrecursorsShareAFragment() {
        final List<PrecursorFeature> candidates = List.of(
                precursor(400.2, 60d, 1_000_000d), precursor(410.3, 60d, 1_000_000d));
        final double[] shares = FragmentIntensityShares.of(channel(), candidates, fragmentTrace(150.1, 60d, 20_000d));
        assertEquals(2, shares.length);
        assertTrue(shares[0] > 0 && shares[1] > 0, "neither measurement can rule the other out");
    }

    @Test
    public void noCandidatesIsNotAnError() {
        assertEquals(0, FragmentIntensityShares.of(channel(), List.of(),
                fragmentTrace(150.1, 60d, 20_000d)).length);
    }
}
