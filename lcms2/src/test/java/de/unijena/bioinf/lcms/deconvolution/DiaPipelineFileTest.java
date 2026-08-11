package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs a whole file through processing, alignment, merging and feature extraction, and reports the features that
 * come out. Nothing after the deconvolution is DIA aware - the pseudo spectra are ordinary MS2 spectra with
 * ordinary trace references - so this is what checks that claim rather than assuming it.
 * <p>
 * The counts it prints say how much MS/MS the run produced, not whether that MS/MS is right;
 * {@link DiaVersusDdaFileTest} is what answers that.
 * <p>
 * Skipped unless a file is given, as {@code SIRIUS_DIA_TEST_FILE}:
 * <pre>
 * SIRIUS_DIA_TEST_FILE=/path/to/file.mzML ./gradlew :lcms2:cleanTest :lcms2:test --tests '*DiaPipelineFileTest*'
 * </pre>
 */
public class DiaPipelineFileTest {

    static Optional<Path> file(String variable) {
        String location = System.getenv(variable);
        if (location == null || location.isBlank()) return Optional.empty();
        Path path = Path.of(location);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    @Test
    public void runTheWholePipeline() throws IOException {
        Optional<Path> file = file("SIRIUS_DIA_TEST_FILE");
        assumeTrue(file.isPresent(), "no test file given, set SIRIUS_DIA_TEST_FILE");

        final PipelineRun run = PipelineRun.run(file.get(), file.get().getFileName().toString(), deconvolution -> {
            final String quantile = System.getenv("SIRIUS_DIA_NOISE_QUANTILE");
            if (quantile != null && !quantile.isBlank()) deconvolution.setNoiseQuantile(Double.parseDouble(quantile));
            final String prior = System.getenv("SIRIUS_DIA_INTENSITY_PRIOR");
            if (prior != null && !prior.isBlank()) deconvolution.setIntensityPrior(Boolean.parseBoolean(prior));
        });

        int withMsMs = 0, msnSpectra = 0, msnPeaks = 0, withIsotopes = 0;
        for (AlignedFeatures feature : run.features) {
            final Optional<MSData> data = feature.getMSData();
            if (data.isEmpty()) continue;
            if (data.get().getIsotopePattern() != null) ++withIsotopes;
            if (data.get().getMsnSpectra() != null && !data.get().getMsnSpectra().isEmpty()) {
                ++withMsMs;
                msnSpectra += data.get().getMsnSpectra().size();
                if (data.get().getMergedMSnSpectrum() != null) {
                    msnPeaks += data.get().getMergedMSnSpectrum().size();
                }
            }
        }

        System.out.printf("=== %s ===%n", run.name);
        System.out.printf("processSample %.1f s, alignment and features %.1f s%n",
                run.processingSeconds, run.alignmentSeconds);
        if (run.diaReport != null) System.out.print(run.diaReport.describe());
        System.out.printf("aligned features %d%n", run.features.size());
        System.out.printf("  with isotope pattern %d, with MS/MS %d (%.1f %%)%n", withIsotopes, withMsMs,
                run.features.isEmpty() ? 0d : 100d * withMsMs / run.features.size());
        System.out.printf("  merged MSn spectra %d, %.1f peaks per feature with MS/MS%n", msnSpectra,
                withMsMs == 0 ? 0d : msnPeaks / (double) withMsMs);

        assertTrue(!run.features.isEmpty(), "the pipeline must produce features");
        assertTrue(withMsMs > 0, "and some of them must carry MS/MS");
        for (AlignedFeatures feature : run.features) {
            assertNotNull(feature.getRetentionTime(), "every feature needs a retention time");
            assertTrue(feature.getAverageMass() > 0, "and a mass");
        }
    }
}
