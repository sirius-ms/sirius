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
package de.unijena.bioinf.babelms.msp;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SpectrumWithAdditionalFields;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import static de.unijena.bioinf.babelms.ParserTestUtils.loadExperiment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MSPExperimentParserTest {
    @Test
    void parseMspComments() throws IOException {
        Ms2Experiment experiment = loadExperiment("mona/MoNA010759.msp");
        assertEquals("c1cc(cc(c1)N)C(=O)O", experiment.getAnnotation(Smiles.class).orElseThrow().smiles);
    }

    /**
     * NIST writes synonyms as {@code Synon: $:NN<value>} - a truncated one carries no information but must not
     * cost the record.
     */
    @Test
    void shortNistSynonymDoesNotDropRecord() throws IOException {
        Ms2Experiment experiment = loadExperiment("msp/short_nist_synonym.msp");
        assertEquals("Quercetin", experiment.getName());
        assertEquals(1, experiment.getMs2Spectra().size());
    }

    private static final String QUERCETIN_SMILES = "O=C1C(O)=C(Oc2cc(O)cc(O)c12)c1ccc(O)c(O)c1";
    private static final String CAFFEINE_SMILES = "CN1C=NC2=C1C(=O)N(C(=O)N2C)C";

    private static final String QUERCETIN = """
            Name: Quercetin
            Precursor_type: [M+H]+
            Spectrum_type: MS2
            PrecursorMZ: 303.0499
            SMILES: O=C1C(O)=C(Oc2cc(O)cc(O)c12)c1ccc(O)c(O)c1
            Num Peaks: 3
            137.0233 9120
            153.0182 41003
            303.0499 87550

            """;

    private static final String CAFFEINE = """
            Name: Caffeine
            Precursor_type: [M+H]+
            Spectrum_type: MS2
            PrecursorMZ: 195.0877
            SMILES: CN1C=NC2=C1C(=O)N(C(=O)N2C)C
            Num Peaks: 3
            110.0713 3100
            138.0662 88200
            195.0877 41500

            """;

    private static final String BROKEN_HEADER = """
            Name: Broken
            Precursor_type: [M+H]+
            Spectrum_type: MS2
            PrecursorMZ: 287.0550
            SMILES: O=C1C=C(Oc2cc(O)cc(O)c12)c1ccc(O)c(O)c1
            """;

    /**
     * The peak list is the measurement - if it cannot be read as declared, the record is dropped.
     */
    static Stream<Arguments> brokenRecords() {
        return Stream.of(
                Arguments.of("Num Peaks too high", BROKEN_HEADER + """
                        Num Peaks: 5
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("Num Peaks too low", BROKEN_HEADER + """
                        Num Peaks: 2
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("Num Peaks not a number", BROKEN_HEADER + """
                        Num Peaks: 3a
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("malformed peak line", BROKEN_HEADER + """
                        Num Peaks: 3
                        153.0182 30120
                        165.0546
                        287.0550 64100

                        """),
                Arguments.of("peaks written as mz:intensity", BROKEN_HEADER + """
                        Num Peaks: 3
                        153.0182:30120
                        165.0546:8712
                        287.0550:64100

                        """),
                Arguments.of("Num Peaks implausibly high", BROKEN_HEADER + """
                        Num Peaks: 1500000000
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("multiply charged", BROKEN_HEADER.replace("[M+H]+", "[M+2H]2+") + """
                        Num Peaks: 3
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("peak with thousands separators", BROKEN_HEADER + """
                        Num Peaks: 3
                        153.0182 30,120,000
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("precursor m/z with two separators", BROKEN_HEADER.replace("PrecursorMZ: 287.0550", "PrecursorMZ: 287.0550,288.0583") + """
                        Num Peaks: 3
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("malformed peak line with ':' in its annotation", BROKEN_HEADER + """
                        Num Peaks: 3
                        153.0182 30120
                        165.0546 ? "y2:frag"
                        287.0550 64100

                        """),
                Arguments.of("unparsable adduct and no ion mode", BROKEN_HEADER.replace("[M+H]+", "ESI") + """
                        Num Peaks: 3
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("no adduct and an ion mode that is no charge", BROKEN_HEADER.replace("Precursor_type: [M+H]+\n", "Ion_mode: X\n") + """
                        Num Peaks: 3
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("a peak that is not a number", BROKEN_HEADER + """
                        Num Peaks: 3
                        153.0182 30120
                        +NaN 8712
                        287.0550 64100

                        """),
                Arguments.of("comment with an empty PEAKID", BROKEN_HEADER + """
                        Comments: PEAKID=
                        Num Peaks: 5
                        153.0182 30120

                        """),
                Arguments.of("only the neutral exact mass and no known adduct", """
                        Name: Broken
                        Precursor_type: ESI
                        Ion_mode: P
                        Spectrum_type: MS2
                        ExactMass: 286.0477
                        Num Peaks: 3
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """),
                Arguments.of("no precursor m/z", """
                        Name: Broken
                        Precursor_type: [M+H]+
                        Spectrum_type: MS2
                        Num Peaks: 3
                        153.0182 30120
                        165.0546 8712
                        287.0550 64100

                        """)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenRecords")
    void brokenRecordOnlyDropsItself(String problem, String broken) throws IOException {
        assertIntact(parse(QUERCETIN + broken + CAFFEINE), "Quercetin", "Caffeine");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenRecords")
    void brokenLastRecordOnlyDropsItself(String problem, String broken) throws IOException {
        assertIntact(parse(QUERCETIN + CAFFEINE + broken), "Quercetin", "Caffeine");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenRecords")
    void brokenFirstRecordOnlyDropsItself(String problem, String broken) throws IOException {
        assertIntact(parse(broken + QUERCETIN + CAFFEINE), "Quercetin", "Caffeine");
    }

    /**
     * Peak lines without a preceding 'Num Peaks' belong to the record whose header was read before them - also if
     * that header only contains lines that are not stored (empty values, skipped lines) - not to the record before.
     * The record after is lost as well, see {@link #numPeaksMissingDropsItsRecordAndTheNextOne()}.
     */
    @Test
    void peaksWithoutNumPeaksAfterUnstoredHeaderLinesKeepThePreviousRecord() throws IOException {
        assertIntact(parse(QUERCETIN + """
                Name:
                some junk
                153.0182 30120
                165.0546 8712

                """ + CAFFEINE), "Quercetin");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("msLevels")
    void msLevelIsParsedFromSpectrumType(String spectrumType, int msLevel) throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Spectrum_type: MS2", "Spectrum_type: " + spectrumType));
        assertEquals(msLevel, experiments.get(0).getMs2Spectra().get(0).getMsLevel());
    }

    static Stream<Arguments> msLevels() {
        return Stream.of(Arguments.of("MS2", 2), Arguments.of("ms2", 2), Arguments.of("MS/MS", 2), Arguments.of("MSMS", 2), Arguments.of("MS3", 3),
                Arguments.of("MS2 (CID)", 2), Arguments.of("MS/MS CID", 2),
                Arguments.of("ESI-MS2", 2), Arguments.of("LC-MS2", 2), Arguments.of("MS2HCD", 2), Arguments.of("LC-MS/MS", 2),
                Arguments.of("MS^2", 2), Arguments.of("MS 2", 2), Arguments.of("MS-2", 2), Arguments.of("MS-MS", 2),
                Arguments.of("MS3 (MS/MS/MS)", 3));
    }

    /**
     * SIRIUS only needs to know whether a spectrum is a fragmentation spectrum. A spectrum type without a usable level
     * is treated like a missing one: with a precursor m/z it is a fragmentation spectrum (level 2), else MS1.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("unknownSpectrumTypes")
    void unknownSpectrumTypeIsInferredFromPrecursor(String spectrumType) throws IOException {
        List<Ms2Experiment> withPrecursor = parse(QUERCETIN.replace("Spectrum_type: MS2", "Spectrum_type: " + spectrumType) + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(withPrecursor));
        assertEquals(2, withPrecursor.get(0).getMs2Spectra().get(0).getMsLevel());
        assertIntact(withPrecursor.subList(1, 2), "Caffeine");

        List<Ms2Experiment> withoutPrecursor = parse(QUERCETIN.replace("Spectrum_type: MS2", "Spectrum_type: " + spectrumType).replace("PrecursorMZ: 303.0499\n", ""));
        assertEquals(1, withoutPrecursor.get(0).getMs1Spectra().size());
        assertTrue(withoutPrecursor.get(0).getMs2Spectra().isEmpty());
    }

    static Stream<String> unknownSpectrumTypes() {
        return Stream.of("MSn", "Centroid", "MS99999999999", "GC-MS 70 eV");
    }

    @Test
    void spectrumTypeOneIsMs1() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Spectrum_type: MS2", "Spectrum_type: 1"));
        assertEquals(1, experiments.get(0).getMs1Spectra().size());
        assertTrue(experiments.get(0).getMs2Spectra().isEmpty());
    }

    @Test
    void emptyPeakListKeepsRecordAndAlignment() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN + """
                Name: Empty
                Precursor_type: [M+H]+
                Spectrum_type: MS2
                PrecursorMZ: 287.0550
                Num Peaks: 0

                """ + CAFFEINE);
        assertEquals(List.of("Quercetin", "Empty", "Caffeine"), names(experiments));
        assertEquals(0, experiments.get(1).getMs2Spectra().get(0).size());
        assertIntact(List.of(experiments.get(0), experiments.get(2)), "Quercetin", "Caffeine");
    }

    /**
     * The parser keeps intensities as written - filtering zero or negative intensities is up to preprocessing.
     */
    @Test
    void zeroAndNegativeIntensitiesAreKeptAsWritten() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("137.0233 9120", "137.0233 0").replace("153.0182 41003", "153.0182 -41003") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertEquals(0, experiments.get(0).getMs2Spectra().get(0).getIntensityAt(0), 0);
        assertEquals(-41003, experiments.get(0).getMs2Spectra().get(0).getIntensityAt(1), 0);
        assertIntact(experiments.subList(1, 2), "Caffeine");
    }

    /**
     * A wrapped line that starts with a time or a ratio is no peak data - its record is kept.
     */
    @Test
    void wrappedLineStartingWithTimeOrRatioKeepsRecord() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Precursor_type:", """
                Synon: quercetin
                1:1 complex
                Comments: "acquired 2020-01-01
                10:30:00 on QTOF"
                Precursor_type:""") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertEquals(3, experiments.get(0).getMs2Spectra().get(0).size());
        assertIntact(experiments.subList(1, 2), "Caffeine");
    }

    /**
     * After a broken record, the first line of the next record is read ahead and put back - whatever its length.
     */
    @Test
    void longFirstLineAfterBrokenRecordIsKept() throws IOException {
        String longComment = "Comments: \"" + "x".repeat(1 << 21) + "\"\n";
        List<Ms2Experiment> experiments = parse(QUERCETIN + BROKEN_HEADER + """
                Num Peaks: 5
                153.0182 30120

                """ + longComment + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertEquals(3, experiments.get(1).getMs2Spectra().get(0).size());
    }

    private static final String NUM_PEAKS_MISSING = BROKEN_HEADER + """
            153.0182 30120
            165.0546 8712
            287.0550 64100

            """;

    /**
     * Peak lines in a header cannot be told apart from a wrapped value starting with numbers, so the record is read to
     * the end of its peak list and dropped. If 'Num Peaks' is really missing, that end is the one of the next record -
     * which is then lost as well, but never read wrongly.
     */
    @Test
    void numPeaksMissingDropsItsRecordAndTheNextOne() throws IOException {
        assertIntact(parse(QUERCETIN + NUM_PEAKS_MISSING + CAFFEINE), "Quercetin");
        assertIntact(parse(NUM_PEAKS_MISSING + QUERCETIN + CAFFEINE), "Caffeine");
        assertIntact(parse(QUERCETIN + CAFFEINE + NUM_PEAKS_MISSING), "Quercetin", "Caffeine");
    }

    /**
     * A wrapped value that starts with two numbers looks like a peak line - the record is dropped as a whole, the rest
     * of its header must not come back as a record of its own.
     */
    @Test
    void wrappedNumericHeaderLineDropsOnlyItsRecord() throws IOException {
        assertIntact(parse(QUERCETIN + """
                Name: Broken
                Synon: something
                1.5 2 abc
                Precursor_type: [M+H]+
                Spectrum_type: MS2
                PrecursorMZ: 287.0550
                Num Peaks: 3
                153.0182 30120
                165.0546 8712
                287.0550 64100

                """ + CAFFEINE), "Quercetin", "Caffeine");
    }

    /**
     * An adduct that cannot be parsed is unknown - the charge must still be known from the ion mode.
     */
    @Test
    void unparsableAdductFallsBackToIonMode() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("[M+H]+", "ESI\nIon_mode: P") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertEquals(PrecursorIonType.unknownPositive(), experiments.get(0).getPrecursorIonType());
        assertEquals(1, experiments.get(0).getMs2Spectra().get(0).getIonization().getCharge());
        assertIntact(experiments.subList(1, 2), "Caffeine");
    }

    /**
     * NIST records may carry the adduct as synonym $:03 as well - it is used before falling back to the ion mode.
     */
    @Test
    void unparsableAdductFallsBackToNistSynonym() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("[M+H]+", "ESI\nSynon: $:03[M+Na]+") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertEquals(PrecursorIonType.fromString("[M+Na]+"), experiments.get(0).getPrecursorIonType());
        assertIntact(experiments.subList(1, 2), "Caffeine");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("negativeIonModes")
    void negativeIonModeIsNegative(String ionMode) throws IOException {
        for (String adduct : new String[]{"ESI\n", ""}) { // unparsable adduct, and no adduct at all
            List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Precursor_type: [M+H]+\n", adduct.isEmpty() ? "" : "Precursor_type: " + adduct)
                    .replace("Spectrum_type:", "Ion_mode: " + ionMode + "\nSpectrum_type:"));
            assertEquals(PrecursorIonType.unknownNegative(), experiments.get(0).getPrecursorIonType(), ionMode + " / " + adduct);
            assertEquals(-1, experiments.get(0).getMs2Spectra().get(0).getIonization().getCharge());
        }
    }

    static Stream<String> negativeIonModes() {
        return Stream.of("N", "Negative", "NEGATIVE", "-", "-1");
    }

    /**
     * 'Num Peaks' is recognised like any other key - also indented or with blanks before the ':'.
     */
    @Test
    void numPeaksWithBlanksIsRecognised() throws IOException {
        assertIntact(parse(QUERCETIN.replace("Num Peaks: 3", " Num Peaks: 3") + CAFFEINE.replace("Num Peaks: 3", "Num Peaks : 3")), "Quercetin", "Caffeine");
    }

    @Test
    void commentWithEmptyPeakIdKeepsRecord() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Precursor_type:", "Comments: PEAKID=\nPrecursor_type:") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertIntact(experiments.subList(1, 2), "Caffeine");
    }

    /**
     * ExactMass is the neutral mass - without a PrecursorMZ, the precursor m/z is computed from it and the adduct.
     */
    @Test
    void precursorMzIsComputedFromExactMassAndAdduct() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("PrecursorMZ: 303.0499", "ExactMass: 302.042653") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        final double expected = PrecursorIonType.fromString("[M+H]+").neutralMassToPrecursorMass(302.042653);
        assertEquals(expected, experiments.get(0).getIonMass(), 1e-9);
        assertEquals(expected, experiments.get(0).getMs2Spectra().get(0).getPrecursorMz(), 1e-9);
        assertIntact(experiments.subList(1, 2), "Caffeine");
    }

    /**
     * The fallback to ExactMass must not hide why the adduct cannot be used - an unsupported ion type is reported as such.
     */
    @Test
    void exactMassWithUnsupportedAdductReportsTheAdduct() {
        assertThrows(IllegalArgumentException.class, () -> MSP.parsePrecursorMZ(Map.of("ExactMass", "302.042653", "Precursor_type", "[M+2H]2+")));
    }

    /**
     * A precursor m/z of 0 is no precursor m/z - the exact mass is used instead.
     */
    @Test
    void zeroPrecursorMzFallsBackToExactMass() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("PrecursorMZ: 303.0499", "PrecursorMZ: 0\nExactMass: 302.042653"));
        assertEquals(PrecursorIonType.fromString("[M+H]+").neutralMassToPrecursorMass(302.042653), experiments.get(0).getIonMass(), 1e-9);
    }

    /**
     * Without a spectrum type, a precursor m/z of 0 is none - the spectrum is MS1, not a broken MS2 spectrum.
     */
    @Test
    void zeroPrecursorMzWithoutSpectrumTypeIsMs1() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Spectrum_type: MS2\n", "").replace("PrecursorMZ: 303.0499", "PrecursorMZ: 0") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertEquals(1, experiments.get(0).getMs1Spectra().size());
        assertIntact(experiments.subList(1, 2), "Caffeine");
    }

    /**
     * Without a spectrum type, a record with a precursor field is MS2 if its precursor m/z can be determined in any way -
     * here from the exact mass and the adduct, as the given value is 0.
     */
    @Test
    void unusablePrecursorMzWithExactMassWithoutSpectrumTypeIsMs2() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Spectrum_type: MS2\n", "").replace("PrecursorMZ: 303.0499", "PrecursorMZ: 0\nExactMass: 302.042653"));
        assertTrue(experiments.get(0).getMs1Spectra().isEmpty());
        assertEquals(1, experiments.get(0).getMs2Spectra().size());
        assertEquals(PrecursorIonType.fromString("[M+H]+").neutralMassToPrecursorMass(302.042653), experiments.get(0).getMs2Spectra().get(0).getPrecursorMz(), 1e-9);
    }

    /**
     * An unparsable PrecursorMZ must not hide a valid precursor m/z given as NIST synonym $:04.
     */
    @Test
    void unparsablePrecursorMzFallsBackToNistSynonym() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("PrecursorMZ: 303.0499", "PrecursorMZ: n/a\nSynon: $:04303.0499"));
        assertEquals(303.0499, experiments.get(0).getIonMass(), 1e-9);
        assertEquals(303.0499, experiments.get(0).getMs2Spectra().get(0).getPrecursorMz(), 1e-9);
    }

    /**
     * For intrinsically charged compounds, ExactMass is the mass of the charged formula - the m/z only lacks the
     * electron, no proton is added.
     */
    @Test
    void exactMassOfIntrinsicallyChargedCompoundIsTheIonMass() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("[M+H]+", "[M]+").replace("PrecursorMZ: 303.0499", "ExactMass: 224.12867"));
        assertEquals(224.12867 - Charge.ELECTRON_MASS, experiments.get(0).getIonMass(), 1e-9);
        assertEquals(224.12867 - Charge.ELECTRON_MASS, experiments.get(0).getMs2Spectra().get(0).getPrecursorMz(), 1e-9);
    }

    /**
     * Comments of a record are joined with a separator that contains regex characters - splitting them again must not
     * leave parts of the separator behind (they became the name of records without one).
     */
    @Test
    void nameFromCommentsOfRecordWithoutName() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Name: Quercetin\n", "Comments: acquired on QTOF\nComments: second comment here\n"));
        assertEquals(List.of("acquired on QTOF"), names(experiments));
    }

    /**
     * A record-level warning is reported once per record, not once for each spectrum of an MS-DIAL .mat record.
     */
    @Test
    void precursorWarningOncePerMatRecord() throws IOException {
        List<String> warnings = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.WARNING && String.valueOf(record.getMessage()).contains("computed from the exact mass"))
                    warnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        root.addHandler(handler);
        try {
            // one MS1 and two MS2 spectra, all with the record's unusable precursor m/z
            List<Ms2Experiment> experiments = parse(matRecord("A", "0", "3").replace("PRECURSORTYPE:", "ExactMass: 302.042653\nPRECURSORTYPE:")
                    + "MSTYPE: MS2\nNum Peaks: 1\n153,0182\t41003\n");
            assertEquals(1, experiments.size());
            assertEquals(2, experiments.get(0).getMs2Spectra().size());
            assertEquals(1, warnings.size(), warnings.toString());
        } finally {
            root.removeHandler(handler);
        }
    }

    /**
     * The look-ahead of one reader must not leak into another one parsed with the same parser.
     */
    @Test
    void parserReusedForAnotherReaderStartsFresh() throws IOException {
        MSPExperimentParser parser = new MSPExperimentParser();
        assertEquals("Quercetin", parser.parse(new BufferedReader(new StringReader(QUERCETIN + CAFFEINE)), URI.create("file:/a.msp")).getName());
        // the first reader is abandoned while the parser still holds its second record
        assertEquals("Other", parser.parse(new BufferedReader(new StringReader(CAFFEINE.replace("Caffeine", "Other"))), URI.create("file:/b.msp")).getName());
    }

    /**
     * Optional metadata that cannot be parsed is unknown - it does not cost the record.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"abc", "NaN", "-5", "12.5.3"})
    void unparsableRetentionTimeInCommentsIsUnknown(String retentionTime) throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Precursor_type:", "Comments: \"rtinseconds=" + retentionTime + "\"\nPrecursor_type:") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertTrue(experiments.get(0).getAnnotation(RetentionTime.class).isEmpty());
        assertIntact(experiments.subList(1, 2), "Caffeine");
    }

    @Test
    void retentionTimeInCommentsWithDecimalComma() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Precursor_type:", "Comments: \"rtinseconds=12,5\"\nPrecursor_type:"));
        assertEquals(12.5, experiments.get(0).getAnnotation(RetentionTime.class).orElseThrow().getRetentionTimeInSeconds(), 1e-9);
    }

    /**
     * A header line without ':' is skipped (and reported) - wrapping a value is not allowed, so the line is not
     * repaired, but the record is kept.
     */
    @Test
    void wrappedSynonymIsSkipped() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("Precursor_type:", """
                Synon: 3,3',4',5,7-pentahydroxy-
                flavone
                Precursor_type:""") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertEquals("3,3',4',5,7-pentahydroxy-", fields(experiments.get(0)).get("Synon"));
        assertIntact(experiments.subList(1, 2), "Caffeine");
    }

    @Test
    void junkLineAtRecordStartIsSkipped() throws IOException {
        assertIntact(parse(QUERCETIN + "some junk\n" + CAFFEINE), "Quercetin", "Caffeine");
    }

    /**
     * A wrapped value is a malformed value like any other - it is kept as written and reported.
     */
    @Test
    void wrappedSmilesIsKeptAsWritten() throws IOException {
        List<Ms2Experiment> experiments = parse(QUERCETIN.replace("c12)c1ccc(O)c(O)c1", "c12)\nc1ccc(O)c(O)c1") + CAFFEINE);
        assertEquals(List.of("Quercetin", "Caffeine"), names(experiments));
        assertEquals("O=C1C(O)=C(Oc2cc(O)cc(O)c12)", experiments.get(0).getAnnotation(Smiles.class).orElseThrow().smiles);
        assertEquals(3, experiments.get(0).getMs2Spectra().get(0).size());
    }

    private static String matRecord(String name, String precursorMz, String ms1NumPeaks) {
        return """
                NAME: %s
                PRECURSORMZ: %s
                PRECURSORTYPE: [M+H]+
                IONMODE: Positive
                MSTYPE: MS1
                Num Peaks: %s
                %s\t100000
                304,0533\t16600
                305,0558\t2900
                MSTYPE: MS2
                Num Peaks: 3
                137,0233\t9120
                153,0182\t41003
                %s\t87550
                """.formatted(name, precursorMz, ms1NumPeaks, precursorMz, precursorMz);
    }

    /**
     * MS-DIAL .mat records hold an MS1 and an MS2 spectrum. If one of them is broken, the whole record goes - its
     * other spectrum must neither survive on its own nor be attached to the record before it.
     */
    @Test
    void brokenMatRecordDropsAllItsSpectra() throws IOException {
        List<Ms2Experiment> experiments = new ArrayList<>();
        for (String ms1NumPeaks : new String[]{"4", "2"}) {
            experiments = parse(matRecord("A", "303,0499", "3") + matRecord("B", "287,055", ms1NumPeaks) + matRecord("C", "195,0877", "3"));
            assertEquals(List.of(303.0499, 195.0877), experiments.stream().map(Ms2Experiment::getIonMass).toList(), "MS1 Num Peaks " + ms1NumPeaks);
            for (Ms2Experiment experiment : experiments) {
                assertEquals(1, experiment.getMs1Spectra().size());
                assertEquals(3, experiment.getMs1Spectra().get(0).size());
                assertEquals(1, experiment.getMs2Spectra().size());
                assertEquals(3, experiment.getMs2Spectra().get(0).size());
            }
        }
    }

    /**
     * A file that ends inside the MS2 peak list of its last .mat record - the record must not survive with its MS1 only.
     */
    @Test
    void matRecordCutOffInItsLastSpectrumIsDropped() throws IOException {
        String truncated = matRecord("B", "287,055", "3");
        truncated = truncated.substring(0, truncated.lastIndexOf("137,0233"));
        List<Ms2Experiment> experiments = parse(matRecord("A", "303,0499", "3") + truncated);
        assertEquals(List.of(303.0499), experiments.stream().map(Ms2Experiment::getIonMass).toList());
        assertEquals(1, experiments.get(0).getMs2Spectra().size());
    }

    private static void assertIntact(List<Ms2Experiment> experiments, String... expectedNames) throws IOException {
        assertEquals(List.of(expectedNames), names(experiments));
        for (Ms2Experiment experiment : experiments) {
            // no header lines of another record merged in
            Ms2Experiment alone = parse(experiment.getName().equals("Quercetin") ? QUERCETIN : CAFFEINE).get(0);
            assertEquals(fields(alone), fields(experiment), experiment.getName());
            assertTrue(experiment.getMs1Spectra().isEmpty(), experiment.getName());
            assertEquals(1, experiment.getMs2Spectra().size(), experiment.getName());
            assertEquals(3, experiment.getMs2Spectra().get(0).size(), experiment.getName());
            switch (experiment.getName()) {
                case "Quercetin" -> {
                    assertEquals(303.0499, experiment.getIonMass(), 1e-9);
                    assertEquals(QUERCETIN_SMILES, experiment.getAnnotation(Smiles.class).orElseThrow().smiles);
                }
                case "Caffeine" -> {
                    assertEquals(195.0877, experiment.getIonMass(), 1e-9);
                    assertEquals(CAFFEINE_SMILES, experiment.getAnnotation(Smiles.class).orElseThrow().smiles);
                }
                default -> throw new AssertionError("unexpected record " + experiment.getName());
            }
        }
    }

    private static Map<String, String> fields(Ms2Experiment experiment) {
        return new TreeMap<>(((SpectrumWithAdditionalFields<?>) experiment.getMs2Spectra().get(0)).additionalFields());
    }

    private static List<String> names(List<Ms2Experiment> experiments) {
        return experiments.stream().map(Ms2Experiment::getName).toList();
    }

    private static List<Ms2Experiment> parse(String msp) throws IOException {
        List<Ms2Experiment> experiments = new ArrayList<>();
        try (CloseableIterator<Ms2Experiment> it = new MsExperimentParser().getParserByExt("msp")
                .parseIterator(new BufferedReader(new StringReader(msp)), URI.create("file:/test.msp"))) {
            it.forEachRemaining(experiments::add);
        }
        return experiments;
    }
}
