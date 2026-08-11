package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The evidence stage two has that stage one does not: the same pair judged in several samples at once, and the
 * abundances across those samples.
 */
public class DiaConsensusEvidenceTest {

    private static final double MS1_STEP = 0.2;
    private static final double CHANNEL_STEP = 2.24;
    private static final int CHANNEL_LENGTH = 90;
    private static final ScanPointMapping MS1 = axis(MS1_STEP, 1000);
    private static final ScanPointMapping CHANNEL = axis(CHANNEL_STEP, CHANNEL_LENGTH);

    private static ScanPointMapping axis(double step, int length) {
        final double[] rts = new double[length];
        final int[] ids = new int[length];
        for (int k = 0; k < length; ++k) {
            rts[k] = k * step;
            ids[k] = k;
        }
        return new ScanPointMapping(rts, ids, null);
    }

    private static DiaChannel channel() {
        final int[] toMs1 = new int[CHANNEL_LENGTH];
        for (int k = 0; k < CHANNEL_LENGTH; ++k) {
            toMs1[k] = Math.min(MS1.length() - 1, (int) Math.round(CHANNEL.getRetentionTimeAt(k) / MS1_STEP));
        }
        return new DiaChannel(0, new float[]{350f}, new float[]{450f}, new CollisionEnergy(35), CHANNEL, toMs1);
    }

    private static double gaussian(double rt, double centre, double sigma) {
        return Math.exp(-0.5 * Math.pow((rt - centre) / sigma, 2));
    }

    /** an MS1 trace peaking at the given time */
    private static ContiguousTrace precursor(double centre, double apexIntensity) {
        final double[] mz = new double[MS1.length()];
        final float[] intensity = new float[MS1.length()];
        for (int k = 0; k < mz.length; ++k) {
            mz[k] = 400.2;
            intensity[k] = (float) (apexIntensity * gaussian(MS1.getRetentionTimeAt(k), centre, 3d));
        }
        return new ContiguousTrace(MS1, 0, MS1.length() - 1, mz, intensity);
    }

    /** a fragment trace on the channel's own axis, covering the scan points where it is above a thousandth */
    private static ContiguousTrace fragment(double mz, double centre, double apexIntensity, double sigma) {
        int from = -1, to = -1;
        for (int k = 0; k < CHANNEL_LENGTH; ++k) {
            if (gaussian(CHANNEL.getRetentionTimeAt(k), centre, sigma) > 1e-3) {
                if (from < 0) from = k;
                to = k;
            }
        }
        final double[] masses = new double[to - from + 1];
        final float[] intensities = new float[to - from + 1];
        for (int k = from; k <= to; ++k) {
            masses[k - from] = mz;
            intensities[k - from] = (float) (apexIntensity * gaussian(CHANNEL.getRetentionTimeAt(k), centre, sigma));
        }
        return new ContiguousTrace(CHANNEL, from, to, masses, intensities);
    }

    private static ChannelWindow window(ContiguousTrace precursorTrace) {
        return ChannelWindow.of(channel(), precursorTrace, new TraceSegment(
                (int) Math.round(60d / MS1_STEP), (int) Math.round(50d / MS1_STEP),
                (int) Math.round(70d / MS1_STEP))).orElseThrow();
    }

    private static DiaTraceCorrelation.SampleWindow sampleWindow(double fragmentCentre, double scale) {
        final ContiguousTrace precursorTrace = precursor(60d, 1_000_000d * scale);
        return new DiaTraceCorrelation.SampleWindow(precursorTrace,
                fragment(150.1, fragmentCentre, 20_000d * scale, 3d), channel(), window(precursorTrace));
    }

    @Test
    public void poolingThreeSamplesTriplesTheEvidence() {
        final DiaTraceCorrelation.SampleWindow one = sampleWindow(60d, 1d);
        final DiaTraceCorrelation.Result single = DiaTraceCorrelation.pooled(List.of(one));
        final DiaTraceCorrelation.Result three = DiaTraceCorrelation.pooled(List.of(one, one, one));

        assertTrue(single.isDefined() && three.isDefined());
        assertEquals(3 * single.datapoints(), three.datapoints(),
                "three samples contribute three times the points, which is what the calibration is binned by");
        assertEquals(single.correlation(), three.correlation(), 1e-9,
                "and the same profile three times has the same shape agreement");
    }

    @Test
    public void samplesOfDifferentIntensityAreNotConfusedWithShape() {
        // one sample a hundred times more intense than the other. Concatenating without normalising would let the
        // step between the samples dominate the correlation instead of the shapes within them.
        final DiaTraceCorrelation.Result sameScale = DiaTraceCorrelation.pooled(
                List.of(sampleWindow(60d, 1d), sampleWindow(60d, 1d)));
        final DiaTraceCorrelation.Result mixedScale = DiaTraceCorrelation.pooled(
                List.of(sampleWindow(60d, 1d), sampleWindow(60d, 100d)));

        assertEquals(sameScale.correlation(), mixedScale.correlation(), 1e-9);
        assertEquals(sameScale.datapoints(), mixedScale.datapoints());
    }

    @Test
    public void aFragmentThatDoesNotCoEluteCorrelatesWorse() {
        final DiaTraceCorrelation.Result together = DiaTraceCorrelation.pooled(
                List.of(sampleWindow(60d, 1d), sampleWindow(60d, 1d), sampleWindow(60d, 1d)));
        final DiaTraceCorrelation.Result apart = DiaTraceCorrelation.pooled(
                List.of(sampleWindow(75d, 1d), sampleWindow(75d, 1d), sampleWindow(75d, 1d)));

        assertTrue(together.correlation() > 0.9, "a co-eluting fragment: " + together.correlation());
        assertTrue(apart.correlation() < together.correlation() - 0.2, "one eluting later: " + apart.correlation());
    }

    @Test
    public void oneInconsistentSampleCostsTheEdge() {
        // the same fragment co-elutes in two samples and peaks elsewhere in the third. Pooling has to notice: a
        // consistent assignment is what three measurements are for.
        final DiaTraceCorrelation.Result consistent = DiaTraceCorrelation.pooled(
                List.of(sampleWindow(60d, 1d), sampleWindow(60d, 1d), sampleWindow(60d, 1d)));
        final DiaTraceCorrelation.Result inconsistent = DiaTraceCorrelation.pooled(
                List.of(sampleWindow(60d, 1d), sampleWindow(60d, 1d), sampleWindow(75d, 1d)));

        assertTrue(inconsistent.correlation() < consistent.correlation() - 0.05,
                consistent.correlation() + " against " + inconsistent.correlation());
        assertEquals(consistent.datapoints(), inconsistent.datapoints(), "on the same number of points");
    }

    @Test
    public void aSampleThatBarelyMeasuredTheFragmentIsLeftOut() {
        // a fragment caught in a single scan point of the window has no direction to agree or disagree with, and
        // dividing it by its own mean would turn it into a perfect match for anything
        final ContiguousTrace precursorTrace = precursor(60d, 1_000_000d);
        final DiaTraceCorrelation.SampleWindow single = new DiaTraceCorrelation.SampleWindow(precursorTrace,
                fragment(150.1, 60d, 20_000d, 0.3), channel(), window(precursorTrace));
        assertEquals(1, measuredPoints(single), "the fixture has to be a single point to test this");

        final DiaTraceCorrelation.Result pooled = DiaTraceCorrelation.pooled(List.of(sampleWindow(60d, 1d), single));
        assertEquals(DiaTraceCorrelation.pooled(List.of(sampleWindow(60d, 1d))).datapoints(), pooled.datapoints());
    }

    private static int measuredPoints(DiaTraceCorrelation.SampleWindow sample) {
        int count = 0;
        final Trace fragment = sample.fragment();
        for (int k = Math.max(sample.window().from(), fragment.startId()),
             end = Math.min(sample.window().to(), fragment.endId()); k <= end; ++k) {
            if (fragment.intensity(k) > 0) ++count;
        }
        return count;
    }

    @Test
    public void nothingPooledIsUndefinedRatherThanZero() {
        assertFalse(DiaTraceCorrelation.pooled(List.of()).isDefined());
    }

    @Test
    public void abundanceFollowingThePrecursorIsEvidence() {
        final double[] precursorAbundance = {1_000_000d, 500_000d, 250_000d};
        final DiaTraceCorrelation.Result following = DiaAbundanceCorrelation.of(precursorAbundance,
                new double[]{20_000d, 10_000d, 5_000d});
        final DiaTraceCorrelation.Result opposing = DiaAbundanceCorrelation.of(precursorAbundance,
                new double[]{5_000d, 10_000d, 20_000d});

        assertEquals(3, following.datapoints());
        assertTrue(following.correlation() > 0.99, "proportional abundances: " + following.correlation());
        assertTrue(opposing.correlation() < 0, "the opposite pattern: " + opposing.correlation());
    }

    @Test
    public void aFragmentMissingWhereThePrecursorIsStrongSpeaksAgainstIt() {
        // the precursor is at its strongest in the first sample and the fragment was not measured there at all
        final DiaTraceCorrelation.Result result = DiaAbundanceCorrelation.of(
                new double[]{1_000_000d, 100_000d, 100_000d}, new double[]{0d, 10_000d, 10_000d});
        assertTrue(result.correlation() < 0, "absence is a measurement: " + result.correlation());
    }

    @Test
    public void twoSamplesAreNotAPattern() {
        // two points always lie on a line, so a correlation over them would be plus or minus one regardless
        assertFalse(DiaAbundanceCorrelation.of(new double[]{1d, 2d}, new double[]{1d, 2d}).isDefined());
        assertFalse(DiaAbundanceCorrelation.of(new double[]{1d}, new double[]{1d}).isDefined());
    }

    @Test
    public void aFragmentMeasuredNowhereHasNoAbundanceEvidence() {
        assertFalse(DiaAbundanceCorrelation.of(new double[]{1d, 2d, 3d}, new double[]{0d, 0d, 0d}).isDefined());
    }

    /** the graph over aligned nodes, with two samples that both saw the same feature and the same fragment */
    @Test
    public void anEdgeIsBuiltFromEveryContributingSample() {
        final List<DiaAlignedFragments.SampleFragments> perSample = new ArrayList<>();
        final List<DiaAlignedFragments.Instance> fragmentInstances = new ArrayList<>();
        final List<DiaFinalPrecursors.Instance> precursorInstances = new ArrayList<>();
        for (int s = 0; s < 3; ++s) {
            final ProcessedSample sample = new ProcessedSample(MS1, null, 1, s);
            final ContiguousTrace fragmentTrace = fragment(150.1, 60d, 20_000d, 3d);
            perSample.add(new DiaAlignedFragments.SampleFragments(sample, channel(), List.of(fragmentTrace)));
            final ContiguousTrace precursorTrace = precursor(60d, 1_000_000d);
            precursorInstances.add(DiaFinalPrecursors.Instance.of(sample, precursorTrace,
                    new TraceSegment((int) Math.round(60d / MS1_STEP), (int) Math.round(50d / MS1_STEP),
                            (int) Math.round(70d / MS1_STEP)), 400.2, 1_000_000f, new int[]{s + 1}));
        }
        final List<DiaAlignedFragments.Aligned> fragments = DiaAlignedFragments.group(perSample,
                new Deviation(10), 5d);
        assertEquals(1, fragments.size(), "the same fragment in all three samples");
        assertEquals(3, fragments.get(0).numberOfSamples());

        final DiaConsensusGraph graph = DiaConsensusGraph.build(perSample,
                List.of(DiaFinalPrecursors.Aligned.of(400.2, 60d, precursorInstances)), fragments, false);

        assertEquals(1, graph.getEdges().size(), "one feature, one fragment, one decision");
        final DiaConsensusGraph.ConsensusEdge edge = graph.getEdges().get(0);
        assertEquals(3, edge.samples(), "informed by all three samples");
        assertTrue(edge.hasShapeEvidence());
        assertTrue(edge.shape().correlation() > 0.9, "co-eluting: " + edge.shape().correlation());
        assertTrue(edge.specificity() > 0.5, "and mostly inside the window: " + edge.specificity());
        assertEquals(0, graph.getFeaturesWithoutShapeEvidence());
    }

    @Test
    public void aFragmentTooHeavyForThePrecursorIsNoCandidate() {
        final List<DiaAlignedFragments.SampleFragments> perSample = new ArrayList<>();
        final List<DiaFinalPrecursors.Instance> precursorInstances = new ArrayList<>();
        for (int s = 0; s < 3; ++s) {
            final ProcessedSample sample = new ProcessedSample(MS1, null, 1, s);
            // 500 Da cannot be a fragment of a 400 Da precursor
            perSample.add(new DiaAlignedFragments.SampleFragments(sample, channel(),
                    List.of(fragment(500.3, 60d, 20_000d, 3d))));
            precursorInstances.add(DiaFinalPrecursors.Instance.of(sample, precursor(60d, 1_000_000d),
                    new TraceSegment((int) Math.round(60d / MS1_STEP), (int) Math.round(50d / MS1_STEP),
                            (int) Math.round(70d / MS1_STEP)), 400.2, 1_000_000f, new int[]{s + 1}));
        }
        final DiaConsensusGraph graph = DiaConsensusGraph.build(perSample,
                List.of(DiaFinalPrecursors.Aligned.of(400.2, 60d, precursorInstances)),
                DiaAlignedFragments.group(perSample, new Deviation(10), 5d), false);

        assertTrue(graph.getEdges().isEmpty());
        assertEquals(1, graph.getFeaturesWithoutShapeEvidence());
    }
}
