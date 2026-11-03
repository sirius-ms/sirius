package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.LCMSProcessing;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.file;
import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.files;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Judges the deconvolution against a DDA measurement of the same sample, because the number of peaks a
 * deconvolution produces says nothing about whether they belong to the precursor they were assigned to. A peak
 * assigned to two precursors instead of one is only more information if the second precursor really produced it,
 * and the cheapest way to find out is to look up whether the DDA spectrum of that precursor contains the peak.
 * <p>
 * Three runs, so the two settings are compared on the same compounds rather than through aggregate counts: the DDA
 * file as reference, and the DIA file with the intensity prior on and off. Features are matched by precursor mass
 * and retention time, and only the overlap is scored - DDA sees fewer compounds, and a compound it never selected
 * for fragmentation cannot say anything about our assignment.
 * <p>
 * Every rate is reported next to a decoy rate, obtained by scoring the same peaks against the DDA spectrum of a
 * different compound of similar precursor mass that elutes far away. A fragment mass matches some peak of some
 * spectrum by chance surprisingly often, and without that control a confirmation rate cannot be read at all.
 * <p>
 * The decisive number is the last block: of the peaks the prior removes, how many were in the DDA spectrum. Those
 * are the ones it got wrong; the removed peaks that are absent from the DDA spectrum, it removed correctly.
 * <p>
 * Skipped unless both files are given:
 * <pre>
 * SIRIUS_DIA_TEST_FILE=/path/SWATH_1.mzML SIRIUS_DDA_TEST_FILE=/path/DDA_1.mzML \
 *   ./gradlew :lcms2:cleanTest :lcms2:test --tests '*DiaVersusDdaFileTest*'
 * </pre>
 */
public class DiaVersusDdaFileTest {







    @Test
    public void compareAgainstDda() throws IOException {
        // the DIA side may name several files, which are then aligned into one experiment: the consensus across
        // replicates is part of what produces the final spectra, so it belongs in what gets judged
        final List<Path> diaFiles = files("SIRIUS_DIA_TEST_FILE");
        final Optional<Path> ddaFile = file("SIRIUS_DDA_TEST_FILE");
        assumeTrue(!diaFiles.isEmpty() && ddaFile.isPresent(),
                "no file pair given, set SIRIUS_DIA_TEST_FILE and SIRIUS_DDA_TEST_FILE");

        final double rtTolerance = doubleFromEnvironment("SIRIUS_MATCH_RT_TOLERANCE", SpectrumAgreement.DEFAULT_RT_TOLERANCE);
        final Double quantile = System.getenv("SIRIUS_DIA_NOISE_QUANTILE") == null ? null
                : Double.parseDouble(System.getenv("SIRIUS_DIA_NOISE_QUANTILE"));

        // only the spectra are kept from each run: three whole runs' features at once do not fit in a test JVM
        final Consumer<LCMSProcessing> settings = processing -> {
            if (quantile != null) DiaPipelineFileTest.deconvolutionOf(processing).setNoiseQuantile(quantile);
            final String keep = System.getenv("SIRIUS_DIA_KEEP_UNREFINED");
            if (keep != null && !keep.isBlank()) {
                DiaPipelineFileTest.refinementOf(processing).setKeepUnrefinedSpectra(Boolean.parseBoolean(keep));
            }
            final String minPoints = System.getenv("SIRIUS_DIA_MIN_POINTS");
            if (minPoints != null && !minPoints.isBlank()) {
                DiaPipelineFileTest.deconvolutionOf(processing).setMinFragmentScanPoints(Integer.parseInt(minPoints));
            }
            final String decoyQuantile = System.getenv("SIRIUS_DIA_DECOY_QUANTILE");
            if (decoyQuantile != null && !decoyQuantile.isBlank()) {
                DiaPipelineFileTest.refinementOf(processing).setDecoyQuantile(Double.parseDouble(decoyQuantile));
            }
        };
        final SpectrumAgreement.Run reference = SpectrumAgreement.Run.of(List.of(ddaFile.get()), "DDA reference", null, false);
        final SpectrumAgreement.Run before = SpectrumAgreement.Run.of(diaFiles, "stage 1", settings, false);
        final SpectrumAgreement.Run after = SpectrumAgreement.Run.of(diaFiles, "stage 1 + 2", settings, true);

        System.out.printf("%n=== reference: %s ===%n", ddaFile.get().getFileName());
        System.out.printf("%d features, %d with a merged MS/MS%n", reference.featureCount, reference.spectra.size());

        final SpectrumAgreement.Agreement first = SpectrumAgreement.score(before, reference, rtTolerance);
        final SpectrumAgreement.Agreement second = SpectrumAgreement.score(after, reference, rtTolerance);
        System.out.printf("%n=== agreement with the reference (retention time tolerance %.0f s) ===%n", rtTolerance);
        System.out.println(SpectrumAgreement.header());
        System.out.println(first.describe());
        System.out.println(second.describe());

        final Changes changes = scoreChanges(before, after, reference, rtTolerance);
        System.out.printf("%n=== what stage two changed (%d compounds present in all three runs) ===%n",
                changes.features);
        System.out.println(changes.describe());

        assertTrue(!reference.spectra.isEmpty(), "the reference run must produce MS/MS");
        assertTrue(first.matchedFeatures > 0 && second.matchedFeatures > 0, "and features must match across runs");
        // if the assignment carried no information, a peak would agree with the reference no more often than with
        // an unrelated spectrum of similar precursor mass
        assertTrue(first.confirmedRate() > first.decoyRate(), "stage 1: agreement must beat chance");
        assertTrue(second.confirmedRate() > second.decoyRate(), "stage 1 + 2: agreement must beat chance");
    }

    private static double doubleFromEnvironment(String variable, double fallback) {
        final String value = System.getenv(variable);
        return value == null || value.isBlank() ? fallback : Double.parseDouble(value);
    }

    /**
     * The comparison aggregate counts cannot make: take the compounds both runs and the reference found, split the
     * peaks into those the second run kept, those it dropped and those it added, and ask the reference about each
     * group separately. Dropped peaks the reference confirms are what the change got wrong; added peaks it confirms
     * are what it got right.
     */
    private static Changes scoreChanges(SpectrumAgreement.Run before, SpectrumAgreement.Run after, SpectrumAgreement.Run reference, double rtTolerance) {
        final Changes result = new Changes();
        for (SpectrumAgreement.FeatureSpectrum ours : before.spectra) {
            final SpectrumAgreement.FeatureSpectrum counterpart = after.bestMatch(ours, rtTolerance);
            if (counterpart == null) continue;
            final SpectrumAgreement.FeatureSpectrum match = reference.bestMatch(ours, rtTolerance);
            if (match == null) continue;
            final SpectrumAgreement.FeatureSpectrum decoy = reference.decoy(ours);
            ++result.features;

            for (int k = 0; k < ours.size(); ++k) {
                add(counterpart.contains(ours.mz[k]) ? result.kept : result.dropped, ours, k, match, decoy);
            }
            for (int k = 0; k < counterpart.size(); ++k) {
                if (!ours.contains(counterpart.mz[k])) add(result.added, counterpart, k, match, decoy);
            }
        }
        return result;
    }

    private static void add(Group group, SpectrumAgreement.FeatureSpectrum spectrum, int peak, SpectrumAgreement.FeatureSpectrum match,
                            SpectrumAgreement.FeatureSpectrum decoy) {
        ++group.peaks;
        group.intensity += spectrum.intensity[peak];
        if (match.contains(spectrum.mz[peak])) {
            ++group.confirmed;
            group.confirmedIntensity += spectrum.intensity[peak];
        }
        if (decoy != null) {
            ++group.decoyPeaks;
            if (decoy.contains(spectrum.mz[peak])) ++group.decoyConfirmed;
        }
    }

    private static final class Group {
        private int peaks, confirmed, decoyPeaks, decoyConfirmed;
        private double intensity, confirmedIntensity;

        String describe(String label) {
            return String.format("%-22s %7d peaks, %5.1f %% in the reference (%5.1f %% by decoy), "
                            + "%5.1f %% of their intensity", label, peaks,
                    peaks == 0 ? 0d : 100 * confirmed / (double) peaks,
                    decoyPeaks == 0 ? 0d : 100 * decoyConfirmed / (double) decoyPeaks,
                    intensity == 0 ? 0d : 100 * confirmedIntensity / intensity);
        }
    }

    private static final class Changes {
        private int features;
        private final Group kept = new Group();
        private final Group dropped = new Group();
        private final Group added = new Group();

        String describe() {
            return kept.describe("kept by stage two") + "\n" + dropped.describe("dropped by stage two") + "\n"
                    + added.describe("added by stage two");
        }
    }
}
