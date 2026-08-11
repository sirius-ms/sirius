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
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.traceextractor.NoTraceCache;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fragment chromatograms are picked along the retention time axis of a single DIA channel. The tests below are
 * mostly about that axis: a fragment trace has to end up on the retention times of the MS2 scans that actually
 * contain it, and it must never walk into the scans of a different isolation window.
 */
public class Ms2ChannelTracePickerTest {

    private static final double MS1_STEP = 0.25;          // seconds between two consecutive scans
    private static final int WINDOWS = 4;                 // 200, 300, 400, 500 (+-50)
    private static final int CYCLES = 40;
    private static final double CYCLE_TIME = WINDOWS * 2 * MS1_STEP;   // 2 s
    private static final float NOISE = 100f;
    /** below this a real instrument would not record a centroid at all */
    private static final double DETECTION_LIMIT = NOISE / 10d;

    private LCMSStorageFactory factory;

    @AfterEach
    public void tearDown() {
        if (factory != null) factory.close();
    }

    /**
     * A fragment peak that elutes as a gaussian. {@code baseline} is the intensity at which the m/z is present in
     * every scan of the window regardless of elution, as happens for fragment masses that are shared with the
     * chemical background.
     */
    private record Fragment(int windowIndex, double mz, double centerRt, double sigma, double apexIntensity,
                            double baseline) {
        Fragment(int windowIndex, double mz, double centerRt, double sigma, double apexIntensity) {
            this(windowIndex, mz, centerRt, sigma, apexIntensity, 0d);
        }

        double intensityAt(double rt) {
            return Math.max(baseline, apexIntensity * Math.exp(-0.5 * Math.pow((rt - centerRt) / sigma, 2)));
        }
    }

    /**
     * Builds a SWATH-like sample: {@link #WINDOWS} windows, one MS1 in front of every MS2, {@link #CYCLES} cycles.
     * Every MS2 scan of a window contains the fragments assigned to that window plus a constant noise peak.
     */
    private ProcessedSample sample(List<Fragment> fragments) throws IOException {
        factory = LCMSStorage.temporaryStorage(null, false);
        final LCMSStorage storage = factory.createNewStorage();

        final DoubleArrayList ms1Rts = new DoubleArrayList();
        final IntArrayList ms1ScanIds = new IntArrayList();
        final List<Ms2SpectrumHeader> ms2Headers = new ArrayList<>();
        final List<SimpleSpectrum> ms2Spectra = new ArrayList<>();

        double rt = 0d;
        int scanId = 0;
        for (int cycle = 0; cycle < CYCLES; ++cycle) {
            for (int w = 0; w < WINDOWS; ++w) {
                // MS1
                final int ms1Index = ms1Rts.size();
                ms1Rts.add(rt);
                ms1ScanIds.add(scanId++);
                rt += MS1_STEP;
                // MS2 of window w
                final double ms2Rt = rt;
                final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
                buf.addPeak(77.7, NOISE);   // something that is always there but never a real signal
                for (Fragment f : fragments) {
                    if (f.windowIndex() != w) continue;
                    double intensity = f.intensityAt(ms2Rt);
                    if (intensity >= DETECTION_LIMIT) buf.addPeak(f.mz(), intensity);
                }
                ms2Headers.add(new Ms2SpectrumHeader("scan=" + scanId, scanId, 1, 2, true,
                        new CollisionEnergy(35), IsolationWindow.fromOffsets(50, 50), ms1Index,
                        200d + w * 100d, 200d + w * 100d, ms2Rt));
                ms2Spectra.add(new SimpleSpectrum(buf));
                ++scanId;
                rt += MS1_STEP;
            }
        }

        final ScanPointMapping mapping = new ScanPointMapping(ms1Rts.toDoubleArray(), ms1ScanIds.toIntArray(), null);
        storage.setMapping(mapping);
        final float[] noisePerScan = new float[mapping.length()];
        java.util.Arrays.fill(noisePerScan, NOISE);
        storage.setStatistics(SampleStats.builder()
                .noiseLevelPerScan(noisePerScan)
                .ms2NoiseLevel(NOISE)
                .minimumIntensity(1f)
                .ms1MassDeviationWithinTraces(new Deviation(8, 3e-4))
                .minimumMs1MassDeviationBetweenTraces(new Deviation(10, 3e-4))
                .build());
        for (int k = 0; k < mapping.length(); ++k) {
            SimpleMutableSpectrum ms1 = new SimpleMutableSpectrum();
            ms1.addPeak(400.1234, 10000d);
            storage.getSpectrumStorage().addSpectrum(new Ms1SpectrumHeader(k, ms1ScanIds.getInt(k), null, 1, true), new SimpleSpectrum(ms1));
        }
        for (int k = 0; k < ms2Headers.size(); ++k) {
            storage.getSpectrumStorage().addMs2Spectrum(ms2Headers.get(k), ms2Spectra.get(k));
        }
        storage.commit();
        return new ProcessedSample(mapping, storage, 1, 0);
    }

    private Ms2ChannelTracePicker picker(ProcessedSample sample, DiaChannel channel) {
        return new Ms2ChannelTracePicker(sample, channel, DiaChannelNoise.estimate(sample, channel),
                new NoTraceCache(), new PersistentHomology());
    }

    private static DiaChannel channelCovering(DiaScanIndex index, double precursorMz) {
        List<DiaChannel> hits = index.channelsCovering(precursorMz);
        assertEquals(1, hits.size(), "expected exactly one channel for m/z " + precursorMz);
        return hits.getFirst();
    }

    @Test
    public void fragmentTraceLivesOnTheRetentionTimesOfItsOwnChannel() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 40d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);
        Ms2ChannelTracePicker picker = picker(sample, channel);

        int seed = channel.channelIndexForRetentionTime(40d);
        ContiguousTrace trace = picker.detectTrace(seed, 100.05).orElseThrow();

        // the apex must sit at the retention time the fragment actually eluted at, not somewhere else on the
        // MS1 axis. One cycle of tolerance, since the channel only samples once per cycle.
        assertEquals(40d, trace.retentionTime(trace.apex()), CYCLE_TIME);
        assertEquals(100.05, trace.averagedMz(), 1e-3);
        // and every scan point of the trace must be a scan point of this channel
        for (int k = trace.startId(); k <= trace.endId(); ++k) {
            assertEquals(channel.getAxis().getRetentionTimeAt(k), trace.retentionTime(k), 1e-9);
        }
    }

    @Test
    public void traceIsContiguousAndCoversTheElutingPeak() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 40d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        ContiguousTrace trace = picker(sample, channel)
                .detectTrace(channel.channelIndexForRetentionTime(40d), 100.05).orElseThrow();

        assertTrue(trace.endId() > trace.startId());
        assertTrue(trace.startId() >= 0 && trace.endId() < channel.numberOfScans());
        // a 3 s sigma peak sampled every 2 s: several points, but nowhere near the whole run
        assertTrue(trace.endId() - trace.startId() + 1 >= 3, "peak should cover at least three scan points");
        assertTrue(trace.endId() - trace.startId() + 1 < channel.numberOfScans() / 2);
        for (int k = trace.startId(); k <= trace.endId(); ++k) {
            assertTrue(trace.intensity(k) > 0, "a contiguous trace must not contain empty scan points");
        }
    }

    /**
     * The same fragment m/z in two different isolation windows must not end up in one trace. This is the failure
     * mode we would get from mapping all MS2 scans onto a single shared axis.
     */
    @Test
    public void fragmentsOfDifferentWindowsDoNotEndUpInTheSameTrace() throws IOException {
        ProcessedSample sample = sample(List.of(
                new Fragment(1, 100.05, 20d, 3d, 10000d),
                new Fragment(2, 100.05, 60d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaChannel first = channelCovering(index, 300);
        ContiguousTrace early = picker(sample, first)
                .detectTrace(first.channelIndexForRetentionTime(20d), 100.05).orElseThrow();
        assertEquals(20d, early.retentionTime(early.apex()), CYCLE_TIME);

        DiaChannel second = channelCovering(index, 400);
        ContiguousTrace late = picker(sample, second)
                .detectTrace(second.channelIndexForRetentionTime(60d), 100.05).orElseThrow();
        assertEquals(60d, late.retentionTime(late.apex()), CYCLE_TIME);

        // neither trace may reach into the retention time of the other window's fragment
        assertTrue(early.retentionTime(early.endId()) < 40d,
                "trace of the first window ends at " + early.retentionTime(early.endId()));
        assertTrue(late.retentionTime(late.startId()) > 40d,
                "trace of the second window starts at " + late.retentionTime(late.startId()));
    }

    /**
     * Two compounds in the same window sharing a fragment m/z that is also present in the background. The trace
     * spans the whole run, and it is the segmentation that has to separate the two compounds - along the channel's
     * retention times, which are ten times coarser than the MS1 ones.
     */
    @Test
    public void twoElutingPeaksOfTheSameFragmentAreSegmented() throws IOException {
        ProcessedSample sample = sample(List.of(
                new Fragment(2, 100.05, 20d, 3d, 10000d, 30d),
                new Fragment(2, 100.05, 50d, 3d, 8000d, 30d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        ContiguousTrace trace = picker(sample, channel)
                .detectTrace(channel.channelIndexForRetentionTime(20d), 100.05).orElseThrow();

        assertTrue(trace.retentionTime(trace.startId()) < 20d);
        assertTrue(trace.retentionTime(trace.endId()) > 50d, "the background keeps the trace alive across both peaks");
        assertEquals(2, trace.getSegments().length, "each of the two compounds should get its own segment");
        double firstApexRt = trace.retentionTime(trace.getSegments()[0].apex);
        double secondApexRt = trace.retentionTime(trace.getSegments()[1].apex);
        assertEquals(20d, firstApexRt, CYCLE_TIME);
        assertEquals(50d, secondApexRt, CYCLE_TIME);
    }

    /** Two compounds whose fragment signal dies out in between simply give two separate traces. */
    @Test
    public void wellSeparatedPeaksOfTheSameFragmentGiveSeparateTraces() throws IOException {
        ProcessedSample sample = sample(List.of(
                new Fragment(2, 100.05, 20d, 3d, 10000d),
                new Fragment(2, 100.05, 60d, 3d, 8000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        ContiguousTrace first = picker(sample, channel)
                .detectTrace(channel.channelIndexForRetentionTime(20d), 100.05).orElseThrow();
        ContiguousTrace second = picker(sample, channel)
                .detectTrace(channel.channelIndexForRetentionTime(60d), 100.05).orElseThrow();

        assertEquals(20d, first.retentionTime(first.apex()), CYCLE_TIME);
        assertEquals(60d, second.retentionTime(second.apex()), CYCLE_TIME);
        assertTrue(first.endId() < second.startId(), "the two traces must not overlap");
    }

    @Test
    public void aPeakInASingleScanIsNotATrace() throws IOException {
        // sigma small enough that only one scan of the channel sees it
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 40d, 0.2d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        Optional<ContiguousTrace> trace = picker(sample, channel)
                .detectTrace(channel.channelIndexForRetentionTime(40d), 100.05);
        assertTrue(trace.isEmpty(), "a single scan point cannot be a chromatographic peak");
    }

    @Test
    public void askingForAnAbsentMassYieldsNothing() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 40d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        assertTrue(picker(sample, channel).detectTrace(10, 555.5555).isEmpty());
    }

    @Test
    public void detectMostIntensivePeakFindsTheFragment() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 40d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        ContiguousTrace trace = picker(sample, channel)
                .detectMostIntensivePeak(channel.channelIndexForRetentionTime(40d), 100.0505).orElseThrow();
        assertEquals(100.05, trace.averagedMz(), 1e-3);
    }

    @Test
    public void noiseLevelIsEstimatedFromTheChannelItself() throws IOException {
        ProcessedSample sample = sample(List.of(new Fragment(2, 100.05, 40d, 3d, 10000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);

        float noise = DiaChannelNoise.estimate(sample, channel);
        assertTrue(noise > 0, "a channel with signal must have a positive noise level");
        assertTrue(noise <= 10000f, "the noise level must not be dominated by the eluting fragment");
    }

    @Test
    public void emptyChannelFallsBackToTheSampleWideNoiseLevel() throws IOException {
        ProcessedSample sample = sample(List.of());
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaChannel channel = channelCovering(index, 400);
        // every scan of this channel holds a single peak, which is too little to estimate anything from
        assertEquals(NOISE, DiaChannelNoise.estimate(sample, channel), 1e-3);
    }
}
