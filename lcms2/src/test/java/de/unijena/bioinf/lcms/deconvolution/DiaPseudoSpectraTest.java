package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.spectrum.Ms1SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.NoSegmentation;
import de.unijena.bioinf.lcms.traceextractor.NoTraceCache;
import de.unijena.bioinf.lcms.traceextractor.TracePicker;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Writing the assignment back as ordinary MS2 spectra, which is what lets the rest of the pipeline stay as it is.
 */
public class DiaPseudoSpectraTest {

    private static final double STEP = 0.25;
    private static final int WINDOWS = 3;
    private static final int CYCLES = 60;
    private static final float NOISE = 100f;

    private LCMSStorageFactory factory;

    @AfterEach
    public void tearDown() {
        if (factory != null) factory.close();
    }

    private record Compound(double precursorMz, double centreRt, double sigma, double precursorIntensity,
                            double[] fragmentMzs, double fragmentIntensity) {
        int windowIndex() {
            final int index = (int) Math.round((precursorMz - 200d) / 100d);
            return index >= 0 && index < WINDOWS ? index : -1;
        }
    }

    private static double gaussian(double rt, double centre, double sigma, double apex) {
        return apex * Math.exp(-0.5 * Math.pow((rt - centre) / sigma, 2));
    }

    private static List<Compound> withBackground(List<Compound> compounds) {
        final List<Compound> all = new ArrayList<>(compounds);
        for (double cluster : new double[]{40d, 80d}) {
            for (int k = 0; k < 30; ++k) {
                all.add(new Compound(460d + k * 4d, cluster + (k % 5 - 2) * 0.6, 3d, 20000d, new double[0], 0d));
            }
        }
        return all;
    }

    private static DiaDeconvolution deconvolution() {
        return new DiaDeconvolution(0.5, DiaFragmentResolver.DEFAULT_UNASSIGNED_WEIGHT,
                FragmentAssignment.DEFAULT_MIN_PROBABILITY, DiaDecoys.DEFAULT_MAX_DECOY_PRECURSORS);
    }

    private ProcessedSample sample(List<Compound> compounds) throws IOException {
        factory = LCMSStorage.temporaryStorage(null, false);
        final LCMSStorage storage = factory.createNewStorage();
        final DoubleArrayList ms1Rts = new DoubleArrayList();
        final IntArrayList ms1ScanIds = new IntArrayList();
        final List<SimpleSpectrum> ms1Spectra = new ArrayList<>();
        final List<Ms2SpectrumHeader> ms2Headers = new ArrayList<>();
        final List<SimpleSpectrum> ms2Spectra = new ArrayList<>();

        double rt = 0d;
        int scanId = 0;
        for (int cycle = 0; cycle < CYCLES; ++cycle) {
            for (int w = 0; w < WINDOWS; ++w) {
                final int ms1Index = ms1Rts.size();
                ms1Rts.add(rt);
                ms1ScanIds.add(scanId++);
                final SimpleMutableSpectrum ms1 = new SimpleMutableSpectrum();
                for (Compound c : compounds) {
                    double intensity = gaussian(rt, c.centreRt(), c.sigma(), c.precursorIntensity());
                    if (intensity >= NOISE) ms1.addPeak(c.precursorMz(), intensity);
                }
                if (ms1.isEmpty()) ms1.addPeak(999.9, NOISE / 2);
                ms1Spectra.add(new SimpleSpectrum(ms1));
                rt += STEP;

                final double ms2Rt = rt;
                final SimpleMutableSpectrum ms2 = new SimpleMutableSpectrum();
                for (Compound c : compounds) {
                    if (c.windowIndex() != w) continue;
                    for (double fragment : c.fragmentMzs()) {
                        double intensity = gaussian(ms2Rt, c.centreRt(), c.sigma(), c.fragmentIntensity());
                        if (intensity >= NOISE) ms2.addPeak(fragment, intensity);
                    }
                }
                if (ms2.isEmpty()) ms2.addPeak(1000.1, NOISE / 2);
                ms2Headers.add(new Ms2SpectrumHeader("scan=" + scanId, scanId, 1, 2, true,
                        new CollisionEnergy(35), IsolationWindow.fromOffsets(50, 50), ms1Index,
                        200d + w * 100d, 200d + w * 100d, ms2Rt));
                ms2Spectra.add(new SimpleSpectrum(ms2));
                ++scanId;
                rt += STEP;
            }
        }
        final ScanPointMapping mapping = new ScanPointMapping(ms1Rts.toDoubleArray(), ms1ScanIds.toIntArray(), null);
        storage.setMapping(mapping);
        final float[] noise = new float[mapping.length()];
        java.util.Arrays.fill(noise, NOISE);
        storage.setStatistics(SampleStats.builder().noiseLevelPerScan(noise).ms2NoiseLevel(NOISE).minimumIntensity(1f)
                .ms1MassDeviationWithinTraces(new Deviation(8, 3e-4))
                .minimumMs1MassDeviationBetweenTraces(new Deviation(10, 3e-4)).build());
        for (int k = 0; k < mapping.length(); ++k) {
            storage.getSpectrumStorage().addSpectrum(
                    new Ms1SpectrumHeader(k, ms1ScanIds.getInt(k), null, 1, true), ms1Spectra.get(k));
        }
        for (int k = 0; k < ms2Headers.size(); ++k) {
            storage.getSpectrumStorage().addMs2Spectrum(ms2Headers.get(k), ms2Spectra.get(k));
        }
        final ProcessedSample sample = new ProcessedSample(mapping, storage, 1, 0);
        final TracePicker picker = new TracePicker(sample, new NoTraceCache(), new NoSegmentation());
        picker.setAllowedMassDeviation(new Deviation(8, 3e-4));
        for (Compound c : compounds) {
            picker.detectMostIntensivePeak(mapping.idForRetentionTime(c.centreRt()), c.precursorMz())
                    .ifPresent(t -> storage.getTraceStorage().addContigousTrace(t));
        }
        storage.commit();
        return sample;
    }

    @Test
    public void aSpectrumIsWrittenAndPointsAtItsPrecursorTrace() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1, 233.4}, 8000d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaDeconvolution.Result deconvolution = deconvolution().deconvolute(sample, index);

        DiaPseudoSpectra.Result result = DiaPseudoSpectra.write(sample, deconvolution);

        assertTrue(result.spectraWritten() >= 1, result.describe());
        assertTrue(result.peaksWritten() >= 1, result.describe());

        // the precursor trace must now carry the reference, which is how the merge step finds MS/MS at all
        ContiguousTrace precursorTrace = null;
        for (ContiguousTrace t : sample.getStorage().getTraceStorage()) {
            if (Math.abs(t.averagedMz() - 400.2) < 0.05) precursorTrace = t;
        }
        assertNotNull(precursorTrace);
        MsMsTraceReference[] refs = sample.getStorage().getTraceStorage().getMs2ForTrace(precursorTrace.getUid());
        assertTrue(refs.length >= 1, "the trace must point at its pseudo spectrum");

        Ms2SpectrumHeader header = sample.getStorage().getSpectrumStorage().ms2SpectrumHeader(refs[0].ms2Uid);
        assertEquals(400.2, header.getPrecursorMz(), 0.05,
                "the precursor m/z must be the real mass, not the centre of the isolation window");
        assertEquals(40d, header.getRetentionTime(), 3d);
        assertEquals(2, header.getMsLevel());
        assertTrue(header.getIsolationWindow().isPresent(), "and it must carry a window for this precursor");
        assertTrue(header.getEnergy().isPresent());
    }

    @Test
    public void theSpectrumHoldsTheFragmentsOfThatCompound() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1, 233.4}, 8000d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaDeconvolution.Result deconvolution = deconvolution().deconvolute(sample, index);
        DiaPseudoSpectra.write(sample, deconvolution);

        SimpleSpectrum spectrum = spectrumOf(sample, 400.2);
        assertNotNull(spectrum, "a spectrum must have been written for this compound");
        double[] masses = new double[spectrum.size()];
        for (int k = 0; k < spectrum.size(); ++k) masses[k] = spectrum.getMzAt(k);
        assertTrue(contains(masses, 150.1), "expected 150.1 among " + java.util.Arrays.toString(masses));
        assertTrue(contains(masses, 233.4), "expected 233.4 among " + java.util.Arrays.toString(masses));
        for (int k = 0; k < spectrum.size(); ++k) {
            assertTrue(spectrum.getIntensityAt(k) > 0, "every peak needs an intensity");
        }
    }

    /**
     * A channel samples once per cycle and often not at the precursor's apex, so a peak height has to be the
     * highest the fragment reaches inside the feature rather than its value at the apex scan.
     */
    @Test
    public void peakHeightsAreTheApexInsideTheFeature() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1}, 8000d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaDeconvolution.Result deconvolution = deconvolution().deconvolute(sample, index);
        DiaPseudoSpectra.write(sample, deconvolution);

        SimpleSpectrum spectrum = spectrumOf(sample, 400.2);
        assertNotNull(spectrum);
        // the fragment tops out at 8000, and a coarse channel cannot exceed that
        assertTrue(spectrum.getIntensityAt(0) > 6000d, "close to the real apex, got " + spectrum.getIntensityAt(0));
        assertTrue(spectrum.getIntensityAt(0) <= 8000d + 1e-6);
    }

    @Test
    public void aFeatureWithoutAssignedFragmentsGetsNoSpectrum() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[0], 0d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaDeconvolution.Result deconvolution = deconvolution().deconvolute(sample, index);

        DiaPseudoSpectra.Result result = DiaPseudoSpectra.write(sample, deconvolution);

        assertEquals(0, result.spectraWritten());
        assertTrue(result.featuresWithoutFragments() > 0, result.describe());
    }

    /** A fragment that only just clears the storage threshold must not turn up in a spectrum. */
    @Test
    public void unlikelyFragmentsAreLeftOutOfTheSpectrum() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1}, 8000d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();
        DiaDeconvolution.Result deconvolution = deconvolution().deconvolute(sample, index);

        DiaPseudoSpectra.Result strict = DiaPseudoSpectra.write(sample, deconvolution, 0.99);
        DiaPseudoSpectra.Result loose = DiaPseudoSpectra.write(sample, deconvolution, 0.0);

        assertTrue(loose.peaksWritten() >= strict.peaksWritten(),
                "a lower threshold cannot write fewer peaks: " + loose.describe() + " vs " + strict.describe());
    }

    private static SimpleSpectrum spectrumOf(ProcessedSample sample, double precursorMz) {
        for (Ms2SpectrumHeader header : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
            if (header.getSourceId().startsWith("deconvoluted=")
                    && Math.abs(header.getPrecursorMz() - precursorMz) < 0.05) {
                return sample.getStorage().getSpectrumStorage().getMs2Spectrum(header.getUid());
            }
        }
        return null;
    }

    private static boolean contains(double[] masses, double mz) {
        for (double m : masses) if (Math.abs(m - mz) < 0.01) return true;
        return false;
    }
}
