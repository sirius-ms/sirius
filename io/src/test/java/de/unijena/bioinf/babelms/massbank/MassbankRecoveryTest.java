/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */
package de.unijena.bioinf.babelms.massbank;

import de.unijena.bioinf.babelms.annotations.CompoundMetaData;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ParserTestUtils;
import de.unijena.bioinf.babelms.txt.TxtExperimentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A MassBank file may hold several records, each ended by '//'. A record that cannot be read costs only itself - the
 * parser continues after its '//' with the next one.
 */
public class MassbankRecoveryTest {

    private static String record(String accession, UnaryOperator<String> change) throws IOException {
        String text = Files.readString(ParserTestUtils.getTestFile("massbank/MSBNK-UFZ-UP000040.txt").toPath());
        return change.apply(text.replace("MSBNK-UFZ-UP000040", accession));
    }

    static Stream<Arguments> brokenRecords() {
        return Stream.of(
                Arguments.of("unsupported adduct", (UnaryOperator<String>) r -> r.replace("PRECURSOR_TYPE [M+H]+", "PRECURSOR_TYPE [M+2H]2+")),
                Arguments.of("malformed peak line", (UnaryOperator<String>) r -> r.replace("  59.049 2330.9 661", "  59.049 abc 661")),
                Arguments.of("peak with thousands separators", (UnaryOperator<String>) r -> r.replace("  59.049 2330.9 661", "  59.049 1,234,567 661")),
                Arguments.of("PK$NUM_PEAK too high", (UnaryOperator<String>) r -> r.replace("PK$NUM_PEAK: 2", "PK$NUM_PEAK: 3")),
                Arguments.of("PK$NUM_PEAK implausibly high", (UnaryOperator<String>) r -> r.replace("PK$NUM_PEAK: 2", "PK$NUM_PEAK: 2000000000")),
                Arguments.of("PK$NUM_PEAK too low", (UnaryOperator<String>) r -> r.replace("PK$NUM_PEAK: 2", "PK$NUM_PEAK: 1")),
                Arguments.of("PK$NUM_PEAK not a number", (UnaryOperator<String>) r -> r.replace("PK$NUM_PEAK: 2", "PK$NUM_PEAK: x")),
                Arguments.of("no peak list after PK$ANNOTATION", (UnaryOperator<String>) r -> r.substring(0, r.indexOf("PK$NUM_PEAK")) + "//\n"),
                Arguments.of("blank line between PK$ANNOTATION and the record end", (UnaryOperator<String>) r -> r.substring(0, r.indexOf("PK$NUM_PEAK")) + "\n//\n"),
                Arguments.of("indented record end after PK$ANNOTATION", (UnaryOperator<String>) r -> r.substring(0, r.indexOf("PK$NUM_PEAK")) + "  //\n"),
                Arguments.of("no PK$ fields at all", (UnaryOperator<String>) r -> r.substring(0, r.indexOf("PK$SPLASH")) + "//\n"),
                Arguments.of("PK$NUM_PEAK but no PK$PEAK", (UnaryOperator<String>) r -> r.substring(0, r.indexOf("PK$PEAK")) + "//\n"),
                Arguments.of("empty ion mode and no adduct", (UnaryOperator<String>) r -> r
                        .replace("ION_MODE POSITIVE", "ION_MODE").replaceAll("MS\\$FOCUSED_ION: PRECURSOR_TYPE .*\\n", ""))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenRecords")
    void brokenRecordInTheMiddleOnlyDropsItself(String problem, UnaryOperator<String> breakIt) throws IOException {
        assertIntact(parse(record("A", r -> r) + record("B", breakIt) + record("C", r -> r)), "A", "C");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenRecords")
    void brokenFirstRecordOnlyDropsItself(String problem, UnaryOperator<String> breakIt) throws IOException {
        assertIntact(parse(record("B", breakIt) + record("A", r -> r) + record("C", r -> r)), "A", "C");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenRecords")
    void brokenLastRecordOnlyDropsItself(String problem, UnaryOperator<String> breakIt) throws IOException {
        assertIntact(parse(record("A", r -> r) + record("C", r -> r) + record("B", breakIt)), "A", "C");
    }

    /**
     * A precursor m/z of 0 is none - without MS_TYPE, such a record is an MS1 spectrum, not a broken MS2 one.
     */
    @Test
    void zeroPrecursorMzWithoutMsTypeIsMs1() throws IOException {
        List<Ms2Experiment> experiments = parse(record("A", r -> r) + record("B", r -> r
                .replaceAll("AC\\$MASS_SPECTROMETRY: MS_TYPE .*\\n", "").replace("PRECURSOR_M/Z 165.1121", "PRECURSOR_M/Z 0")
                .replaceAll("CH\\$EXACT_MASS: .*\\n", "")) + record("C", r -> r));
        assertEquals(List.of("A", "B", "C"), ids(experiments));
        assertEquals(1, experiments.get(1).getMs1Spectra().size());
        assertTrue(experiments.get(1).getMs2Spectra().isEmpty());
        assertIntact(List.of(experiments.get(0), experiments.get(2)), "A", "C");
    }

    /**
     * The record end '//' is recognised the same way everywhere, also with trailing blanks.
     */
    @Test
    void recordEndWithTrailingBlanks() throws IOException {
        assertIntact(parse(record("A", r -> r.replace("//", "// ")) + record("B", r -> r) + record("C", r -> r)), "A", "B", "C");
    }

    /**
     * A broken record without '//' is no record that can be skipped - the input ends inside it, as for any file that
     * is no MassBank file.
     */
    @Test
    void brokenRecordWithoutEndFailsTheFile() throws IOException {
        String truncated = record("A", r -> r.replace("PK$NUM_PEAK: 2", "PK$NUM_PEAK: 1").replace("//", ""));
        assertThrows(RuntimeException.class, () -> parse(truncated));
    }

    @Test
    void fileEndingAfterPeakAnnotationFailsTheFile() throws IOException {
        String truncated = record("A", r -> r.substring(0, r.indexOf("PK$NUM_PEAK")));
        assertThrows(RuntimeException.class, () -> parse(truncated));
    }

    @Test
    void precursorMzWithDecimalComma() throws IOException {
        List<Ms2Experiment> experiments = parse(record("A", r -> r.replace("PRECURSOR_M/Z 165.1121", "PRECURSOR_M/Z 165,1121")));
        assertEquals(165.1121, experiments.get(0).getMs2Spectra().get(0).getPrecursorMz(), 1e-9);
    }

    /**
     * An empty record ('//' right after '//', e.g. from joining files) is no end of the file.
     */
    @Test
    void emptyRecordIsSkipped() throws IOException {
        assertIntact(parse(record("A", r -> r) + "//\n" + record("C", r -> r)), "A", "C");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("msTypes")
    void msTypeIsParsed(String msType, int msLevel) throws IOException {
        List<Ms2Experiment> experiments = parse(record("A", r -> r.replace("MS_TYPE MS2", msType.isEmpty() ? "MS_TYPE" : "MS_TYPE " + msType)));
        assertEquals(msLevel, experiments.get(0).getMs2Spectra().get(0).getMsLevel());
    }

    static Stream<Arguments> msTypes() {
        // an empty or unknown MS_TYPE is like a missing one: the precursor m/z makes it MS2
        return Stream.of(Arguments.of("MS2", 2), Arguments.of("ms2", 2), Arguments.of("MS/MS", 2), Arguments.of("MS3", 3),
                Arguments.of("", 2), Arguments.of("MSn", 2));
    }

    /**
     * An empty PRECURSOR_M/Z is no precursor field - without MS_TYPE, the record is MS1, as in the MSP parser.
     */
    @Test
    void emptyPrecursorMzWithoutMsTypeIsMs1() throws IOException {
        List<Ms2Experiment> experiments = parse(record("A", r -> r
                .replaceAll("AC\\$MASS_SPECTROMETRY: MS_TYPE .*\\n", "").replace("PRECURSOR_M/Z 165.1121", "PRECURSOR_M/Z")));
        assertEquals(1, experiments.get(0).getMs1Spectra().size());
        assertTrue(experiments.get(0).getMs2Spectra().isEmpty());
    }

    @Test
    void peaksWithDecimalComma() throws IOException {
        List<Ms2Experiment> experiments = parse(record("A", r -> r.replace("  59.049 2330.9 661", "  59,049 2330,9 661")));
        assertEquals(59.049, experiments.get(0).getMs2Spectra().get(0).getMzAt(0), 1e-9);
    }

    private static void assertIntact(List<Ms2Experiment> experiments, String... expectedIds) {
        assertEquals(List.of(expectedIds), ids(experiments));
        for (Ms2Experiment experiment : experiments) {
            assertEquals(1, experiment.getMs2Spectra().size(), experiment.getName());
            assertEquals(2, experiment.getMs2Spectra().get(0).size(), experiment.getName());
            assertEquals(165.1121, experiment.getMs2Spectra().get(0).getPrecursorMz(), 1e-9, experiment.getName());
        }
    }

    private static List<String> ids(List<Ms2Experiment> experiments) {
        return experiments.stream().map(e -> e.getAnnotation(CompoundMetaData.class).orElseThrow().getCompoundId()).toList();
    }

    private static List<Ms2Experiment> parse(String massbank) throws IOException {
        TxtExperimentParser parser = new TxtExperimentParser();
        BufferedReader reader = new BufferedReader(new StringReader(massbank));
        List<Ms2Experiment> experiments = new ArrayList<>();
        Ms2Experiment experiment;
        while ((experiment = parser.parse(reader, URI.create("file:/test.txt"))) != null)
            experiments.add(experiment);
        return experiments;
    }
}
