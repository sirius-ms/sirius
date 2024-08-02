package de.unijena.bioinf.ChemistryBase.chem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static de.unijena.bioinf.ChemistryBase.chem.RetentionTime.RETENTION_TIME_UNIT_GUESS_THRESHOLD;
import static org.junit.jupiter.api.Assertions.*;

public class RetentionTimeTest {

    @ParameterizedTest
    @MethodSource
    public void parseParametersTest(String s, RetentionTime.ParsedParameters expected) {
        assertEquals(expected, RetentionTime.parseRetentionTimeParameters(s));
    }

    static Stream<Arguments> parseParametersTest() {
        return Stream.of(
                Arguments.of("", new RetentionTime.ParsedParameters(null, null, null)),
                Arguments.of("invalid", new RetentionTime.ParsedParameters(null, null, null)),
                Arguments.of("-1", new RetentionTime.ParsedParameters(null, null, null)),
                Arguments.of("1", new RetentionTime.ParsedParameters(1d, null, null)),
                Arguments.of("x1x", new RetentionTime.ParsedParameters(1d, null, null)),
                Arguments.of("5.0", new RetentionTime.ParsedParameters(5d, null, null)),
                Arguments.of("5.5", new RetentionTime.ParsedParameters(5.5d, null, null)),
                Arguments.of("1-2", new RetentionTime.ParsedParameters(1d, 2d, null)),
                Arguments.of("1.5 - 2.5", new RetentionTime.ParsedParameters(1.5d, 2.5d, null)),
                Arguments.of("1s", new RetentionTime.ParsedParameters(1d, null, "s")),
                Arguments.of("1 sec", new RetentionTime.ParsedParameters(1d, null, "s")),
                Arguments.of("1 min", new RetentionTime.ParsedParameters(1d, null, "min")),
                Arguments.of("1 ms", new RetentionTime.ParsedParameters(1d, null, null)),
                Arguments.of("1 minutes", new RetentionTime.ParsedParameters(1d, null, "min"))
        );
    }

    @Test
    public void fromParametersTest() {
        RetentionTime rt;

        assertTrue(RetentionTime.tryParse("").isEmpty());
        rt = RetentionTime.tryParse("1 s").orElseThrow();
        assertEquals(1, rt.getMiddleTime(), 1e-9);
        assertThrows(RuntimeException.class, rt::getStartTime);

        rt = RetentionTime.tryParse("1-2").orElseThrow();
        assertEquals(60, rt.getStartTime(), 1e-9);
        assertEquals(90, rt.getMiddleTime(), 1e-9);
        assertEquals(120, rt.getEndTime(), 1e-9);

        assertEquals(60, RetentionTime.tryParse("1 min").orElseThrow().getMiddleTime(), 1e-9);
        assertEquals(60, RetentionTime.tryParse("1").orElseThrow().getMiddleTime(), 1e-9);
        assertEquals(1000, RetentionTime.tryParse("1000").orElseThrow().getMiddleTime(), 1e-9);
        assertEquals(RETENTION_TIME_UNIT_GUESS_THRESHOLD, RetentionTime.tryParse("" + RETENTION_TIME_UNIT_GUESS_THRESHOLD).orElseThrow().getMiddleTime(), 1e-9);
        assertEquals((RETENTION_TIME_UNIT_GUESS_THRESHOLD - 1) * 60, RetentionTime.tryParse("" + (RETENTION_TIME_UNIT_GUESS_THRESHOLD - 1)).orElseThrow().getMiddleTime(), 1e-9);
    }
}