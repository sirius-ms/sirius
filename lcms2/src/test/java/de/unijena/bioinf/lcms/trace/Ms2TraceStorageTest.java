package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Storage level tests for fragment chromatograms. These need no spectra at all - they are about the bookkeeping:
 * that a trace can be found again at every scan point it covers, that channels are kept apart, and that the float
 * precision of the spatial index does not lose traces at long retention times.
 */
public class Ms2TraceStorageTest {

    private static final Deviation DEV = new Deviation(10, 3e-4);

    private LCMSStorageFactory factory;

    @AfterEach
    public void tearDown() {
        if (factory != null) factory.close();
    }

    private Ms2TraceStorage storage() throws IOException {
        factory = LCMSStorage.temporaryStorage(null, false);
        return factory.createNewStorage().getMs2TraceStorage();
    }

    /**
     * @param startRt retention time of the first scan point, in seconds
     */
    private static ScanPointMapping axis(int length, double startRt, double step) {
        final double[] rts = new double[length];
        final int[] scanIds = new int[length];
        for (int k = 0; k < length; ++k) {
            // deliberately not a round number, so that the double cannot be represented exactly as a float
            rts[k] = startRt + k * step + 1e-4;
            scanIds[k] = 1000 + k;
        }
        return new ScanPointMapping(rts, scanIds, null);
    }

    private static ContiguousTrace trace(ScanPointMapping axis, int from, int to, double mz, float apexIntensity) {
        final double[] masses = new double[to - from + 1];
        final float[] intensities = new float[masses.length];
        final int apex = (from + to) / 2;
        for (int k = from; k <= to; ++k) {
            masses[k - from] = mz;
            intensities[k - from] = apexIntensity / (1 + Math.abs(apex - k));
        }
        return new ContiguousTrace(axis, from, to, masses, intensities);
    }

    @Test
    public void storedTraceIsFoundAtEveryScanPointItCovers() throws IOException {
        ScanPointMapping axis = axis(50, 0d, 2.24d);
        Ms2TraceStorage.ChannelTraces channel = storage().forChannel(0, axis);
        ContiguousTrace stored = channel.add(trace(axis, 10, 20, 123.4567, 5000f));

        for (int k = 10; k <= 20; ++k) {
            assertEquals(stored.getUid(), channel.find(k, 123.4567, DEV).orElseThrow(
                    () -> new AssertionError("trace not found at its own scan point")).getUid());
        }
        assertTrue(channel.find(9, 123.4567, DEV).isEmpty());
        assertTrue(channel.find(21, 123.4567, DEV).isEmpty());
    }

    /**
     * A rectangle keeps its retention times as float, and at an hour into the run a float step is 2.4e-4 s, several
     * times the tolerance a rectangle applies. Lookups have to stay reliable there.
     */
    @Test
    public void traceAtLongRetentionTimesIsStillFoundAtItsEdges() throws IOException {
        ScanPointMapping axis = axis(50, 3600d, 2.24d);
        Ms2TraceStorage.ChannelTraces channel = storage().forChannel(0, axis);
        ContiguousTrace stored = channel.add(trace(axis, 10, 20, 123.4567, 5000f));

        // the first and the last scan point are the ones that sit exactly on the rectangle boundary
        assertEquals(stored.getUid(), channel.find(10, 123.4567, DEV).orElseThrow().getUid(),
                "trace must be found at its first scan point");
        assertEquals(stored.getUid(), channel.find(20, 123.4567, DEV).orElseThrow().getUid(),
                "trace must be found at its last scan point");
    }

    @Test
    public void differentMassesAreNotConfused() throws IOException {
        ScanPointMapping axis = axis(50, 0d, 2.24d);
        Ms2TraceStorage.ChannelTraces channel = storage().forChannel(0, axis);
        ContiguousTrace a = channel.add(trace(axis, 10, 20, 123.4567, 5000f));
        ContiguousTrace b = channel.add(trace(axis, 10, 20, 200.1111, 3000f));

        assertNotEquals(a.getUid(), b.getUid());
        assertEquals(a.getUid(), channel.find(15, 123.4567, DEV).orElseThrow().getUid());
        assertEquals(b.getUid(), channel.find(15, 200.1111, DEV).orElseThrow().getUid());
        assertTrue(channel.find(15, 150.0, DEV).isEmpty());
        assertEquals(2, channel.size());
    }

    @Test
    public void channelsAreIndependent() throws IOException {
        Ms2TraceStorage store = storage();
        // two channels with different axes, holding the very same mass over the very same scan points
        ScanPointMapping axisA = axis(50, 0d, 2.24d);
        ScanPointMapping axisB = axis(50, 1.12d, 2.24d);
        Ms2TraceStorage.ChannelTraces a = store.forChannel(0, axisA);
        Ms2TraceStorage.ChannelTraces b = store.forChannel(1, axisB);

        ContiguousTrace inA = a.add(trace(axisA, 10, 20, 123.4567, 5000f));
        ContiguousTrace inB = b.add(trace(axisB, 10, 20, 123.4567, 1000f));

        assertNotEquals(inA.getUid(), inB.getUid());
        assertEquals(inA.getUid(), a.find(15, 123.4567, DEV).orElseThrow().getUid());
        assertEquals(inB.getUid(), b.find(15, 123.4567, DEV).orElseThrow().getUid());
        assertEquals(1, a.size());
        assertEquals(1, b.size());
        assertEquals(2, store.numberOfTraces());
        // each trace is bound to the axis of its own channel
        assertEquals(axisA.getRetentionTimeAt(15), a.get(inA.getUid()).retentionTime(15), 1e-9);
        assertEquals(axisB.getRetentionTimeAt(15), b.get(inB.getUid()).retentionTime(15), 1e-9);
    }

    /**
     * A trace whose intensities are all zero has a NaN average mass and an infinite m/z range, which would poison
     * the spatial index rather than simply not being found.
     */
    @Test
    public void aTraceWithoutFiniteBoundsIsRejected() throws IOException {
        ScanPointMapping axis = axis(50, 0d, 2.24d);
        Ms2TraceStorage.ChannelTraces channel = storage().forChannel(0, axis);
        ContiguousTrace allZero = new ContiguousTrace(axis, 10, 14,
                new double[]{100d, 100d, 100d, 100d, 100d}, new float[]{0f, 0f, 0f, 0f, 0f});
        assertFalse(Double.isFinite(allZero.averagedMz()), "precondition: such a trace has no average mass");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> channel.add(allZero));
        assertTrue(e.getMessage().contains("finite"));
        assertEquals(0, channel.size());
        assertTrue(channel.find(12, 100d, DEV).isEmpty());
    }

    @Test
    public void unknownUidYieldsNull() throws IOException {
        ScanPointMapping axis = axis(10, 0d, 2.24d);
        assertNull(storage().forChannel(0, axis).get(4711));
    }

    @Test
    public void reopeningAChannelViewSeesTheSameTraces() throws IOException {
        ScanPointMapping axis = axis(50, 0d, 2.24d);
        Ms2TraceStorage store = storage();
        ContiguousTrace stored = store.forChannel(3, axis).add(trace(axis, 10, 20, 123.4567, 5000f));

        Ms2TraceStorage.ChannelTraces again = store.forChannel(3, axis);
        assertEquals(1, again.size());
        assertEquals(stored.getUid(), again.find(15, 123.4567, DEV).orElseThrow().getUid());
    }
}
