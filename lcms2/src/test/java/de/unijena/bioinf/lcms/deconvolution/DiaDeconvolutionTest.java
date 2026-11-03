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
import de.unijena.bioinf.lcms.trace.segmentation.NoSegmentation;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
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
 * The whole of stage one on a synthetic sample: three isolation windows, compounds with known fragments, and an
 * interfering compound that co-elutes with one of them in the same window.
 */
public class DiaDeconvolutionTest {

    private static final double STEP = 0.25;                 // seconds between consecutive scans
    private static final int WINDOWS = 3;                    // 200, 300, 400 (+-50)
    private static final int CYCLES = 60;
    private static final float NOISE = 100f;

    private LCMSStorageFactory factory;

    @AfterEach
    public void tearDown() {
        if (factory != null) factory.close();
    }

    /** a compound: a precursor m/z with an elution profile, and the fragments it produces */
    private record Compound(double precursorMz, double centreRt, double sigma, double precursorIntensity,
                            double[] fragmentMzs, double fragmentIntensity) {
        int windowIndex() {
            final int index = (int) Math.round((precursorMz - 200d) / 100d);
            return index >= 0 && index < WINDOWS ? index : -1;   // outside every window
        }
    }

    private static double gaussian(double rt, double centre, double sigma, double apex) {
        return apex * Math.exp(-0.5 * Math.pow((rt - centre) / sigma, 2));
    }

    /**
     * Compounds outside every isolation window, co-eluting with everything else. These are what the decoys are
     * drawn from: without them a channel has no null distribution and, since with n decoys the strongest claim is
     * log(1/(n+1)), nothing can be called significant and every fragment stays unassigned.
     */
    private static List<Compound> background(double clusterRt, int count) {
        final List<Compound> background = new java.util.ArrayList<>();
        for (int k = 0; k < count; ++k) {
            // outside every isolation window but inside the mass band decoys are drawn from, and co-eluting with
            // the real compounds - a decoy only produces an edge if it overlaps the fragments in time
            background.add(new Compound(460d + k * 4d, clusterRt + (k % 5 - 2) * 0.6, 3d, 20000d, new double[0], 0d));
        }
        return background;
    }

    private static List<Compound> withBackground(List<Compound> compounds) {
        final List<Compound> all = new java.util.ArrayList<>(compounds);
        all.addAll(background(40d, 30));
        all.addAll(background(80d, 30));
        return all;
    }

    /** a lower significance floor, so the mechanism can be tested without thousands of decoys */
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
                // MS1: every precursor of every compound
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

                // MS2 of window w: the fragments of the compounds isolated by it
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

        // pick the MS1 traces the way the pipeline does, so the deconvolution sees real precursor features
        final TracePicker picker = new TracePicker(sample, new NoTraceCache(), new NoSegmentation());
        picker.setAllowedMassDeviation(new Deviation(8, 3e-4));
        for (Compound c : compounds) {
            final int scan = mapping.idForRetentionTime(c.centreRt());
            picker.detectMostIntensivePeak(scan, c.precursorMz())
                    .ifPresent(t -> storage.getTraceStorage().addContigousTrace(t));
        }
        storage.commit();
        return sample;
    }

    private static int fragmentIndexOf(DiaDeconvolution.ChannelResult channel, double mz) {
        List<ContiguousTrace> fragments = channel.getGraph().getFragments();
        for (int k = 0; k < fragments.size(); ++k) {
            if (Math.abs(fragments.get(k).averagedMz() - mz) < 0.01) return k;
        }
        return -1;
    }

    private static DiaDeconvolution.ChannelResult channelCovering(DiaDeconvolution.Result result, double mz) {
        for (DiaDeconvolution.ChannelResult c : result.getChannels()) {
            if (c.getChannel().covers(mz)) return c;
        }
        throw new AssertionError("no channel covers " + mz);
    }

    @Test
    public void fragmentsAreAssignedToTheCompoundTheyBelongTo() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1, 233.4}, 8000d),
                new Compound(300.5, 80d, 3d, 30000d, new double[]{120.7, 199.3}, 6000d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaDeconvolution.Result result = deconvolution().deconvolute(sample, index);

        assertFalse(result.nullDistributionTooThin(), "the fixture must provide a usable null distribution");
        assertEquals(3, result.getChannels().size());
        assertTrue(result.numberOfPrecursorFeatures() >= 2, "both precursors must become features");
        assertTrue(result.numberOfAssignedFragments() >= 2, "the fragments must be assigned somewhere");

        DiaDeconvolution.ChannelResult channel = channelCovering(result, 400.2);
        int fragment = fragmentIndexOf(channel, 150.1);
        assertTrue(fragment >= 0, "the fragment must have been picked");
        FragmentAssignment assignment = channel.assignmentOf(fragment);
        assertFalse(assignment.isEmpty(), "and assigned: " + assignment);
        PrecursorFeature owner = channel.getGraph().getPrecursors().get(assignment.precursorIndexAt(0));
        assertEquals(400.2, owner.getMz(), 0.05, "assigned to the compound whose window it was measured in");
    }

    /** A fragment of one window must never be attributed to a precursor of another. */
    @Test
    public void fragmentsStayInsideTheirOwnChannel() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1}, 8000d),
                new Compound(300.5, 40d, 3d, 40000d, new double[]{150.1}, 8000d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaDeconvolution.Result result = deconvolution().deconvolute(sample, index);

        for (DiaDeconvolution.ChannelResult channel : result.getChannels()) {
            for (int f = 0; f < channel.getGraph().getFragments().size(); ++f) {
                FragmentAssignment assignment = channel.assignmentOf(f);
                for (int rank = 0; rank < assignment.size(); ++rank) {
                    PrecursorFeature owner = channel.getGraph().getPrecursors().get(assignment.precursorIndexAt(rank));
                    assertTrue(channel.getChannel().covers(owner.getMz()),
                            "a fragment was assigned to a precursor outside its channel");
                }
            }
        }
    }

    /**
     * Two compounds co-eluting in one window, sharing a fragment. A single sample cannot attribute that fragment,
     * and the assignment has to say so rather than pick one.
     */
    @Test
    public void aSharedFragmentOfTwoCoElutingCompoundsIsAmbiguous() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1, 233.4}, 8000d),
                new Compound(420.7, 40d, 3d, 35000d, new double[]{150.1, 301.9}, 7000d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaDeconvolution.Result result = deconvolution().deconvolute(sample, index);
        DiaDeconvolution.ChannelResult channel = channelCovering(result, 400.2);

        int shared = fragmentIndexOf(channel, 150.1);
        assertTrue(shared >= 0);
        FragmentAssignment assignment = channel.assignmentOf(shared);
        assertTrue(assignment.size() >= 2 || assignment.isEmpty(),
                "a fragment of two indistinguishable compounds must not be attributed to one: " + assignment);
    }

    @Test
    public void everyChannelGetsItsOwnNoiseLevelAndNullDistribution() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1, 233.4}, 8000d),
                new Compound(300.5, 80d, 3d, 30000d, new double[]{120.7}, 6000d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaDeconvolution.Result result = deconvolution().deconvolute(sample, index);

        for (DiaDeconvolution.ChannelResult channel : result.getChannels()) {
            assertTrue(channel.getNoiseLevel() > 0, "every channel needs a noise level");
        }
        // and the report has to name the numbers we designed this against
        String description = result.describe();
        assertTrue(description.contains("DIA deconvolution"), description);
        assertTrue(description.contains("channel 0"), description);
    }

    @Test
    public void aSampleWithoutFragmentsProducesEmptyChannels() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[0], 0d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaDeconvolution.Result result = deconvolution().deconvolute(sample, index);

        assertEquals(0, result.numberOfAssignedFragments());
        assertEquals(0, result.numberOfAmbiguousFragments());
    }

    /**
     * A sample too small to provide decoys cannot support any claim of significance, and has to say so instead of
     * quietly returning nothing. With n decoys the strongest possible statement is log(1/(n+1)), so a handful of
     * them cannot clear any sensible significance floor.
     */
    @Test
    public void aSampleWithoutDecoysReportsThatItCannotDecide() throws IOException {
        // no background at all, so there is nothing outside the windows to build a null distribution from
        ProcessedSample sample = sample(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1, 233.4}, 8000d)));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaDeconvolution.Result result = deconvolution().deconvolute(sample, index);

        assertTrue(result.nullDistributionTooThin(), "the sample cannot calibrate anything");
        assertEquals(0, result.numberOfAssignedFragments(), "so nothing may be assigned");
        assertTrue(result.describe().contains("WARNING"), "and it has to be reported:\n" + result.describe());
    }

    /** Same input, same output: the null distribution must not wander between runs. */
    @Test
    public void deconvolutionIsReproducible() throws IOException {
        ProcessedSample sample = sample(withBackground(List.of(
                new Compound(400.2, 40d, 3d, 40000d, new double[]{150.1, 233.4}, 8000d),
                new Compound(420.7, 45d, 3d, 35000d, new double[]{301.9}, 7000d))));
        DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow();

        DiaDeconvolution.Result first = deconvolution().deconvolute(sample, index);
        DiaDeconvolution.Result second = deconvolution().deconvolute(sample, index);

        assertEquals(first.numberOfAssignedFragments(), second.numberOfAssignedFragments());
        assertEquals(first.numberOfAmbiguousFragments(), second.numberOfAmbiguousFragments());
        DiaDeconvolution.ChannelResult a = channelCovering(first, 400.2), b = channelCovering(second, 400.2);
        for (int f = 0; f < a.getGraph().getFragments().size(); ++f) {
            assertEquals(a.assignmentOf(f).probabilityOfMostLikely(), b.assignmentOf(f).probabilityOfMostLikely(), 1e-12);
        }
    }
}
