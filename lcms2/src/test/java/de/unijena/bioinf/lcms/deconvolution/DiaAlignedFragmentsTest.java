package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.RecalibrationFunction;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Recognising the same fragment in several samples, which is what lets three coarse measurements of one channel be
 * pooled into one decision.
 */
public class DiaAlignedFragmentsTest {

    private static final double CHANNEL_STEP = 2.24;
    private static final int CHANNEL_LENGTH = 100;
    /** the alignment's own estimates on a replicate group of this kind */
    private static final Deviation MASS_DEVIATION = new Deviation(10);
    private static final double RT_TOLERANCE = 5d;

    private static ScanPointMapping axis(double step, int length) {
        final double[] rts = new double[length];
        final int[] ids = new int[length];
        for (int k = 0; k < length; ++k) {
            rts[k] = k * step;
            ids[k] = k;
        }
        return new ScanPointMapping(rts, ids, null);
    }

    private static final ScanPointMapping CHANNEL_AXIS = axis(CHANNEL_STEP, CHANNEL_LENGTH);

    private static DiaChannel channel() {
        final int[] toMs1 = new int[CHANNEL_LENGTH];
        for (int k = 0; k < CHANNEL_LENGTH; ++k) toMs1[k] = k * 11;
        return new DiaChannel(0, new float[]{350f}, new float[]{450f}, new CollisionEnergy(35), CHANNEL_AXIS, toMs1);
    }

    /** a sample whose retention times are shifted by the given number of seconds against the merged axis */
    private static ProcessedSample sample(int uid, double shift) {
        final ProcessedSample sample = new ProcessedSample(CHANNEL_AXIS, null, 1, uid);
        sample.setRtRecalibration(RecalibrationFunction.linear(new PolynomialFunction(new double[]{-shift, 1d})));
        return sample;
    }

    /** a peak of the given mass around the given channel scan point */
    private static ContiguousTrace trace(double mz, int apex) {
        final int from = Math.max(0, apex - 3), to = Math.min(CHANNEL_LENGTH - 1, apex + 3);
        final double[] masses = new double[to - from + 1];
        final float[] intensities = new float[to - from + 1];
        for (int k = from; k <= to; ++k) {
            masses[k - from] = mz;
            intensities[k - from] = (float) (1000d - 200d * Math.abs(k - apex));
        }
        return new ContiguousTrace(CHANNEL_AXIS, from, to, masses, intensities);
    }

    private static List<DiaAlignedFragments.SampleFragments> input(List<ProcessedSample> samples,
                                                                  List<List<ContiguousTrace>> traces) {
        final List<DiaAlignedFragments.SampleFragments> input = new ArrayList<>();
        for (int k = 0; k < samples.size(); ++k) {
            input.add(new DiaAlignedFragments.SampleFragments(samples.get(k), channel(), traces.get(k)));
        }
        return input;
    }

    @Test
    public void theSameFragmentInThreeSamplesIsOneFragment() {
        final List<ProcessedSample> samples = List.of(sample(0, 0), sample(1, 0), sample(2, 0));
        final List<DiaAlignedFragments.Aligned> aligned = DiaAlignedFragments.group(
                input(samples, List.of(List.of(trace(200.1234, 20)), List.of(trace(200.1240, 20)),
                        List.of(trace(200.1230, 21)))), MASS_DEVIATION, RT_TOLERANCE);

        assertEquals(1, aligned.size(), "one fragment, measured three times");
        assertEquals(3, aligned.get(0).numberOfSamples());
        assertEquals(200.1234, aligned.get(0).getMz(), 1e-3);
        for (int sample = 0; sample < 3; ++sample) {
            assertNotNull(aligned.get(0).traceIn(sample), "and the trace of every sample is reachable");
        }
    }

    @Test
    public void differentMassesStayApart() {
        final List<ProcessedSample> samples = List.of(sample(0, 0), sample(1, 0));
        // 200.1234 and 200.2234 are 500 ppm apart, far beyond what alignment expects between samples
        final List<DiaAlignedFragments.Aligned> aligned = DiaAlignedFragments.group(
                input(samples, List.of(List.of(trace(200.1234, 20)), List.of(trace(200.2234, 20)))),
                MASS_DEVIATION, RT_TOLERANCE);

        assertEquals(2, aligned.size());
        assertEquals(1, aligned.get(0).numberOfSamples());
        assertEquals(1, aligned.get(1).numberOfSamples());
    }

    @Test
    public void theSameMassAtADifferentTimeIsADifferentFragment() {
        final List<ProcessedSample> samples = List.of(sample(0, 0), sample(1, 0));
        // scan point 20 is 44.8 s, scan point 60 is 134.4 s
        final List<DiaAlignedFragments.Aligned> aligned = DiaAlignedFragments.group(
                input(samples, List.of(List.of(trace(200.1234, 20)), List.of(trace(200.1234, 60)))),
                MASS_DEVIATION, RT_TOLERANCE);

        assertEquals(2, aligned.size(), "the same mass eluting twice is two fragments");
    }

    @Test
    public void retentionTimesAreRecalibratedBeforeTheyAreCompared() {
        // the second sample runs ten seconds late, which recalibration is there to undo. Without it the two
        // apexes are 11.2 s apart and would never be grouped.
        final List<ProcessedSample> samples = List.of(sample(0, 0), sample(1, 10d));
        final List<DiaAlignedFragments.Aligned> aligned = DiaAlignedFragments.group(
                input(samples, List.of(List.of(trace(200.1234, 20)), List.of(trace(200.1234, 24)))),
                MASS_DEVIATION, RT_TOLERANCE);

        assertEquals(1, aligned.size(), "recalibrated, both peak at about the same time");
        assertEquals(2, aligned.get(0).numberOfSamples());
    }

    @Test
    public void oneSampleNeverAppearsTwiceInAFragment() {
        // two elutions of the same mass in one sample, close enough in time to be within the tolerance
        final List<ProcessedSample> samples = List.of(sample(0, 0));
        final List<DiaAlignedFragments.Aligned> aligned = DiaAlignedFragments.group(
                input(samples, List.of(List.of(trace(200.1234, 20), trace(200.1234, 22)))),
                MASS_DEVIATION, RT_TOLERANCE);

        assertEquals(2, aligned.size(), "two elutions of one sample are two fragments, not one merged peak");
        assertEquals(1, aligned.get(0).numberOfSamples());
        assertEquals(1, aligned.get(1).numberOfSamples());
    }

    @Test
    public void aFragmentSeenOnlyOnceIsKept() {
        final List<ProcessedSample> samples = List.of(sample(0, 0), sample(1, 0), sample(2, 0));
        final List<DiaAlignedFragments.Aligned> aligned = DiaAlignedFragments.group(
                input(samples, List.of(List.of(trace(200.1234, 20)), List.of(), List.of())),
                MASS_DEVIATION, RT_TOLERANCE);

        assertEquals(1, aligned.size(), "less evidence is not no evidence");
        assertEquals(1, aligned.get(0).numberOfSamples());
        assertNull(aligned.get(0).traceIn(1));
    }

    @Test
    public void nothingToGroupIsNoProblem() {
        assertTrue(DiaAlignedFragments.group(List.of(), MASS_DEVIATION, RT_TOLERANCE).isEmpty());
        assertTrue(DiaAlignedFragments.group(input(List.of(sample(0, 0)), List.of(List.of())),
                MASS_DEVIATION, RT_TOLERANCE).isEmpty());
    }
}
