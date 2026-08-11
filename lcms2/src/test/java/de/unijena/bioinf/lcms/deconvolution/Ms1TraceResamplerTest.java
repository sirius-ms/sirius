package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A precursor trace lives on the MS1 axis of its sample, a fragment trace on the much coarser axis of its DIA
 * channel. Before the two can be compared at all, the precursor has to be sampled at the times the channel
 * actually measured.
 */
public class Ms1TraceResamplerTest {

    private static ScanPointMapping axis(double startRt, double step, int length) {
        final double[] rts = new double[length];
        final int[] scanIds = new int[length];
        for (int k = 0; k < length; ++k) {
            rts[k] = startRt + k * step;
            scanIds[k] = k;
        }
        return new ScanPointMapping(rts, scanIds, null);
    }

    /** a trace that rises linearly from 0 to apexIntensity and back, so interpolation has an exact expectation */
    private static ContiguousTrace triangle(ScanPointMapping ms1Axis, int from, int to, double mz, float apexIntensity) {
        final int n = to - from + 1;
        final double[] masses = new double[n];
        final float[] intensities = new float[n];
        final int apex = n / 2;
        for (int k = 0; k < n; ++k) {
            masses[k] = mz;
            intensities[k] = apexIntensity * (1f - Math.abs(apex - k) / (float) apex);
        }
        intensities[apex] = apexIntensity;
        return new ContiguousTrace(ms1Axis, from, to, masses, intensities);
    }

    @Test
    public void samplingAtTheVerySameTimesReproducesTheTrace() {
        // both axes tick at the same times, so no interpolation is involved
        ScanPointMapping ms1 = axis(0d, 1d, 40);
        ContiguousTrace trace = triangle(ms1, 10, 20, 300.5, 1000f);

        double[] resampled = Ms1TraceResampler.resample(trace, ms1, 10, 20);

        assertEquals(11, resampled.length);
        for (int k = 10; k <= 20; ++k) {
            assertEquals(trace.intensity(k), resampled[k - 10], 1e-9);
        }
    }

    @Test
    public void samplingBetweenTwoScanPointsInterpolatesLinearly() {
        ScanPointMapping ms1 = axis(0d, 1d, 40);
        ContiguousTrace trace = triangle(ms1, 10, 20, 300.5, 1000f);
        // a channel that measures exactly halfway between two MS1 scans
        ScanPointMapping channel = axis(0.5d, 1d, 40);

        double[] resampled = Ms1TraceResampler.resample(trace, channel, 10, 19);

        for (int k = 10; k <= 19; ++k) {
            // in double throughout: the resampler interpolates in double, summing the two floats first would
            // round the expectation and make this compare unequal at the 1e-5 level
            double expected = ((double) trace.intensity(k) + (double) trace.intensity(k + 1)) / 2d;
            assertEquals(expected, resampled[k - 10], 1e-9, "at channel index " + k);
        }
    }

    /**
     * A channel only samples once per cycle, so it generally does not measure at the apex of the precursor at
     * all: its highest point can be up to half a cycle off and correspondingly lower. That is a property of the
     * acquisition, not of the resampling, and it is why nothing downstream may compare apex intensities of a
     * precursor and a fragment directly.
     */
    @Test
    public void aCoarseChannelUndersamplesTheApexOfAFinelySampledTrace() {
        // MS1 every 0.2 s, channel every 2.24 s, as in the real SWATH file
        ScanPointMapping ms1 = axis(0d, 0.2d, 500);
        ContiguousTrace trace = triangle(ms1, 100, 160, 300.5, 1000f);  // rises 20->26 s, falls 26->32 s
        ScanPointMapping channel = axis(0d, 2.24d, 45);

        double[] resampled = Ms1TraceResampler.resample(trace, channel, 0, 44);

        int apexIndex = 0;
        for (int k = 1; k < resampled.length; ++k) if (resampled[k] > resampled[apexIndex]) apexIndex = k;
        final double apexRt = channel.getRetentionTimeAt(apexIndex);
        assertEquals(26d, apexRt, 2.24d, "the resampled apex must be within one cycle of the real one");

        // the triangle at that time, computed analytically: 1000 * (1 - |rt - 26| / 6)
        final double expected = 1000d * (1d - Math.abs(apexRt - 26d) / 6d);
        assertEquals(expected, resampled[apexIndex], 1e-3);
        assertTrue(resampled[apexIndex] < 1000d, "and is necessarily below the true apex");
    }

    @Test
    public void outsideTheTraceEverythingIsZero() {
        ScanPointMapping ms1 = axis(0d, 1d, 40);
        ContiguousTrace trace = triangle(ms1, 10, 20, 300.5, 1000f);
        ScanPointMapping channel = axis(0d, 1d, 40);

        double[] resampled = Ms1TraceResampler.resample(trace, channel, 0, 39);

        for (int k = 0; k < 10; ++k) assertEquals(0d, resampled[k], 1e-12, "before the trace at " + k);
        for (int k = 21; k < 40; ++k) assertEquals(0d, resampled[k], 1e-12, "after the trace at " + k);
        assertTrue(resampled[15] > 0d);
    }

    @Test
    public void resamplingIsAlignedWithTheRequestedRange() {
        ScanPointMapping ms1 = axis(0d, 1d, 40);
        ContiguousTrace trace = triangle(ms1, 10, 20, 300.5, 1000f);
        ScanPointMapping channel = axis(0d, 1d, 40);

        double[] full = Ms1TraceResampler.resample(trace, channel, 0, 39);
        double[] part = Ms1TraceResampler.resample(trace, channel, 12, 18);

        assertEquals(7, part.length);
        for (int k = 12; k <= 18; ++k) assertEquals(full[k], part[k - 12], 1e-12);
    }

    @Test
    public void aTraceOfASingleScanPointIsOnlySeenAtItsOwnTime() {
        ScanPointMapping ms1 = axis(0d, 1d, 40);
        ContiguousTrace trace = new ContiguousTrace(ms1, 15, 15, new double[]{300.5}, new float[]{1000f});
        ScanPointMapping channel = axis(0d, 1d, 40);

        double[] resampled = Ms1TraceResampler.resample(trace, channel, 10, 20);

        assertEquals(1000d, resampled[5], 1e-9, "at its own retention time");
        assertEquals(0d, resampled[4], 1e-12);
        assertEquals(0d, resampled[6], 1e-12);
    }

    @Test
    public void channelAndTraceThatDoNotOverlapInTimeGiveNothing() {
        ScanPointMapping ms1 = axis(0d, 1d, 40);
        ContiguousTrace trace = triangle(ms1, 10, 20, 300.5, 1000f);
        ScanPointMapping elsewhere = axis(1000d, 1d, 10);

        double[] resampled = Ms1TraceResampler.resample(trace, elsewhere, 0, 9);

        for (double v : resampled) assertEquals(0d, v, 1e-12);
    }

    @Test
    public void invalidRangesAreRejected() {
        ScanPointMapping ms1 = axis(0d, 1d, 40);
        ContiguousTrace trace = triangle(ms1, 10, 20, 300.5, 1000f);
        ScanPointMapping channel = axis(0d, 1d, 20);

        assertThrows(IllegalArgumentException.class, () -> Ms1TraceResampler.resample(trace, channel, -1, 5));
        assertThrows(IllegalArgumentException.class, () -> Ms1TraceResampler.resample(trace, channel, 5, 20));
        assertThrows(IllegalArgumentException.class, () -> Ms1TraceResampler.resample(trace, channel, 7, 6));
    }
}
