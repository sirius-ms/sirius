package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The intensity a precursor needs before anything of it is visible in a channel, learned from the precursors whose
 * own intact ion survived into it.
 * <p>
 * A precursor that was fragmented at all sends some of its ions through the channel unfragmented, and that ion is
 * traced like any other. So the precursors whose intact ion is present are a set for which the channel demonstrably
 * had enough signal to see something, and the MS1 intensities of that set say where the instrument stops seeing
 * anything - a positive control, needing no decoy, in the same way isotope pairs are one for correlation.
 */
public class DiaPrecursorSurvivalTest {

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

    private static final ScanPointMapping MS1 = axis(MS1_STEP, 4000);
    private static final ScanPointMapping CHANNEL_AXIS = axis(CHANNEL_STEP, 350);

    private static DiaChannel channel() {
        final int[] toMs1 = new int[CHANNEL_AXIS.length()];
        for (int k = 0; k < toMs1.length; ++k) {
            toMs1[k] = (int) Math.round(CHANNEL_AXIS.getRetentionTimeAt(k) / MS1_STEP);
        }
        return new DiaChannel(0, new float[]{100f}, new float[]{500f}, new CollisionEnergy(35), CHANNEL_AXIS, toMs1);
    }

    private static ContiguousTrace ms1Peak(double mz, double centreRt, double sigma, float apex) {
        final double[] masses = new double[MS1.length()];
        final float[] intensities = new float[MS1.length()];
        for (int k = 0; k < MS1.length(); ++k) {
            masses[k] = mz;
            intensities[k] = (float) (apex
                    * Math.exp(-0.5 * Math.pow((MS1.getRetentionTimeAt(k) - centreRt) / sigma, 2)));
        }
        return new ContiguousTrace(MS1, 0, MS1.length() - 1, masses, intensities);
    }

    private static ContiguousTrace channelPeak(double mz, double centreRt, double sigma, float apex) {
        final int from = CHANNEL_AXIS.idForRetentionTime(centreRt - 3 * sigma);
        final int to = CHANNEL_AXIS.idForRetentionTime(centreRt + 3 * sigma);
        final double[] masses = new double[to - from + 1];
        final float[] intensities = new float[masses.length];
        for (int k = from; k <= to; ++k) {
            masses[k - from] = mz;
            intensities[k - from] = (float) (apex
                    * Math.exp(-0.5 * Math.pow((CHANNEL_AXIS.getRetentionTimeAt(k) - centreRt) / sigma, 2)));
        }
        return new ContiguousTrace(CHANNEL_AXIS, from, to, masses, intensities);
    }

    private static PrecursorFeature feature(ContiguousTrace trace, double centreRt) {
        final TraceSegment segment = new TraceSegment((int) Math.round(centreRt / MS1_STEP),
                (int) Math.round((centreRt - 10) / MS1_STEP), (int) Math.round((centreRt + 10) / MS1_STEP));
        final Optional<PrecursorFeature> f = PrecursorFeature.of(channel(), trace, segment);
        assertTrue(f.isPresent(), "the fixture must produce a usable precursor feature");
        return f.get();
    }

    /**
     * Ten compounds of rising intensity, of which only those above a hundred thousand keep an intact ion. The floor
     * has to land in that region rather than at the weakest precursor present, which is the whole point: the weak
     * ones are what it is supposed to exclude.
     */
    @Test
    public void theFloorComesFromThePrecursorsThatSurvived() {
        final List<PrecursorFeature> precursors = new ArrayList<>();
        final List<ContiguousTrace> fragments = new ArrayList<>();
        for (int k = 0; k < 10; ++k) {
            final double mz = 200d + 10 * k;
            final double rt = 100d + 60 * k;
            final float apex = (float) Math.pow(10, 3 + 0.5 * k);   // 1e3 ... 10^7.5
            precursors.add(feature(ms1Peak(mz, rt, 4d, apex), rt));
            // a fragment every compound produces, and an intact ion only the strong ones keep
            fragments.add(channelPeak(mz / 2, rt, 4d, apex / 10f));
            if (apex >= 1e5) fragments.add(channelPeak(mz, rt, 4d, apex / 20f));
        }

        final DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(), precursors, fragments);
        final DiaPrecursorSurvival.Result result = DiaPrecursorSurvival.of(graph, new Deviation(20, 0.005),
                0.25, 1d, 5);

        assertEquals(6, result.survived(), "the six precursors at or above 1e5 kept their intact ion");
        assertEquals(10, result.precursors());
        assertTrue(result.floor() >= 1e5, "the floor may not fall below the weakest survivor, got " + result.floor());
        assertTrue(result.floor() <= 1e7, "nor above the strongest one, got " + result.floor());
    }

    /**
     * The intact ion is not as easy to see as the fragments, so requiring it is stricter than requiring a fragment
     * and the floor read off it is too high. The statistics have to say by how much rather than leave it to a guess,
     * which is what the ratio is for: here every intact ion is half its compound's strongest fragment.
     */
    @Test
    public void theRatioSaysHowMuchStricterTheIntactIonIs() {
        final List<PrecursorFeature> precursors = new ArrayList<>();
        final List<ContiguousTrace> fragments = new ArrayList<>();
        for (int k = 0; k < 6; ++k) {
            final double mz = 300d + 10 * k;
            final double rt = 100d + 60 * k;
            final float apex = (float) 1e6;
            precursors.add(feature(ms1Peak(mz, rt, 4d, apex), rt));
            fragments.add(channelPeak(mz / 2, rt, 4d, 20000f));      // the base fragment
            fragments.add(channelPeak(mz, rt, 4d, 10000f));          // the intact ion, half of it
        }

        final DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(), precursors, fragments);
        final DiaPrecursorSurvival.Result result = DiaPrecursorSurvival.of(graph, new Deviation(20, 0.005),
                0.25, 1d, 5);

        assertEquals(6, result.survived());
        assertEquals(0.5, result.medianIntactToStrongestFragment(), 0.05,
                "got " + result.medianIntactToStrongestFragment());
    }

    /**
     * Survivors alone are not enough: a quantile of six observations is not a quantile, and the default minimum has
     * to suppress the floor even though the same fixture yields one when the caller asks for less.
     */
    @Test
    public void aQuantileOfTooFewSurvivorsIsNotRead() {
        final List<PrecursorFeature> precursors = new ArrayList<>();
        final List<ContiguousTrace> fragments = new ArrayList<>();
        for (int k = 0; k < 6; ++k) {
            final double mz = 300d + 10 * k;
            final double rt = 100d + 60 * k;
            precursors.add(feature(ms1Peak(mz, rt, 4d, 1e6f), rt));
            fragments.add(channelPeak(mz, rt, 4d, 10000f));
        }
        final DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(), precursors, fragments);

        assertTrue(DiaPrecursorSurvival.of(graph, new Deviation(20, 0.005)).survived() > 0);
        assertFalse(DiaPrecursorSurvival.of(graph, new Deviation(20, 0.005)).hasFloor(),
                "six survivors cannot carry a quantile");
    }

    /** With nothing to learn from, the floor has to be absent rather than a number invented from no observations. */
    @Test
    public void tooFewSurvivorsMeansNoFloor() {
        final ContiguousTrace trace = ms1Peak(250d, 100d, 4d, 1e6f);
        final DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(), List.of(feature(trace, 100d)),
                List.of(channelPeak(125d, 100d, 4d, 20000f)));

        final DiaPrecursorSurvival.Result result = DiaPrecursorSurvival.of(graph, new Deviation(20, 0.005),
                0.25, 1d);

        assertEquals(0, result.survived());
        assertEquals(0, result.decoySurvived(), "and nothing half a Dalton away either");
        assertFalse(result.hasFloor(), "no survivor, no floor");
        assertTrue(Double.isNaN(result.medianIntactToStrongestFragment()));
    }
}
