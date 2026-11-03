package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The scan index is the one place where all the vendor-specific messiness of DIA files arrives, so these
 * tests deliberately hammer the things we cannot rely on: the order in which spectra are written, the order
 * in which co-isolated precursors are listed, and the exact floating point representation of window bounds.
 */
public class DiaScanIndexBuilderTest {

    /**
     * A plain SWATH scheme: {@code windowsPerCycle} windows of 100 m/z width, one MS1 in front of every MS2,
     * exactly like the Thermo files we tested against.
     */
    /**
     * As {@link #swath(int, int)} but with an explicit gap between one window's visits, which is the quantity the
     * deconvolvability rule judges: a window revisited slowly cannot carry a peak shape however often it fires.
     */
    private static Fixture swathRevisitedEvery(int windowsPerCycle, int cycles, double gapSeconds) {
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        int uid = 0;
        for (int cycle = 0; cycle < cycles; ++cycle) {
            final double cycleStart = cycle * gapSeconds;
            ms1Rts.add(cycleStart);
            final int parentId = ms1Rts.size() - 1;
            for (int w = 0; w < windowsPerCycle; ++w) {
                headers.add(header(uid++, 200d + w * 100d, 50, 50, 35d, parentId,
                        cycleStart + (w + 1) * gapSeconds / (windowsPerCycle + 1)));
            }
        }
        return new Fixture(headers, new Int2ObjectOpenHashMap<>(), mapping(ms1Rts.toDoubleArray()));
    }

    private static Fixture swath(int windowsPerCycle, int cycles) {
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        double rt = 0d;
        int uid = 0;
        for (int cycle = 0; cycle < cycles; ++cycle) {
            for (int w = 0; w < windowsPerCycle; ++w) {
                // MS1 first, then the MS2 of window w
                ms1Rts.add(rt);
                final int parentId = ms1Rts.size() - 1;
                rt += 0.1;
                headers.add(header(uid++, 200d + w * 100d, 50, 50, 35d, parentId, rt));
                rt += 0.1;
            }
        }
        return new Fixture(headers, new Int2ObjectOpenHashMap<>(), mapping(ms1Rts.toDoubleArray()));
    }

    private static Ms2SpectrumHeader header(int uid, double targetMz, double lowerOffset, double upperOffset,
                                            double collisionEnergy, int parentId, double retentionTime) {
        return new Ms2SpectrumHeader("scan=" + (uid + 1), uid + 1, 1, 2, true,
                new CollisionEnergy(collisionEnergy), IsolationWindow.fromOffsets(lowerOffset, upperOffset),
                parentId, targetMz, targetMz, retentionTime).withUid(uid);
    }

    private static ScanPointMapping mapping(double[] retentionTimes) {
        final int[] scanIds = new int[retentionTimes.length];
        for (int k = 0; k < scanIds.length; ++k) scanIds[k] = k;
        return new ScanPointMapping(retentionTimes, scanIds, null);
    }

    /** generous enough that the existing fixtures' fast cadence always clears the density rule */
    private static final double PEAK_WIDTH = 10d;

    private record Fixture(List<Ms2SpectrumHeader> headers, Int2ObjectMap<float[]> multiplexed, ScanPointMapping ms1) {
        Optional<DiaScanIndex> build() {
            return build(PEAK_WIDTH);
        }

        Optional<DiaScanIndex> build(double peakWidth) {
            return DiaScanIndexBuilder.build(headers, multiplexed, ms1, peakWidth);
        }
    }

    /**
     * A window revisited too slowly to sample a peak is not a channel we can deconvolute.
     * <p>
     * The rule is the only thing separating a DIA acquisition from a data dependent one that happens to re-select
     * the same precursor: both produce a window that recurs, and only one produces a window whose consecutive
     * visits fall inside the same elution. Measured on real files the two sit at about eleven and four points per
     * peak against about one, so the margin is real but not large, and the quantity has to be the revisit gap
     * against the peak width rather than the scan count.
     */
    @Test
    public void aWindowRevisitedTooSlowlyToSampleAPeakIsNotAChannel() {
        // sigma of one second: a peak spans about four, so three points need a gap of no more than 1.33 s
        final Fixture dense = swathRevisitedEvery(3, 40, 1.2);
        final Fixture sparse = swathRevisitedEvery(3, 40, 6d);

        assertEquals(3, dense.build(1d).orElseThrow().numberOfChannels(),
                "revisited well inside a peak, so every window is deconvolutable");
        assertTrue(sparse.build(1d).isEmpty(),
                "revisited every six seconds against a one second sigma: nothing to correlate");
        // and the same sparse acquisition becomes deconvolutable if the peaks are broad enough
        assertEquals(3, sparse.build(6d).orElseThrow().numberOfChannels(),
                "the rule is about the gap relative to the peak, not the gap alone");
    }

    /**
     * A handful of deconvolutable windows in an otherwise data dependent run is not a DIA acquisition.
     * <p>
     * Deconvolution rests on a null distribution built from the decoy edges of the channels it processes, so a run
     * where only a few per cent of the scans qualify is calibrated against almost nothing - the p-values would be
     * unmeasured rather than merely weak. Below {@link DiaScanIndexBuilder#MIN_SCAN_COVERAGE} the honest answer is
     * that this is not a DIA measurement, and every scan belongs to the data dependent path.
     */
    @Test
    public void aFewDeconvolutableWindowsDoNotMakeTheSampleDia() {
        // one window revisited densely, and many that are not: together far below the coverage the gate wants
        final List<Ms2SpectrumHeader> headers = new ArrayList<>(swathRevisitedEvery(1, 12, 1d).headers());
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        for (int k = 0; k < 400; ++k) ms1Rts.add(k * 1d);
        int uid = 10_000;
        for (int k = 0; k < 400; ++k) {
            // a different precursor every scan, as data dependent selection produces
            headers.add(header(uid++, 300d + k * 0.7, 15, 15, 35d, k, k * 1d + 0.3));
        }
        final Fixture mixed = new Fixture(headers, new Int2ObjectOpenHashMap<>(), mapping(ms1Rts.toDoubleArray()));

        assertTrue(mixed.build(1d).isEmpty(),
                "12 of 412 scans is under the coverage gate, so the sample is not treated as DIA");
    }

    @Test
    public void plainSwathYieldsOneChannelPerWindow() {
        DiaScanIndex index = swath(8, 20).build().orElseThrow();
        assertEquals(8, index.numberOfChannels());
        for (DiaChannel channel : index.getChannels()) {
            assertEquals(20, channel.numberOfScans(), "every window fires once per cycle");
            assertEquals(1, channel.numberOfWindows());
        }
    }

    /**
     * Same relative isolation window (+-50), different target m/z. These are different channels, even though
     * the {@link IsolationWindow} objects are indistinguishable.
     */
    @Test
    public void windowsAreDistinguishedByAbsoluteMzNotByRelativeOffsets() {
        DiaScanIndex index = swath(3, 10).build().orElseThrow();
        assertEquals(3, index.numberOfChannels());
        double[] los = index.getChannels().stream().mapToDouble(DiaChannel::lowestMz).sorted().toArray();
        assertArrayEquals(new double[]{150, 250, 350}, los, 1e-3);
    }

    /** We must not depend on the order in which the file happens to list its spectra. */
    @Test
    public void resultIsIndependentOfSpectrumOrderInTheFile() {
        Fixture ordered = swath(4, 12);
        DiaScanIndex fromOrdered = ordered.build().orElseThrow();

        List<Ms2SpectrumHeader> shuffled = new ArrayList<>(ordered.headers());
        Collections.shuffle(shuffled, new Random(42));
        DiaScanIndex fromShuffled = DiaScanIndexBuilder.build(shuffled, ordered.multiplexed(), ordered.ms1(), PEAK_WIDTH).orElseThrow();

        assertEquals(fromOrdered.numberOfChannels(), fromShuffled.numberOfChannels());
        for (int c = 0; c < fromOrdered.numberOfChannels(); ++c) {
            DiaChannel a = fromOrdered.getChannels().get(c), b = fromShuffled.getChannels().get(c);
            assertEquals(a.windowLowerBound(0), b.windowLowerBound(0), 1e-6f);
            assertArrayEquals(a.getAxis().getRetentionTimeArray(), b.getAxis().getRetentionTimeArray(), 1e-9);
            assertArrayEquals(a.getAxis().getScanIdArray(), b.getAxis().getScanIdArray());
            assertArrayEquals(a.getToMs1Index(), b.getToMs1Index());
        }
    }

    /** Retention times are the thing we can trust; the axis must be sorted by them. */
    @Test
    public void axisIsSortedByRetentionTime() {
        DiaChannel channel = swath(2, 25).build().orElseThrow().getChannels().getFirst();
        double[] rts = channel.getAxis().getRetentionTimeArray();
        for (int k = 1; k < rts.length; ++k) {
            assertTrue(rts[k] > rts[k - 1], "retention times must increase along the channel axis");
        }
    }

    @Test
    public void multiplexedSpectraFormOneChannelRegardlessOfPrecursorOrder() {
        // three co-isolated windows, listed descending as Thermo does it
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final Int2ObjectMap<float[]> multiplexed = new Int2ObjectOpenHashMap<>();
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        double rt = 0d;
        for (int cycle = 0; cycle < 10; ++cycle) {
            ms1Rts.add(rt);
            rt += 0.1;
            // the header itself carries whatever precursor happened to be listed first
            headers.add(header(cycle, 1200d, 50, 50, 35d, ms1Rts.size() - 1, rt));
            multiplexed.put(cycle, new float[]{1150, 1250, 1050, 1150, 950, 1050});
            rt += 0.1;
        }
        DiaScanIndex index = DiaScanIndexBuilder.build(headers, multiplexed, mapping(ms1Rts.toDoubleArray()), PEAK_WIDTH).orElseThrow();

        assertEquals(1, index.numberOfChannels());
        DiaChannel channel = index.getChannels().getFirst();
        assertEquals(3, channel.numberOfWindows());
        assertEquals(10, channel.numberOfScans());
        // windows are stored in canonical (ascending) order
        assertEquals(950f, channel.windowLowerBound(0), 1e-3f);
        assertEquals(1050f, channel.windowLowerBound(1), 1e-3f);
        assertEquals(1150f, channel.windowLowerBound(2), 1e-3f);
    }

    @Test
    public void sameWindowWithDifferentCollisionEnergiesAreDifferentChannels() {
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        double rt = 0d;
        int uid = 0;
        for (int cycle = 0; cycle < 10; ++cycle) {
            for (double ce : new double[]{20d, 40d}) {
                ms1Rts.add(rt);
                rt += 0.1;
                headers.add(header(uid++, 400d, 50, 50, ce, ms1Rts.size() - 1, rt));
                rt += 0.1;
            }
        }
        DiaScanIndex index = DiaScanIndexBuilder.build(headers, new Int2ObjectOpenHashMap<>(), mapping(ms1Rts.toDoubleArray()), PEAK_WIDTH).orElseThrow();
        assertEquals(2, index.numberOfChannels());
        for (DiaChannel c : index.getChannels()) assertEquals(10, c.numberOfScans());
    }

    /**
     * Vendors round window bounds differently from scan to scan, and the instrument does not realize the nominal
     * bounds precisely anyway. Differences far below the width of a window must not fragment a channel.
     */
    @Test
    public void negligibleDifferencesInWindowBoundsDoNotFragmentChannels() {
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        double rt = 0d;
        for (int cycle = 0; cycle < 20; ++cycle) {
            ms1Rts.add(rt);
            rt += 0.1;
            // jitter the nominal 400.0 target by up to +-0.02 m/z
            headers.add(header(cycle, 400d + (cycle % 5 - 2) * 0.01, 50, 50, 35d, ms1Rts.size() - 1, rt));
            rt += 0.1;
        }
        DiaScanIndex index = DiaScanIndexBuilder.build(headers, new Int2ObjectOpenHashMap<>(), mapping(ms1Rts.toDoubleArray()), PEAK_WIDTH).orElseThrow();
        assertEquals(1, index.numberOfChannels());
        assertEquals(20, index.getChannels().getFirst().numberOfScans());
        assertEquals(350f, index.getChannels().getFirst().windowLowerBound(0), 1e-3f);
    }

    /** Windows that really are different must still be told apart. */
    @Test
    public void windowsThatDifferMeaningfullyStayApart() {
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        double rt = 0d;
        int uid = 0;
        for (int cycle = 0; cycle < 10; ++cycle) {
            for (double target : new double[]{400d, 400.5d}) {
                ms1Rts.add(rt);
                rt += 0.1;
                headers.add(header(uid++, target, 50, 50, 35d, ms1Rts.size() - 1, rt));
                rt += 0.1;
            }
        }
        DiaScanIndex index = DiaScanIndexBuilder.build(headers, new Int2ObjectOpenHashMap<>(), mapping(ms1Rts.toDoubleArray()), PEAK_WIDTH).orElseThrow();
        assertEquals(2, index.numberOfChannels());
    }

    @Test
    public void channelIndexMapsBackToMs1Scans() {
        Fixture fixture = swath(3, 10);
        DiaScanIndex index = fixture.build().orElseThrow();
        for (DiaChannel channel : index.getChannels()) {
            int[] toMs1 = channel.getToMs1Index();
            assertEquals(channel.numberOfScans(), toMs1.length);
            for (int k = 0; k < toMs1.length; ++k) {
                assertTrue(toMs1[k] >= 0 && toMs1[k] < fixture.ms1().length());
                // the MS1 scan we point at must not be further away than one cycle
                double ms2Rt = channel.getAxis().getRetentionTimeAt(k);
                assertTrue(Math.abs(fixture.ms1().getRetentionTimeAt(toMs1[k]) - ms2Rt) <= 0.5,
                        "channel index " + k + " maps to a far away MS1 scan");
            }
            for (int k = 1; k < toMs1.length; ++k) {
                assertTrue(toMs1[k] > toMs1[k - 1], "MS1 indices must increase along the channel axis");
            }
        }
    }

    @Test
    public void channelsCoveringFindsTheRightChannels() {
        DiaScanIndex index = swath(3, 10).build().orElseThrow(); // 150-250, 250-350, 350-450
        assertEquals(1, index.channelsCovering(200).size());
        assertEquals(150f, index.channelsCovering(200).getFirst().windowLowerBound(0), 1e-3f);
        assertEquals(1, index.channelsCovering(400).size());
        assertTrue(index.channelsCovering(1000).isEmpty(), "nothing covers m/z outside of all windows");
        assertTrue(index.channelsCovering(100).isEmpty());
    }

    @Test
    public void overlappingWindowsBothCoverThePrecursor() {
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        double rt = 0d;
        int uid = 0;
        for (int cycle = 0; cycle < 10; ++cycle) {
            for (double target : new double[]{400d, 450d}) { // 350-450 and 400-500 overlap
                ms1Rts.add(rt);
                rt += 0.1;
                headers.add(header(uid++, target, 50, 50, 35d, ms1Rts.size() - 1, rt));
                rt += 0.1;
            }
        }
        DiaScanIndex index = DiaScanIndexBuilder.build(headers, new Int2ObjectOpenHashMap<>(), mapping(ms1Rts.toDoubleArray()), PEAK_WIDTH).orElseThrow();
        assertEquals(2, index.numberOfChannels());
        assertEquals(2, index.channelsCovering(420).size(), "a precursor in the overlap belongs to both channels");
        assertEquals(1, index.channelsCovering(360).size());
    }

    /** DDA: every MS2 has its own narrow window, so there is no channel structure to find. */
    @Test
    public void ddaLikeAcquisitionIsNotRecognizedAsDia() {
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        double rt = 0d;
        for (int k = 0; k < 200; ++k) {
            ms1Rts.add(rt);
            rt += 0.1;
            headers.add(header(k, 100d + k * 1.37, 0.2, 0.2, 35d, ms1Rts.size() - 1, rt));
            rt += 0.1;
        }
        assertTrue(DiaScanIndexBuilder.build(headers, new Int2ObjectOpenHashMap<>(), mapping(ms1Rts.toDoubleArray()), PEAK_WIDTH).isEmpty());
    }

    @Test
    public void spectraWithoutIsolationWindowAreIgnored() {
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final it.unimi.dsi.fastutil.doubles.DoubleArrayList ms1Rts = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        double rt = 0d;
        for (int k = 0; k < 20; ++k) {
            ms1Rts.add(rt);
            rt += 0.1;
            headers.add(new Ms2SpectrumHeader("scan=" + k, k, 1, 2, true, new CollisionEnergy(35),
                    null, ms1Rts.size() - 1, 400d, 400d, rt).withUid(k));
            rt += 0.1;
        }
        assertTrue(DiaScanIndexBuilder.build(headers, new Int2ObjectOpenHashMap<>(), mapping(ms1Rts.toDoubleArray()), PEAK_WIDTH).isEmpty(),
                "without isolation windows there is nothing we could deconvolute");
    }

    @Test
    public void emptyInputYieldsNoIndex() {
        assertTrue(DiaScanIndexBuilder.build(List.of(), new Int2ObjectOpenHashMap<>(), mapping(new double[]{0, 1, 2}), PEAK_WIDTH).isEmpty());
    }

    /** A window that only fires a handful of times cannot carry a chromatographic peak shape. */
    @Test
    public void channelsWithTooFewScansAreDropped() {
        Fixture fixture = swath(3, 10);
        List<Ms2SpectrumHeader> headers = new ArrayList<>(fixture.headers());
        // add a fourth window that only fires twice
        headers.add(header(1000, 900d, 50, 50, 35d, 0, 0.05));
        headers.add(header(1001, 900d, 50, 50, 35d, 1, 0.15));
        DiaScanIndex index = DiaScanIndexBuilder.build(headers, fixture.multiplexed(), fixture.ms1(), PEAK_WIDTH).orElseThrow();
        assertEquals(3, index.numberOfChannels());
        assertTrue(index.channelsCovering(900).isEmpty());
        assertEquals(2, index.getNumberOfDroppedScans());
    }
}
