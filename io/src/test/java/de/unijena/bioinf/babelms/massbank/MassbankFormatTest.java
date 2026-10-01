package de.unijena.bioinf.babelms.massbank;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ParserTestUtils;
import de.unijena.bioinf.babelms.txt.TxtExperimentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MassbankFormatTest {

    private static final String RETENTION = MassbankFormat.AC_CHROMATOGRAPHY_RETENTION_TIME.k();
    private static final String TITLE = MassbankFormat.RECORD_TITLE.k();
    private static final String PRECURSOR_MZ = MassbankFormat.MS_FOCUSED_ION_PRECURSOR_MZ.k();
    private static final String PRECURSOR_TYPE = MassbankFormat.MS_FOCUSED_ION_PRECURSOR_TYPE.k();
    private static final String EXACT_MASS = MassbankFormat.CH_EXACT_MASS.k();
    private static final String ION_MODE = MassbankFormat.AC_MASS_SPECTROMETRY_ION_MODE.k();

    @ParameterizedTest
    @MethodSource
    public void testRetentionTimeUnitParsing(Map<String, String> metadata, double expectedMiddle) {
        Optional<RetentionTime> retentionTime = MassbankFormat.parseRetentionTime(metadata);
        assertTrue(retentionTime.isPresent());
        assertEquals(expectedMiddle, retentionTime.get().getMiddleTime(), 1e-9);
    }

    private static Stream<Arguments> testRetentionTimeUnitParsing() {
        return Stream.of(
                Arguments.of(Map.of(RETENTION, "1", TITLE, "Chemical; something; RT: 1 s"), 1),
                Arguments.of(Map.of(RETENTION, "1", TITLE, "Chemical; something; RT: 1 min; something"), 60),
                Arguments.of(Map.of(RETENTION, "1", TITLE, "Chemical; something; RT: 2 min; something"), 120),
                Arguments.of(Map.of(RETENTION, "50"), 50),
                Arguments.of(Map.of(RETENTION, "1 some remark"), 60),
                Arguments.of(Map.of(RETENTION, "1 se"), 60),
                Arguments.of(Map.of(TITLE, "Title; RT: 5 s"), 5),
                Arguments.of(Map.of(TITLE, "Title: 2 min; RT: 1; Something: 10 s"), 60)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNoRetentionTime(Map<String, String> metadata) {
        Optional<RetentionTime> retentionTime = MassbankFormat.parseRetentionTime(metadata);
        assertTrue(retentionTime.isEmpty());
    }

    private static Stream<Map<String, String>> testNoRetentionTime() {
        return Stream.of(
                Map.of(),
                Map.of(RETENTION, "invalid")
        );
    }

    @Test
    public void testRetentionTimeRange() {
        RetentionTime retentionTime = MassbankFormat.parseRetentionTime(Map.of(RETENTION, "0.1-0.3 min")).orElseThrow();
        assertEquals(6, retentionTime.getStartTime(), 1e-9);
        assertEquals(18, retentionTime.getEndTime(), 1e-9);
        assertEquals(12, retentionTime.getMiddleTime(), 1e-9);
    }

    /**
     * A list of values or thousands separators is no single m/z - it must not be read as one.
     */
    @Test
    public void precursorMzWithTwoSeparatorsIsUnusable() {
        assertTrue(MassbankFormat.parseGivenPrecursorMz(Map.of(PRECURSOR_MZ, "165.1121,166.1150")).isEmpty());
        assertTrue(MassbankFormat.parseGivenPrecursorMz(Map.of(PRECURSOR_MZ, "165.1121.3")).isEmpty());
        assertEquals(165.1121, MassbankFormat.parseGivenPrecursorMz(Map.of(PRECURSOR_MZ, "165,1121")).orElseThrow(), 1e-9);
    }

    @Test
    public void precursorMzIsTakenAsGiven() {
        assertEquals(165.1121, MassbankFormat.parsePrecursorMZ(Map.of(PRECURSOR_MZ, "165.1121", EXACT_MASS, "164.1049", PRECURSOR_TYPE, "[M+H]+")).orElseThrow(), 1e-9);
    }

    /**
     * CH$EXACT_MASS is the neutral mass - without a precursor m/z, the m/z is computed from it and the adduct.
     */
    @Test
    public void precursorMzIsComputedFromExactMassAndAdduct() {
        double expected = PrecursorIonType.fromString("[M+H]+").neutralMassToPrecursorMass(164.1049);
        assertEquals(expected, MassbankFormat.parsePrecursorMZ(Map.of(EXACT_MASS, "164.1049", PRECURSOR_TYPE, "[M+H]+")).orElseThrow(), 1e-9);
    }

    /**
     * Without a known adduct, the neutral mass gives no m/z.
     */
    @Test
    public void exactMassWithoutKnownAdductGivesNoPrecursorMz() {
        assertTrue(MassbankFormat.parsePrecursorMZ(Map.of(EXACT_MASS, "164.1049", ION_MODE, "POSITIVE")).isEmpty());
        assertTrue(MassbankFormat.parsePrecursorMZ(Map.of(EXACT_MASS, "164.1049")).isEmpty());
    }

    /**
     * For intrinsically charged compounds, CH$EXACT_MASS is the mass of the charged formula (e.g. [C12H18NO3]+) - the
     * m/z only lacks the electron, no proton is added.
     */
    @Test
    public void exactMassOfIntrinsicallyChargedCompoundIsTheIonMass() {
        assertEquals(224.12867 - Charge.ELECTRON_MASS, MassbankFormat.parsePrecursorMZ(Map.of(EXACT_MASS, "224.12867", PRECURSOR_TYPE, "[M]+")).orElseThrow(), 1e-9);
    }

    @Test
    public void exactMassWithUnsupportedAdductReportsTheAdduct() {
        assertThrows(IllegalArgumentException.class, () -> MassbankFormat.parsePrecursorMZ(Map.of(EXACT_MASS, "164.1049", PRECURSOR_TYPE, "[M+2H]2+")));
    }

    @Test
    public void zeroPrecursorMzFallsBackToExactMass() {
        double expected = PrecursorIonType.fromString("[M+H]+").neutralMassToPrecursorMass(164.1049);
        assertEquals(expected, MassbankFormat.parsePrecursorMZ(Map.of(PRECURSOR_MZ, "0", EXACT_MASS, "164.1049", PRECURSOR_TYPE, "[M+H]+")).orElseThrow(), 1e-9);
    }

    @Test
    public void unusablePrecursorMzFallsBackToExactMass() {
        double expected = PrecursorIonType.fromString("[M+H]+").neutralMassToPrecursorMass(164.1049);
        for (String precursorMz : new String[]{"Infinity", "NaN", "/", "-1"})
            assertEquals(expected, MassbankFormat.parsePrecursorMZ(Map.of(PRECURSOR_MZ, precursorMz, EXACT_MASS, "164.1049", PRECURSOR_TYPE, "[M+H]+")).orElseThrow(), 1e-9, precursorMz);
        assertTrue(MassbankFormat.parsePrecursorMZ(Map.of(EXACT_MASS, "NaN", PRECURSOR_TYPE, "[M+H]+")).isEmpty());
    }

    @Test
    public void recordWithoutPrecursorMzGetsItFromExactMass() throws IOException {
        File original = ParserTestUtils.getTestFile("massbank/MSBNK-UFZ-UP000040.txt");
        String withoutPrecursorMz = Files.readAllLines(original.toPath()).stream()
                .filter(l -> !l.startsWith(PRECURSOR_MZ)).collect(Collectors.joining("\n", "", "\n"));
        Ms2Experiment experiment = new TxtExperimentParser().parse(new BufferedReader(new StringReader(withoutPrecursorMz)), original.toURI());
        double expected = PrecursorIonType.fromString("[M+H]+").neutralMassToPrecursorMass(164.1049);
        assertEquals(expected, experiment.getIonMass(), 1e-9);
        assertEquals(expected, experiment.getMs2Spectra().get(0).getPrecursorMz(), 1e-9);
    }
}
