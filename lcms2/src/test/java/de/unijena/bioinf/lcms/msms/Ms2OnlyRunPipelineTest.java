package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.lcms.align.AlignmentBackbone;
import de.unijena.bioinf.lcms.io.SyntheticLcMsRun;
import de.unijena.bioinf.lcms.projectspace.CollectingDatabaseAdapter;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The whole preprocessing on a simulated experiment, one run of which holds nothing but MS/MS spectra.
 * <p>
 * The fragment masses of the simulated compounds are chains of common neutral losses, and their intensities span
 * two orders of magnitude, because that is what the MS/MS quality check looks for. A spectrum of three arbitrary
 * peaks is rejected by it, quite correctly, and would make every assertion here about a spectrum being kept
 * meaningless.
 */
public class Ms2OnlyRunPipelineTest {

    @TempDir
    Path tmp;

    static final double[] CAFFEINE_FRAGMENTS = {55.0291, 83.0240, 109.0396, 137.0709, 155.0815, 182.0924};
    static final double[] TRYPTOPHAN_FRAGMENTS = {59.0491, 89.0597, 115.0754, 143.0703, 171.1016, 189.1122};
    static final double[] ABSENT_FRAGMENTS = {60.0444, 88.0393, 114.0549, 144.0655, 172.0968, 190.1074, 232.1180};
    static final double[] SIX_INTENSITIES = {600, 9000, 4000, 10000, 1200, 800};
    static final double[] SEVEN_INTENSITIES = {700, 9000, 3000, 10000, 1500, 900, 600};

    static final SyntheticLcMsRun.Compound CAFFEINE = new SyntheticLcMsRun.Compound(195.0877, 120d, 30000d, 8,
            CAFFEINE_FRAGMENTS, SIX_INTENSITIES);
    static final SyntheticLcMsRun.Compound TRYPTOPHAN = new SyntheticLcMsRun.Compound(205.0972, 240d, 20000d, 11,
            TRYPTOPHAN_FRAGMENTS, SIX_INTENSITIES);
    /** measured in the MS/MS only run alone - the LC/MS run never saw it, so no feature can be found for it */
    static final SyntheticLcMsRun.Compound ABSENT = new SyntheticLcMsRun.Compound(265.1183, 180d, 15000d, 14,
            ABSENT_FRAGMENTS, SEVEN_INTENSITIES);

    /**
     * The simulator has to produce something the preprocessing recognises before anything can be built on it: if a
     * plain run of two compounds does not come out as two features, no assertion about an MS/MS only run means
     * anything.
     */
    @Test
    public void theSimulatedRunAloneYieldsItsTwoCompounds() throws IOException {
        final CollectingDatabaseAdapter adapter = run(List.of(lcmsRun(CAFFEINE, TRYPTOPHAN)));
        assertTrue(featureAt(adapter, CAFFEINE.precursorMz()).isPresent(), () -> "no feature for caffeine among "
                + describe(adapter));
        assertTrue(featureAt(adapter, TRYPTOPHAN.precursorMz()).isPresent(), "no feature for tryptophan");
        assertTrue(featureAt(adapter, CAFFEINE.precursorMz()).orElseThrow().isHasMsMs(),
                "the caffeine feature has its MS/MS");
    }

    /**
     * The case this exists for: a second run of the same experiment that holds only fragmentation spectra. Its
     * spectra point at precursors the LC/MS run measured, so they belong to those features rather than to features
     * of their own.
     */
    @Test
    public void anMs2OnlyRunHandsItsSpectraToTheFeaturesTheyPointAt() throws IOException {
        final CollectingDatabaseAdapter adapter = run(List.of(
                lcmsRun(CAFFEINE, TRYPTOPHAN), ms2OnlyRun("ms2only.mzML", CAFFEINE, TRYPTOPHAN)));

        final AlignedFeatures caffeine = featureAt(adapter, CAFFEINE.precursorMz()).orElseThrow();
        assertTrue(caffeine.isHasMsMs());
        assertEquals(2, caffeine.getMSData().orElseThrow().getMsnSpectra().size(),
                "one MS/MS from each of the two runs");
        assertEquals(2, adapter.features.size(), "no extra feature is invented for a spectrum that was assigned");
    }

    /**
     * A precursor the LC/MS run never measured. There is nothing to attach the spectrum to, and throwing it away
     * would lose the only measurement of that compound in the experiment, so it becomes a feature carrying nothing
     * but its spectrum.
     */
    @Test
    public void aSpectrumThatFitsNoFeatureBecomesOneOfItsOwn() throws IOException {
        final CollectingDatabaseAdapter adapter = run(List.of(
                lcmsRun(CAFFEINE, TRYPTOPHAN), ms2OnlyRun("ms2only.mzML", CAFFEINE, ABSENT)));

        assertEquals(3, adapter.features.size(), () -> "expected the two LC/MS features plus one for the precursor "
                + "only the MS/MS run saw, got " + describe(adapter));
        final AlignedFeatures own = featureAt(adapter, ABSENT.precursorMz()).orElseThrow();
        assertTrue(own.isHasMsMs());
        assertFalse(own.isHasMs1(), "there is no MS1 scan of this compound anywhere in the experiment");
        assertNull(own.getApexIntensity(), "a feature without a chromatographic peak has no apex intensity");
        assertNull(own.getTraceRef(), "and no trace");
        assertNotNull(own.getCompoundId(), "it is a compound of its own");
        assertEquals(ABSENT.apexRetentionTime(), own.getRetentionTime().getMiddleTime(), 1e-6,
                "the retention time it was fragmented at is known and kept");
    }

    /** the same precursor fragmented several times within one elution is one compound, not several features */
    @Test
    public void repeatedFragmentationOfOneAbsentPrecursorBecomesASingleFeature() throws IOException {
        final Path ms2only = SyntheticLcMsRun.writeMs2OnlyRun(tmp.resolve("ms2only.mzML"), List.of(
                withRetentionTime(ABSENT, 178d), withRetentionTime(ABSENT, 181d), withRetentionTime(ABSENT, 184d)));
        final CollectingDatabaseAdapter adapter = run(List.of(lcmsRun(CAFFEINE, TRYPTOPHAN), ms2only));

        assertEquals(3, adapter.features.size(), () -> "the three spectra are one compound: " + describe(adapter));
        assertEquals(3, featureAt(adapter, ABSENT.precursorMz()).orElseThrow()
                .getMSData().orElseThrow().getMsnSpectra().size(), "all three spectra are kept");
    }

    /** far enough apart in retention time, the same precursor is two different compounds */
    @Test
    public void theSamePrecursorAtADifferentRetentionTimeIsADifferentCompound() throws IOException {
        final Path ms2only = SyntheticLcMsRun.writeMs2OnlyRun(tmp.resolve("ms2only.mzML"), List.of(
                withRetentionTime(ABSENT, 100d), withRetentionTime(ABSENT, 300d)));
        final CollectingDatabaseAdapter adapter = run(List.of(lcmsRun(CAFFEINE, TRYPTOPHAN), ms2only));

        assertEquals(4, adapter.features.size(), () -> "expected two features for the two elutions: "
                + describe(adapter));
    }

    /** a feature with nothing but a spectrum is worth having only if the spectrum says something */
    @Test
    public void aSpectrumTooPoorToStandOnItsOwnIsDiscarded() throws IOException {
        final SyntheticLcMsRun.Compound noise = new SyntheticLcMsRun.Compound(265.1183, 180d, 15000d, 14,
                new double[]{101.3311, 207.7712}, new double[]{110, 130});
        final CollectingDatabaseAdapter adapter = run(List.of(
                lcmsRun(CAFFEINE, TRYPTOPHAN), ms2OnlyRun("ms2only.mzML", noise)));

        assertEquals(2, adapter.features.size(), () -> "only the two LC/MS features remain: " + describe(adapter));
    }

    /** an experiment that consists of nothing but MS/MS: there is no run to align, but the spectra still count */
    @Test
    public void anExperimentOfNothingButMs2StillYieldsItsCompounds() throws IOException {
        final CollectingDatabaseAdapter adapter = run(List.of(ms2OnlyRun("ms2only.mzML", CAFFEINE, ABSENT)));

        assertEquals(2, adapter.features.size(), () -> "both spectra become features: " + describe(adapter));
        assertTrue(featureAt(adapter, CAFFEINE.precursorMz()).isPresent());
        assertTrue(featureAt(adapter, ABSENT.precursorMz()).isPresent());
    }

    private static SyntheticLcMsRun.Compound withRetentionTime(SyntheticLcMsRun.Compound compound, double rt) {
        return new SyntheticLcMsRun.Compound(compound.precursorMz(), rt, compound.apexIntensity(),
                compound.carbonCount(), compound.fragmentMz(), compound.fragmentIntensity());
    }

    private Path lcmsRun(SyntheticLcMsRun.Compound... compounds) throws IOException {
        return SyntheticLcMsRun.writeLcMsRun(tmp.resolve("lcms.mzML"), List.of(compounds), 42L);
    }

    private Path ms2OnlyRun(String name, SyntheticLcMsRun.Compound... compounds) throws IOException {
        return SyntheticLcMsRun.writeMs2OnlyRun(tmp.resolve(name), List.of(compounds));
    }

    private CollectingDatabaseAdapter run(List<Path> files) throws IOException {
        final CollectingDatabaseAdapter adapter = new CollectingDatabaseAdapter();
        final LCMSProcessing processing = new LCMSProcessing(adapter, false);
        try {
            for (Path file : files) processing.processSample(file);
            if (processing.hasLcMsRuns()) {
                final AlignmentBackbone backbone = processing.align();
                final ProcessedSample merged = processing.merge(backbone);
                processing.extractFeaturesAndExportToProjectSpace(merged, backbone);
            } else {
                processing.importMs2OnlyRunsAlone();
            }
        } finally {
            processing.closeStorages();
        }
        return adapter;
    }

    static String describe(CollectingDatabaseAdapter adapter) {
        return adapter.features.stream().map(f -> String.format("%.4f@%.0fs", f.getAverageMass(),
                f.getRetentionTime() == null ? Double.NaN : f.getRetentionTime().getMiddleTime())).toList().toString();
    }

    static Optional<AlignedFeatures> featureAt(CollectingDatabaseAdapter adapter, double mz) {
        return adapter.features.stream().filter(f -> Math.abs(f.getAverageMass() - mz) < 0.01).findFirst();
    }
}
