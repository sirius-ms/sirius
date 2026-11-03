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
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Picking the fragment chromatograms of a channel in one pass. The channel is built the same way the real parser
 * would, so the picker sees genuine MS2 spectra rather than hand made traces.
 */
public class DiaChannelFragmentsTest {

    private static final double STEP = 0.25;
    private static final int WINDOWS = 3;         // 200, 300, 400 (+-50)
    private static final int CYCLES = 40;
    private static final float NOISE = 100f;

    private LCMSStorageFactory factory;

    @AfterEach
    public void tearDown() {
        if (factory != null) factory.close();
    }

    /** a fragment eluting as a gaussian in one window */
    private record Fragment(int windowIndex, double mz, double centerRt, double sigma, double apexIntensity) {}

    private ProcessedSample sample(List<Fragment> fragments, int noisePeaksPerScan) throws IOException {
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
                // peaks below the noise level, at masses that jitter so they never form a trace anyway
                for (int n = 0; n < noisePeaksPerScan; ++n) {
                    buf.addPeak(600d + n * 7d + (cycle % 5) * 0.05, NOISE * 0.5);
                }
                if (buf.isEmpty()) buf.addPeak(1000.0, NOISE * 0.5);
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
                .minimumIntensity(1f).ms1MassDeviationWithinTraces(new Deviation(8, 3e-4))
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

    /**
     * What a channel reports must be what it stored, and in an order that follows the traces rather than the index.
     * <p>
     * The result used to be collected from what the picker handed back per seed, and the picker hands back the trace
     * that already covers a peak instead of picking a new one. Where two traces cover the same peak - about one lookup
     * in a hundred on real data - which one comes back follows the rectangle index's page layout, so a trace that no
     * lookup happened to return was missing from the result, and differently so between two runs on the same data.
     * That was the last of the run to run variation in the DIA fragment counts.
     */
    @Test
    public void theResultIsEveryStoredTraceInADefinedOrder() throws IOException {
        ProcessedSample sample = sample(List.of(
                new Fragment(2, 150.1, 30d, 3d, 5000d),
                new Fragment(2, 233.4, 30d, 3d, 3000d),
                new Fragment(2, 301.9, 50d, 3d, 4000d)), 0);
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        DiaChannelFragments.Result result = DiaChannelFragments.pickAll(sample, channel, NOISE);

        // nothing the channel stored may be absent from what it reports
        assertEquals(sample.getStorage().getMs2TraceStorage().forChannel(channel.getChannelId(), channel.getAxis()).size(),
                result.numberOfTraces(), "every stored trace is reported");
        // and the order is the traces' own, so anything indexed by it is reproducible
        final double[] reported = result.traces().stream().mapToDouble(ContiguousTrace::averagedMz).toArray();
        final double[] ascending = reported.clone();
        java.util.Arrays.sort(ascending);
        assertArrayEquals(ascending, reported, 1e-9, "reported in ascending mass, not in index order");
    }

    @Test
    public void everyFragmentOfTheChannelIsPickedExactlyOnce() throws IOException {
        ProcessedSample sample = sample(List.of(
                new Fragment(2, 150.1, 30d, 3d, 5000d),
                new Fragment(2, 233.4, 30d, 3d, 3000d),
                new Fragment(2, 301.9, 50d, 3d, 4000d)), 0);
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        DiaChannelFragments.Result result = DiaChannelFragments.pickAll(sample, channel, NOISE);

        assertEquals(3, result.numberOfTraces(), "three fragments, three traces");
        // the traces must be distinct objects with distinct uids, i.e. the cache deduplicated the repeated seeds
        assertEquals(3, result.traces().stream().mapToInt(ContiguousTrace::getUid).distinct().count());
        double[] masses = result.traces().stream().mapToDouble(ContiguousTrace::averagedMz).sorted().toArray();
        assertArrayEquals(new double[]{150.1, 233.4, 301.9}, masses, 1e-3);
    }

    @Test
    public void seedingHappensManyMoreTimesThanTracesAreProduced() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 150.1, 30d, 3d, 5000d)), 0);
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        DiaChannelFragments.Result result = DiaChannelFragments.pickAll(sample, channel, NOISE);

        assertEquals(1, result.numberOfTraces());
        assertTrue(result.seedsTried() > 5, "the fragment is above noise in several scans, got " + result.seedsTried());
    }

    @Test
    public void peaksAtOrBelowTheNoiseLevelAreNotSeeded() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 150.1, 30d, 3d, 5000d)), 20);
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        DiaChannelFragments.Result result = DiaChannelFragments.pickAll(sample, channel, NOISE);

        assertTrue(result.seedsBelowNoise() >= 20 * CYCLES - 5,
                "the noise peaks must be skipped, skipped " + result.seedsBelowNoise());
        assertEquals(1, result.numberOfTraces(), "and must not produce traces of their own");
    }

    /** Fragments of one window must not turn up when the neighbouring window is picked. */
    @Test
    public void aChannelOnlySeesItsOwnFragments() throws IOException {
        ProcessedSample sample = sample(List.of(
                new Fragment(0, 150.1, 30d, 3d, 5000d),
                new Fragment(2, 233.4, 30d, 3d, 3000d)), 0);
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaChannelFragments.Result first = DiaChannelFragments.pickAll(sample, channelCovering(index, 200), NOISE);
        DiaChannelFragments.Result third = DiaChannelFragments.pickAll(sample, channelCovering(index, 400), NOISE);

        assertEquals(1, first.numberOfTraces());
        assertEquals(150.1, first.traces().getFirst().averagedMz(), 1e-3);
        assertEquals(1, third.numberOfTraces());
        assertEquals(233.4, third.traces().getFirst().averagedMz(), 1e-3);
    }

    @Test
    public void aChannelWithoutSignalYieldsNoTraces() throws IOException {
        ProcessedSample sample = sample(List.of(), 5);
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaChannelFragments.Result result = DiaChannelFragments.pickAll(sample, channelCovering(index, 400), NOISE);

        assertEquals(0, result.numberOfTraces());
        assertEquals(0, result.seedsTried());
    }

    /**
     * Fragments are picked without segmentation: nothing reads a fragment's own features, since the range it is
     * compared over comes from the precursor. A fragment spanning two eluting peaks must therefore still come back
     * as one segment.
     */
    @Test
    public void fragmentsArePickedWithoutSegmentation() throws IOException {
        ProcessedSample sample = sample(List.of(
                new Fragment(2, 150.1, 20d, 3d, 5000d),
                new Fragment(2, 150.1, 50d, 3d, 4000d)), 0);
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        DiaChannelFragments.Result result = DiaChannelFragments.pickAll(sample, channel, NOISE);

        for (ContiguousTrace trace : result.traces()) {
            assertEquals(1, trace.getSegments().length, "a fragment trace must carry a single segment");
            assertEquals(trace.startId(), trace.getSegments()[0].leftEdge);
            assertEquals(trace.endId(), trace.getSegments()[0].rightEdge);
        }
    }

    /** The picked traces live on the channel axis, which is what everything downstream assumes. */
    @Test
    public void pickedTracesAreOnTheChannelAxis() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 150.1, 30d, 3d, 5000d)), 0);
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        ContiguousTrace trace = DiaChannelFragments.pickAll(sample, channel, NOISE).traces().getFirst();

        for (int k = trace.startId(); k <= trace.endId(); ++k) {
            assertEquals(channel.getAxis().getRetentionTimeAt(k), trace.retentionTime(k), 1e-9);
        }
        assertEquals(30d, trace.retentionTime(trace.apex()), channel.medianScanInterval());
    }
}
