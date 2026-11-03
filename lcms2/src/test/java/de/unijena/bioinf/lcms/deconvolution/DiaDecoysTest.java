package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Choosing the precursors that stand in for "this fragment does not belong here".
 */
public class DiaDecoysTest {

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

    /** a channel isolating 350 to 450 */
    private static DiaChannel channel() {
        final int[] toMs1 = new int[CHANNEL_AXIS.length()];
        for (int k = 0; k < toMs1.length; ++k) toMs1[k] = (int) Math.round(CHANNEL_AXIS.getRetentionTimeAt(k) / MS1_STEP);
        return new DiaChannel(0, new float[]{350f}, new float[]{450f}, new CollisionEnergy(35), CHANNEL_AXIS, toMs1);
    }

    /** a multiplexed channel isolating 350-450 and 750-850 */
    private static DiaChannel multiplexedChannel() {
        final int[] toMs1 = new int[CHANNEL_AXIS.length()];
        for (int k = 0; k < toMs1.length; ++k) toMs1[k] = (int) Math.round(CHANNEL_AXIS.getRetentionTimeAt(k) / MS1_STEP);
        return new DiaChannel(1, new float[]{350f, 750f}, new float[]{450f, 850f}, new CollisionEnergy(35),
                CHANNEL_AXIS, toMs1);
    }

    private static ContiguousTrace trace(double mz, double centreRt) {
        final double[] masses = new double[MS1.length()];
        final float[] intensities = new float[MS1.length()];
        for (int k = 0; k < MS1.length(); ++k) {
            masses[k] = mz;
            intensities[k] = (float) (10000d * Math.exp(-0.5 * Math.pow((MS1.getRetentionTimeAt(k) - centreRt) / 4d, 2)));
        }
        ContiguousTrace t = new ContiguousTrace(MS1, 0, MS1.length() - 1, masses, intensities);
        t.setSegments(new TraceSegment[]{new TraceSegment(
                (int) Math.round(centreRt / MS1_STEP),
                (int) Math.round((centreRt - 10) / MS1_STEP),
                (int) Math.round((centreRt + 10) / MS1_STEP))});
        return t;
    }

    @Test
    public void precursorsInsideTheWindowAreNeverDecoys() {
        List<ContiguousTrace> traces = List.of(trace(400.2, 60d), trace(360.5, 60d), trace(300.1, 60d));

        List<PrecursorFeature> decoys = DiaDecoys.select(channel(), traces);

        assertEquals(1, decoys.size());
        assertEquals(300.1, decoys.getFirst().getMz(), 1e-6, "only the one outside the window");
    }

    /**
     * Isolation is not a perfect boundary, so a precursor sitting just outside the window may genuinely be
     * co-isolated. Using it as a decoy would put real edges into the null distribution.
     */
    @Test
    public void precursorsJustOutsideTheWindowAreAvoided() {
        // 348 is outside 350-450 but only by two, well within the default margin of five
        List<PrecursorFeature> decoys = DiaDecoys.select(channel(), List.of(trace(348d, 60d)));
        assertTrue(decoys.isEmpty(), "a precursor at the window edge is not a safe decoy");

        // 340 clears the margin
        assertEquals(1, DiaDecoys.select(channel(), List.of(trace(340d, 60d))).size());
    }

    /** Every window of a multiplexed channel has to be respected, not just the first. */
    @Test
    public void everyWindowOfAMultiplexedChannelIsExcluded() {
        List<ContiguousTrace> traces = List.of(trace(400.2, 60d), trace(800.5, 60d), trace(600.3, 60d));

        List<PrecursorFeature> decoys = DiaDecoys.select(multiplexedChannel(), traces);

        assertEquals(1, decoys.size());
        assertEquals(600.3, decoys.getFirst().getMz(), 1e-6, "the mass between the two windows");
    }

    /**
     * Candidate edges require the fragment to be no heavier than its precursor, so decoys taken from a far
     * lighter part of the m/z range would bias the decoy population towards light fragments.
     */
    @Test
    public void decoysAreTakenFromAMassBandAroundTheChannel() {
        // the channel is 100 wide, so the default band reaches 200 either side: 150 to 650
        List<PrecursorFeature> decoys = DiaDecoys.select(channel(),
                List.of(trace(120d, 60d), trace(300d, 60d), trace(500d, 60d), trace(900d, 60d)));

        double[] masses = decoys.stream().mapToDouble(PrecursorFeature::getMz).sorted().toArray();
        assertArrayEquals(new double[]{300d, 500d}, masses, 1e-6);
    }

    @Test
    public void decoysAreCappedAndSpreadAcrossTheRun() {
        final List<ContiguousTrace> traces = new ArrayList<>();
        for (int k = 0; k < 300; ++k) traces.add(trace(300d, 20d + k * 0.5));

        List<PrecursorFeature> decoys = DiaDecoys.select(channel(), traces, 5d, 2d, 20);

        assertEquals(20, decoys.size());
        double[] rts = decoys.stream().mapToDouble(PrecursorFeature::getRetentionTime).toArray();
        // they must cover the range rather than clustering at its start
        assertTrue(rts[0] < 30d, "starts early, at " + rts[0]);
        assertTrue(rts[rts.length - 1] > 140d, "reaches the end, at " + rts[rts.length - 1]);
        for (int k = 1; k < rts.length; ++k) assertTrue(rts[k] >= rts[k - 1], "and is ordered by retention time");
    }

    @Test
    public void selectionIsDeterministic() {
        final List<ContiguousTrace> traces = new ArrayList<>();
        for (int k = 0; k < 300; ++k) traces.add(trace(300d, 20d + k * 0.5));

        double[] first = DiaDecoys.select(channel(), traces, 5d, 2d, 20).stream()
                .mapToDouble(PrecursorFeature::getRetentionTime).toArray();
        double[] second = DiaDecoys.select(channel(), traces, 5d, 2d, 20).stream()
                .mapToDouble(PrecursorFeature::getRetentionTime).toArray();

        assertArrayEquals(first, second, 0d, "no randomness, so a rerun gives the same null distribution");
    }

    @Test
    public void tracesWithoutSegmentationAreSkipped() {
        final double[] masses = new double[MS1.length()];
        final float[] intensities = new float[MS1.length()];
        for (int k = 0; k < MS1.length(); ++k) {
            masses[k] = 300d;
            intensities[k] = 1000f;
        }
        ContiguousTrace unsegmented = new ContiguousTrace(MS1, 0, MS1.length() - 1, masses, intensities);
        assertNull(unsegmented.getSegments(), "precondition");

        assertTrue(DiaDecoys.select(channel(), List.of(unsegmented)).isEmpty());
    }

    /**
     * A decoy feature has to be usable by the very same graph builder as a real one, on the channel's axis.
     */
    @Test
    public void decoyFeaturesSitOnTheChannelAxis() {
        PrecursorFeature decoy = DiaDecoys.select(channel(), List.of(trace(300d, 60d))).getFirst();

        assertEquals(channel().getChannelId(), decoy.getChannelId());
        assertTrue(decoy.getWindow().length() >= DiaTraceCorrelation.MIN_POINTS);
        assertEquals(60d, CHANNEL_AXIS.getRetentionTimeAt(decoy.getApexChannelIndex()), CHANNEL_STEP);
    }
}
