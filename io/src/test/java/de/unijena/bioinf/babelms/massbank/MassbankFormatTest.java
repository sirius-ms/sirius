package de.unijena.bioinf.babelms.massbank;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MassbankFormatTest {

    private static final String RETENTION = MassbankFormat.AC_CHROMATOGRAPHY_RETENTION_TIME.k();
    private static final String TITLE = MassbankFormat.RECORD_TITLE.k();
    private static final String PRECURSOR_MZ = MassbankFormat.MS_FOCUSED_ION_PRECURSOR_MZ.k();
    private static final String PRECURSOR_TYPE = MassbankFormat.MS_FOCUSED_ION_PRECURSOR_TYPE.k();
    private static final String EXACT_MASS = MassbankFormat.CH_EXACT_MASS.k();

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

}
