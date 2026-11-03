package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The rule of {@link UncontestedAssignmentStrategy}: each precursor and fragment judged alone against fixed bars.
 * <p>
 * What has to hold is not only that a well correlated fragment is kept, but that nothing about the <em>other</em>
 * precursors changes the answer. That is the property distinguishing this strategy from the two that normalise, and
 * it is the one worth a test, because it is what makes a crowded isolation window cost it nothing.
 */
public class UncontestedAssignmentStrategyTest {

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
        for (int k = 0; k < toMs1.length; ++k) {
            toMs1[k] = (int) Math.round(CHANNEL_AXIS.getRetentionTimeAt(k) / MS1_STEP);
        }
        return new DiaChannel(0, new float[]{350f}, new float[]{450f}, new CollisionEnergy(35), CHANNEL_AXIS, toMs1);
    }

    private static ContiguousTrace ms1Peak(double mz, double centre, double sigma, float apex) {
        final double[] masses = new double[MS1.length()];
        final float[] intensities = new float[MS1.length()];
        for (int k = 0; k < MS1.length(); ++k) {
            masses[k] = mz;
            intensities[k] = (float) (apex
                    * Math.exp(-0.5 * Math.pow((MS1.getRetentionTimeAt(k) - centre) / sigma, 2)));
        }
        return new ContiguousTrace(MS1, 0, MS1.length() - 1, masses, intensities);
    }

    /** a fragment measured only around the given time, as a well behaved fragment of one compound would be */
    private static ContiguousTrace fragment(double mz, double centre, double sigma, float apex) {
        final int from = CHANNEL_AXIS.idForRetentionTime(centre - 4 * sigma);
        final int to = CHANNEL_AXIS.idForRetentionTime(centre + 4 * sigma);
        final double[] masses = new double[to - from + 1];
        final float[] intensities = new float[masses.length];
        for (int k = from; k <= to; ++k) {
            masses[k - from] = mz;
            intensities[k - from] = (float) (apex
                    * Math.exp(-0.5 * Math.pow((CHANNEL_AXIS.getRetentionTimeAt(k) - centre) / sigma, 2)));
        }
        return new ContiguousTrace(CHANNEL_AXIS, from, to, masses, intensities);
    }

    private static PrecursorFeature feature(ContiguousTrace trace, double centre) {
        final TraceSegment segment = new TraceSegment((int) Math.round(centre / MS1_STEP),
                (int) Math.round((centre - 15) / MS1_STEP), (int) Math.round((centre + 15) / MS1_STEP));
        return PrecursorFeature.of(channel(), trace, segment).orElseThrow();
    }

    @Test
    public void aCoElutingFragmentIsKeptAndAnUnrelatedOneIsNot() {
        final PrecursorFeature precursor = feature(ms1Peak(400.2, 60d, 6d, 1_000_000f), 60d);
        final ContiguousTrace together = fragment(150.1, 60d, 6d, 20_000f);
        // the same mass range, but rising where the precursor falls
        final ContiguousTrace elsewhere = fragment(170.1, 52d, 3d, 20_000f);

        final DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(), List.of(precursor),
                List.of(together, elsewhere));
        final List<IntArrayList> accepted = UncontestedAssignmentStrategy.select(graph, 0.8, 5, 5_000d);

        assertEquals(1, accepted.size());
        assertTrue(accepted.getFirst().contains(0), "the co-eluting fragment belongs to it");
        assertFalse(accepted.getFirst().contains(1), "the one peaking elsewhere does not");
    }

    /** a fragment below the intensity bar is dropped however well it correlates */
    @Test
    public void aFragmentBelowTheIntensityBarIsDropped() {
        final PrecursorFeature precursor = feature(ms1Peak(400.2, 60d, 6d, 1_000_000f), 60d);
        final ContiguousTrace weak = fragment(150.1, 60d, 6d, 2_000f);

        final DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(), List.of(precursor), List.of(weak));
        assertTrue(UncontestedAssignmentStrategy.select(graph, 0.8, 5, 5_000d).getFirst().isEmpty());
        assertFalse(UncontestedAssignmentStrategy.select(graph, 0.8, 5, 1_000d).getFirst().isEmpty(),
                "and kept once the bar is below it");
    }

    /**
     * The property that distinguishes this strategy: a second precursor that could equally have produced the fragment
     * changes nothing. Both keep it, at its full height, and neither is weakened by the other's claim.
     */
    @Test
    public void aCompetitorDoesNotWeakenAnyonesClaim() {
        final PrecursorFeature strong = feature(ms1Peak(400.2, 60d, 6d, 1_000_000f), 60d);
        final PrecursorFeature weak = feature(ms1Peak(420.3, 60d, 6d, 100_000f), 60d);
        final ContiguousTrace shared = fragment(150.1, 60d, 6d, 20_000f);

        final List<IntArrayList> alone = UncontestedAssignmentStrategy.select(
                DiaAssignmentGraph.build(channel(), List.of(strong), List.of(shared)), 0.8, 5, 5_000d);
        final List<IntArrayList> contested = UncontestedAssignmentStrategy.select(
                DiaAssignmentGraph.build(channel(), List.of(strong, weak), List.of(shared)), 0.8, 5, 5_000d);

        assertEquals(alone.getFirst(), contested.getFirst(), "the first precursor's answer is unchanged");
        assertTrue(contested.get(1).contains(0), "and the second gets the fragment too");
    }
}
