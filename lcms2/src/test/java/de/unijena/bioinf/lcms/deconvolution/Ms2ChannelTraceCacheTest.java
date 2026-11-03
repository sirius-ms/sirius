package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.spectrum.Ms1SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.Ms2TraceStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Storing fragment traces per channel. The point of these tests is that a trace never leaves the retention time
 * axis of its own channel, not even after a round trip through the store, and that two channels cannot be
 * confused with each other.
 */
public class Ms2ChannelTraceCacheTest {

    private static final double STEP = 0.25;
    private static final int WINDOWS = 3;      // 200, 300, 400 (+-50)
    private static final int CYCLES = 30;
    private static final double CYCLE_TIME = WINDOWS * 2 * STEP;
    private static final float NOISE = 100f;
    private static final Deviation DEV = new Deviation(8, 3e-4);

    private LCMSStorageFactory factory;

    @AfterEach
    public void tearDown() {
        if (factory != null) factory.close();
    }

    private record Fragment(int windowIndex, double mz, double centerRt, double sigma, double apexIntensity) {}

    private ProcessedSample sample(List<Fragment> fragments) throws IOException {
        factory = LCMSStorage.temporaryStorage(null, false);
        final LCMSStorage storage = factory.createNewStorage();
        final DoubleArrayList ms1Rts = new DoubleArrayList();
        final IntArrayList ms1ScanIds = new IntArrayList();
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        final List<SimpleSpectrum> spectra = new ArrayList<>();

        double rt = 0d;
        int scanId = 0;
        for (int cycle = 0; cycle < CYCLES; ++cycle) {
            for (int w = 0; w < WINDOWS; ++w) {
                final int ms1Index = ms1Rts.size();
                ms1Rts.add(rt);
                ms1ScanIds.add(scanId++);
                rt += STEP;
                final double ms2Rt = rt;
                final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
                for (Fragment f : fragments) {
                    if (f.windowIndex() != w) continue;
                    double intensity = f.apexIntensity() * Math.exp(-0.5 * Math.pow((ms2Rt - f.centerRt()) / f.sigma(), 2));
                    if (intensity >= NOISE / 10d) buf.addPeak(f.mz(), intensity);
                }
                if (buf.isEmpty()) buf.addPeak(1000.0, NOISE); // never leave a spectrum completely empty
                headers.add(new Ms2SpectrumHeader("scan=" + scanId, scanId, 1, 2, true,
                        new CollisionEnergy(35), IsolationWindow.fromOffsets(50, 50), ms1Index,
                        200d + w * 100d, 200d + w * 100d, ms2Rt));
                spectra.add(new SimpleSpectrum(buf));
                ++scanId;
                rt += STEP;
            }
        }
        final ScanPointMapping mapping = new ScanPointMapping(ms1Rts.toDoubleArray(), ms1ScanIds.toIntArray(), null);
        storage.setMapping(mapping);
        final float[] noise = new float[mapping.length()];
        java.util.Arrays.fill(noise, NOISE);
        storage.setStatistics(SampleStats.builder().noiseLevelPerScan(noise).ms2NoiseLevel(NOISE)
                .minimumIntensity(1f).ms1MassDeviationWithinTraces(DEV)
                .minimumMs1MassDeviationBetweenTraces(new Deviation(10, 3e-4)).build());
        for (int k = 0; k < mapping.length(); ++k) {
            SimpleMutableSpectrum ms1 = new SimpleMutableSpectrum();
            ms1.addPeak(400.1234, 10000d);
            storage.getSpectrumStorage().addSpectrum(new Ms1SpectrumHeader(k, ms1ScanIds.getInt(k), null, 1, true), new SimpleSpectrum(ms1));
        }
        for (int k = 0; k < headers.size(); ++k) storage.getSpectrumStorage().addMs2Spectrum(headers.get(k), spectra.get(k));
        storage.commit();
        return new ProcessedSample(mapping, storage, 1, 0);
    }

    private static DiaChannel channelCovering(DiaScanIndex index, double mz) {
        List<DiaChannel> hits = index.channelsCovering(mz);
        assertEquals(1, hits.size());
        return hits.getFirst();
    }

    private Ms2ChannelTracePicker picker(ProcessedSample sample, DiaChannel channel, Ms2ChannelTraceCache cache) {
        return new Ms2ChannelTracePicker(sample, channel, DiaChannelNoise.estimate(sample, channel),
                cache, new PersistentHomology());
    }

    @Test
    public void aStoredTraceGetsAUidAndCanBeFoundAgain() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 30d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);
        Ms2ChannelTraceCache cache = new Ms2ChannelTraceCache(sample, channel, DEV);

        int seed = channel.channelIndexForRetentionTime(30d);
        ContiguousTrace trace = picker(sample, channel, cache).detectTrace(seed, 100.05).orElseThrow();

        assertTrue(trace.getUid() >= 0, "a stored trace must have a uid");
        assertEquals(1, cache.numberOfTraces());
        assertEquals(1, sample.getStorage().getMs2TraceStorage().numberOfTraces());

        ContiguousTrace found = cache.getTraceFromCache(seed, trace.mz(seed)).orElseThrow();
        assertEquals(trace.getUid(), found.getUid());
    }

    /** Picking the same fragment twice must give the same trace, not a second copy of it. */
    @Test
    public void pickingTheSameFragmentTwiceReusesTheStoredTrace() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 30d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);
        Ms2ChannelTraceCache cache = new Ms2ChannelTraceCache(sample, channel, DEV);
        Ms2ChannelTracePicker picker = picker(sample, channel, cache);

        int apex = channel.channelIndexForRetentionTime(30d);
        ContiguousTrace first = picker.detectTrace(apex, 100.05).orElseThrow();
        // seed from a different scan point of the very same peak
        ContiguousTrace second = picker.detectTrace(apex + 1, first.mz(apex + 1)).orElseThrow();

        assertEquals(first.getUid(), second.getUid());
        assertEquals(1, cache.numberOfTraces(), "the fragment must be stored exactly once");
    }

    @Test
    public void twoFragmentsOfOneChannelAreStoredSeparately() throws IOException {
        ProcessedSample sample = sample(List.of(
                new Fragment(2, 100.05, 30d, 3d, 10000d),
                new Fragment(2, 150.08, 30d, 3d, 5000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);
        Ms2ChannelTraceCache cache = new Ms2ChannelTraceCache(sample, channel, DEV);
        Ms2ChannelTracePicker picker = picker(sample, channel, cache);

        int seed = channel.channelIndexForRetentionTime(30d);
        ContiguousTrace a = picker.detectTrace(seed, 100.05).orElseThrow();
        ContiguousTrace b = picker.detectTrace(seed, 150.08).orElseThrow();

        assertNotEquals(a.getUid(), b.getUid());
        assertEquals(2, cache.numberOfTraces());
    }

    /**
     * The same fragment m/z in two windows. Each channel has its own store, so neither lookup may ever see the
     * other channel's trace.
     */
    @Test
    public void channelsDoNotSeeEachOthersTraces() throws IOException {
        ProcessedSample sample = sample(List.of(
                new Fragment(0, 100.05, 30d, 3d, 10000d),
                new Fragment(2, 100.05, 30d, 3d, 4000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel first = channelCovering(index, 200);
        DiaChannel second = channelCovering(index, 400);

        Ms2ChannelTraceCache cacheA = new Ms2ChannelTraceCache(sample, first, DEV);
        Ms2ChannelTraceCache cacheB = new Ms2ChannelTraceCache(sample, second, DEV);

        ContiguousTrace a = picker(sample, first, cacheA)
                .detectTrace(first.channelIndexForRetentionTime(30d), 100.05).orElseThrow();
        ContiguousTrace b = picker(sample, second, cacheB)
                .detectTrace(second.channelIndexForRetentionTime(30d), 100.05).orElseThrow();

        assertNotEquals(a.getUid(), b.getUid(), "the two channels must not share a trace");
        assertEquals(1, cacheA.numberOfTraces());
        assertEquals(1, cacheB.numberOfTraces());
        assertEquals(2, sample.getStorage().getMs2TraceStorage().numberOfTraces());
        // the intensities tell them apart: the second window's fragment is weaker
        assertTrue(a.apexIntensity() > b.apexIntensity());

        // and a lookup in one channel must never return the other channel's trace
        assertEquals(a.getUid(), cacheA.getTraceFromCache(first.channelIndexForRetentionTime(30d), 100.05).orElseThrow().getUid());
        assertEquals(b.getUid(), cacheB.getTraceFromCache(second.channelIndexForRetentionTime(30d), 100.05).orElseThrow().getUid());
    }

    /** After a round trip through the store the trace must still be bound to its channel's retention times. */
    @Test
    public void traceKeepsItsChannelAxisAcrossAStoreRoundTrip() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 30d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);
        Ms2ChannelTraceCache cache = new Ms2ChannelTraceCache(sample, channel, DEV);

        int seed = channel.channelIndexForRetentionTime(30d);
        ContiguousTrace stored = picker(sample, channel, cache).detectTrace(seed, 100.05).orElseThrow();
        sample.getStorage().commit();

        Ms2TraceStorage.ChannelTraces traces =
                sample.getStorage().getMs2TraceStorage().forChannel(channel.getChannelId(), channel.getAxis());
        ContiguousTrace reloaded = traces.get(stored.getUid());

        assertNotNull(reloaded);
        assertEquals(stored.startId(), reloaded.startId());
        assertEquals(stored.endId(), reloaded.endId());
        assertEquals(stored.apex(), reloaded.apex());
        assertEquals(stored.averagedMz(), reloaded.averagedMz(), 1e-9);
        for (int k = reloaded.startId(); k <= reloaded.endId(); ++k) {
            assertEquals(channel.getAxis().getRetentionTimeAt(k), reloaded.retentionTime(k), 1e-9,
                    "reloaded trace must still use the channel axis, not the MS1 axis");
        }
        assertEquals(30d, reloaded.retentionTime(reloaded.apex()), CYCLE_TIME);
    }

    @Test
    public void lookupOutsideTheTraceFindsNothing() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 30d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);
        Ms2ChannelTraceCache cache = new Ms2ChannelTraceCache(sample, channel, DEV);

        int seed = channel.channelIndexForRetentionTime(30d);
        ContiguousTrace trace = picker(sample, channel, cache).detectTrace(seed, 100.05).orElseThrow();

        assertTrue(cache.getTraceFromCache(seed, 555.5).isEmpty(), "different mass");
        assertTrue(cache.getTraceFromCache(trace.startId() - 1, 100.05).isEmpty(), "before the trace starts");
        assertTrue(cache.getTraceFromCache(trace.endId() + 1, 100.05).isEmpty(), "after the trace ends");
        assertTrue(cache.getTraceFromCache(-1, 100.05).isEmpty(), "outside the axis");
        assertTrue(cache.getTraceFromCache(channel.numberOfScans(), 100.05).isEmpty(), "outside the axis");
    }
}
