package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.lcms.projectspace.SiriusDatabaseAdapter;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs a real DIA file through {@link LCMSProcessing#processSample} and reports what the deconvolution actually
 * did. This is the test that replaces the estimates the design was built on - fragments per channel, edges,
 * component sizes, how many features end up with a spectrum - with measurements.
 * <p>
 * Skipped unless a file is given, as system property {@code sirius.dia.testfile} or environment variable
 * {@code SIRIUS_DIA_TEST_FILE}:
 * <pre>
 * SIRIUS_DIA_TEST_FILE=/path/to/file.mzML ./gradlew :lcms2:test --tests '*DiaProcessingFileTest*' -i
 * </pre>
 */
public class DiaProcessingFileTest {

    private static Optional<Path> testFile() {
        String location = System.getProperty("sirius.dia.testfile", System.getenv("SIRIUS_DIA_TEST_FILE"));
        if (location == null || location.isBlank()) return Optional.empty();
        Path path = Path.of(location);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    /** everything the processing wants to persist is thrown away; only ids have to be handed out */
    private static class NoopAdapter implements SiriusDatabaseAdapter {
        private final AtomicLong ids = new AtomicLong(1);

        @Override
        public void importRun(LCMSRun run) {
            run.setRunId(ids.getAndIncrement());
        }

        @Override
        public void updateRun(LCMSRun run) {
        }

        @Override
        public void importMergedRun(MergedLCMSRun mergedRun) {
            mergedRun.setRunId(ids.getAndIncrement());
        }

        @Override
        public void updateMergedRun(MergedLCMSRun mergedRun) {
        }

        @Override
        public void removeMergedRun(MergedLCMSRun run) {
        }

        @Override
        public void importScan(Scan scan) {
        }

        @Override
        public void importMSMSScan(MSMSScan scan) {
        }

        @Override
        public void importTrace(AbstractTrace trace) {
        }

        @Override
        public void removeSourceTrace(long id) {
        }

        @Override
        public void removeMergedTrace(long id) {
        }

        @Override
        public boolean importAlignedFeature(AlignedFeatures alignedFeatures) {
            return true;
        }

        @Override
        public void importRetentionTimeAxis(RetentionTimeAxis axis, boolean update) {
        }

        @Override
        public Stream<AlignedFeatures> getImportedFeatureStream(long runId) {
            return Stream.empty();
        }
    }

    @Test
    public void deconvoluteRealFile() throws IOException {
        Optional<Path> file = testFile();
        assumeTrue(file.isPresent(), "no DIA test file given, set SIRIUS_DIA_TEST_FILE");

        final LCMSProcessing processing = new LCMSProcessing(new NoopAdapter(), false);
        // optional knobs, so the effect of a setting can be measured rather than argued about
        final DiaDeconvolution deconvolution = new DiaDeconvolution();
        final String quantile = System.getenv("SIRIUS_DIA_NOISE_QUANTILE");
        if (quantile != null && !quantile.isBlank()) deconvolution.setNoiseQuantile(Double.parseDouble(quantile));
        final String prior = System.getenv("SIRIUS_DIA_INTENSITY_PRIOR");
        if (prior != null && !prior.isBlank()) deconvolution.setIntensityPrior(Boolean.parseBoolean(prior));
        processing.setDiaAssignmentStrategy(new CorrelationAssignmentStrategy(deconvolution, new DiaRefinement()));
        System.out.printf("settings: noise quantile %.2f, intensity prior %s%n",
                deconvolution.getNoiseQuantile(), deconvolution.isIntensityPrior());
        try {
            final long started = System.currentTimeMillis();
            final ProcessedSample sample = processing.processSample(file.get());
            final long took = System.currentTimeMillis() - started;

            // the sample must have been recognised as DIA and gone down the deconvolution path
            final DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow(
                    () -> new AssertionError("no DIA acquisition scheme in " + file.get()));

            int ms1Traces = 0;
            for (ContiguousTrace ignored : sample.getStorage().getTraceStorage()) ++ms1Traces;

            int pseudoSpectra = 0, realMs2 = 0, pseudoPeaks = 0;
            for (Ms2SpectrumHeader header : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
                if (DiaPseudoSpectra.isDeconvoluted(header)) {
                    ++pseudoSpectra;
                    pseudoPeaks += sample.getStorage().getSpectrumStorage().getMs2Spectrum(header.getUid()).size();
                } else {
                    ++realMs2;
                }
            }

            int tracesWithMsMs = 0;
            for (ContiguousTrace trace : sample.getStorage().getTraceStorage()) {
                if (sample.getStorage().getTraceStorage().getMs2ForTrace(trace.getUid()).length > 0) ++tracesWithMsMs;
            }

            System.out.printf("=== %s ===%n", file.get().getFileName());
            System.out.printf("processed in %.1f s%n", took / 1000d);
            System.out.printf("MS1 scans %d, MS1 traces %d%n", sample.getMapping().length(), ms1Traces);
            // what the deconvolution actually did, straight from the run rather than from a log
            final DiaReport report = processing.getDiaReport(sample).orElseThrow(
                    () -> new AssertionError("the sample was not deconvoluted as DIA"));
            System.out.print(report.describe());
            System.out.printf("acquired MS/MS scans %d, pseudo spectra written %d with %d peaks (%.1f per spectrum)%n",
                    realMs2, pseudoSpectra, pseudoPeaks, pseudoSpectra == 0 ? 0d : pseudoPeaks / (double) pseudoSpectra);
            System.out.printf("MS1 traces carrying MS/MS: %d of %d (%.1f %%)%n", tracesWithMsMs, ms1Traces,
                    ms1Traces == 0 ? 0d : 100d * tracesWithMsMs / ms1Traces);

            assertTrue(report.spectraWritten() > 0, "spectra must have been written");
            assertEquals(pseudoSpectra, report.spectraWritten(),
                    "the report has to agree with what is in the spectrum store");

            // the acquisition scheme must be unaffected by the spectra we wrote ourselves: every channel of a
            // regular scheme fires the same number of times, and a pseudo spectrum folding back into its channel
            // would show up as a channel with more scans than the instrument acquired
            assertEquals(pseudoSpectra, index.getNumberOfDeconvolutedScans(),
                    "every computed spectrum must be ignored when reconstructing the scheme");
            int totalChannelScans = 0;
            for (DiaChannel channel : index.getChannels()) totalChannelScans += channel.numberOfScans();
            assertTrue(totalChannelScans <= realMs2,
                    "the channels hold " + totalChannelScans + " scans but only " + realMs2 + " were acquired");

            assertTrue(ms1Traces > 0, "the sample must yield MS1 traces");
            assertTrue(pseudoSpectra > 0, "the deconvolution must produce spectra on a real DIA file");
            assertTrue(tracesWithMsMs > 0, "and they must be reachable from their precursor traces");
            // every pseudo spectrum has to look like an MS2 spectrum to everything downstream
            for (Ms2SpectrumHeader header : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
                if (!DiaPseudoSpectra.isDeconvoluted(header)) continue;
                assertEquals(2, header.getMsLevel());
                assertTrue(header.getPrecursorMz() > 0, "a pseudo spectrum needs a precursor mass");
                assertTrue(header.getParentId() >= 0 && header.getParentId() < sample.getMapping().length(),
                        "and a parent scan inside the sample");
                assertFalse(sample.getStorage().getSpectrumStorage().getMs2Spectrum(header.getUid()).isEmpty(),
                        "and at least one peak");
            }
        } finally {
            processing.closeStorages();
        }
    }
}
