package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.file;
import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.files;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Judges another tool's DIA deconvolution against the same DDA measurement we judge our own against, by reading the
 * spectra it exported as MGF.
 * <p>
 * The point of going through {@link SpectrumAgreement} rather than writing a second comparison is that the two
 * numbers then differ only in the spectra, never in how they were scored: same reference run, same way of matching a
 * compound, same exclusion of the precursor region, same decoy control. Anything else would compare two measurements
 * of two different things.
 * <p>
 * Both tools must have been given the same input for the comparison to mean anything. That is not automatic here -
 * a tool that cannot read multiplexed DIA scans has to be given a file with those scans removed, and such a file has
 * no MS/MS in the m/z range that was acquired only in them. Point {@code SIRIUS_DIA_TEST_FILE} at the same files the
 * other tool was given, and the run appears in the table next to it.
 * <p>
 * Skipped unless the MGF and a DDA measurement are given:
 * <pre>
 * SIRIUS_MZMINE_MGF=/path/sirius.mgf SIRIUS_DDA_TEST_FILE=/path/DDA_1.mzML \
 *   SIRIUS_DIA_TEST_FILE=/path/fixed/SWATH_1.mzML,... \
 *   ./gradlew :lcms2:cleanTest :lcms2:test --tests '*MzmineVersusDdaFileTest*'
 * </pre>
 */
public class MzmineVersusDdaFileTest {

    @Test
    public void compareAnotherToolAgainstDda() throws IOException {
        final Optional<Path> mgf = file("SIRIUS_MZMINE_MGF");
        final Optional<Path> ddaFile = file("SIRIUS_DDA_TEST_FILE");
        assumeTrue(mgf.isPresent() && ddaFile.isPresent(),
                "no MGF and DDA file given, set SIRIUS_MZMINE_MGF and SIRIUS_DDA_TEST_FILE");
        final double rtTolerance = SpectrumAgreement.DEFAULT_RT_TOLERANCE;

        final SpectrumAgreement.Run reference =
                SpectrumAgreement.Run.of(List.of(ddaFile.get()), "DDA reference", null, false);
        final SpectrumAgreement.Run other = read(mgf.get(), "MZmine");

        // our own runs on the same input, if it was given. More than one configuration can be compared at once,
        // and that is the only way to compare them: each configuration reports a slightly different set of
        // compounds, so scoring each on its own set measures the sets as much as the spectra.
        final List<Path> diaFiles = files("SIRIUS_DIA_TEST_FILE");
        final List<SpectrumAgreement.Run> runs = new ArrayList<>();
        runs.add(other);
        if (!diaFiles.isEmpty()) {
            runs.add(SpectrumAgreement.Run.of(diaFiles, "SIRIUS", processing -> {
            }, true));
            if (Boolean.parseBoolean(System.getenv().getOrDefault("SIRIUS_DIA_COMPARE_SHARES", "false"))) {
                runs.add(SpectrumAgreement.Run.of(diaFiles, "SIRIUS, no split", processing -> {
                    DiaPipelineFileTest.deconvolutionOf(processing).setIntensityShares(false);
                    DiaPipelineFileTest.refinementOf(processing).setIntensityShares(false);
                }, true));
            }
        }

        System.out.printf("%n=== reference: %s, %d compounds with a merged MS/MS ===%n",
                ddaFile.get().getFileName(), reference.spectra.size());

        final List<SpectrumAgreement.Matching> matchings = new ArrayList<>();
        for (SpectrumAgreement.Run run : runs) matchings.add(SpectrumAgreement.match(run, reference, rtTolerance));

        // each run on the compounds it reports itself, which is not a comparison at all: a run that only attempts
        // the compounds it can handle looks better than one that also attempts the rest
        System.out.printf("%n=== each run on its own compounds (not comparable with each other) ===%n");
        System.out.println(SpectrumAgreement.header());
        for (SpectrumAgreement.Matching matching : matchings) {
            System.out.println(SpectrumAgreement.score(matching, reference, matching.compounds()).describe());
        }

        final IntSet all = new IntOpenHashSet();
        IntSet shared = null;
        for (SpectrumAgreement.Matching matching : matchings) {
            all.addAll(matching.compounds());
            if (shared == null) shared = new IntOpenHashSet(matching.compounds());
            else shared.retainAll(matching.compounds());
        }
        System.out.printf("%ncompounds: %d reported by every run, %d by at least one%n", shared.size(), all.size());
        report("every run, on the compounds all of them report", matchings, reference, shared);
        report("every run, on the compounds any of them reports", matchings, reference, all);

        assertTrue(other.spectra.size() > 0, "the MGF must contain MS2 spectra");
        final SpectrumAgreement.Agreement theirs =
                SpectrumAgreement.score(matchings.get(0), reference, matchings.get(0).compounds());
        assertTrue(theirs.matchedFeatures > 0, "and its compounds must match the reference");
        assertTrue(theirs.confirmedRate() > theirs.decoyRate(), "its agreement must beat chance");
    }

    /** one block of the table: every run scored over the same set of reference compounds */
    private static void report(String title, List<SpectrumAgreement.Matching> matchings,
                               SpectrumAgreement.Run reference, IntSet compounds) {
        System.out.printf("%n=== %s ===%n", title);
        System.out.println(SpectrumAgreement.header());
        final List<SpectrumAgreement.Agreement> rows = new ArrayList<>();
        for (SpectrumAgreement.Matching matching : matchings) {
            rows.add(SpectrumAgreement.score(matching, reference, compounds));
        }
        for (SpectrumAgreement.Agreement row : rows) System.out.println(row.describe());
        System.out.println("  corrected for chance:");
        for (SpectrumAgreement.Agreement row : rows) System.out.println(row.describeNet());
    }

    /**
     * Reads the MS2 spectra of an MGF, one per feature.
     * <p>
     * Only {@code MSLEVEL=2} blocks are taken. An export of this kind also carries a correlated pseudo MS1 spectrum
     * per feature, which is an isotope and adduct pattern rather than a fragmentation spectrum and has nothing to do
     * with what is being compared here.
     */
    static SpectrumAgreement.Run read(Path path, String name) throws IOException {
        final List<SpectrumAgreement.FeatureSpectrum> spectra = new ArrayList<>();
        int blocks = 0, ms2Blocks = 0;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            boolean inside = false;
            int msLevel = 0;
            double precursorMz = Double.NaN, retentionTime = Double.NaN;
            SimpleMutableSpectrum peaks = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.equals("BEGIN IONS")) {
                    inside = true;
                    msLevel = 0;
                    precursorMz = Double.NaN;
                    retentionTime = Double.NaN;
                    peaks = new SimpleMutableSpectrum();
                    ++blocks;
                } else if (line.equals("END IONS")) {
                    inside = false;
                    if (msLevel == 2 && Double.isFinite(precursorMz) && Double.isFinite(retentionTime)) {
                        ++ms2Blocks;
                        final SpectrumAgreement.FeatureSpectrum spectrum = SpectrumAgreement.FeatureSpectrum.of(
                                precursorMz, retentionTime, new SimpleSpectrum(peaks));
                        if (spectrum.size() > 0) spectra.add(spectrum);
                    }
                    peaks = null;
                } else if (inside) {
                    final int equals = line.indexOf('=');
                    if (equals > 0 && !Character.isDigit(line.charAt(0))) {
                        final String key = line.substring(0, equals);
                        final String value = line.substring(equals + 1).trim();
                        switch (key) {
                            case "MSLEVEL" -> msLevel = Integer.parseInt(value);
                            // the m/z of the precursor, optionally followed by its intensity
                            case "PEPMASS" -> precursorMz = Double.parseDouble(value.split("\\s+")[0]);
                            case "RTINSECONDS" -> retentionTime = Double.parseDouble(value);
                            default -> {
                            }
                        }
                    } else {
                        final String[] columns = line.split("\\s+");
                        if (columns.length >= 2) {
                            peaks.addPeak(Double.parseDouble(columns[0]), Double.parseDouble(columns[1]));
                        }
                    }
                }
            }
        }
        System.out.printf("%s: %d blocks in %s, %d of them MS2, %d with fragment peaks below the precursor%n",
                name, blocks, path.getFileName(), ms2Blocks, spectra.size());
        return new SpectrumAgreement.Run(name, ms2Blocks, spectra);
    }
}
