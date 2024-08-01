package de.unijena.bioinf.babelms.txt;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ParserTestUtils;
import de.unijena.bioinf.babelms.annotations.CompoundMetaData;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class TxtExperimentParserTest {

    @Test
    public void testParseValidFile() throws IOException {
        File input = ParserTestUtils.getTestFile("massbank/MSBNK-UFZ-UP000040.txt");
        URI uri = input.toURI();
        TxtExperimentParser parser = new TxtExperimentParser();
        try (FileInputStream stream = new FileInputStream(input)) {
            Ms2Experiment firstCall = parser.parse(stream, uri);
            assertNotNull(firstCall);
            assertEquals(2, firstCall.getMs2Spectra().get(0).size());

            Ms2Experiment secondCall = parser.parse(stream, uri);
            assertNull(secondCall);
        }
    }

    @Test
    public void testParseInvalidFile() throws IOException {
        File input = ParserTestUtils.getTestFile("massbank/invalid.txt");
        URI uri = input.toURI();
        TxtExperimentParser parser = new TxtExperimentParser();
        try (FileInputStream stream = new FileInputStream(input)) {
            assertThrows(RuntimeException.class, () -> parser.parse(stream, uri));
        }
    }

    @Test
    public void testParseEmptyFile() throws IOException {
        File input = ParserTestUtils.getTestFile("massbank/empty.txt");
        URI uri = input.toURI();
        TxtExperimentParser parser = new TxtExperimentParser();
        try (FileInputStream stream = new FileInputStream(input)) {
            assertNull(parser.parse(stream, uri));
        }
    }

    @Test
    public void testMultipleRecordsInFile() throws IOException {
        File input = ParserTestUtils.getTestFile("massbank/multirecord.txt");
        URI uri = input.toURI();
        TxtExperimentParser parser = new TxtExperimentParser();
        try (FileInputStream stream = new FileInputStream(input)) {
            Ms2Experiment firstCall = parser.parse(stream, uri);
            assertEquals("MSBNK-HBM4EU-HB002845", firstCall.getAnnotation(CompoundMetaData.class).orElseThrow().getCompoundId());

            Ms2Experiment secondCall = parser.parse(stream, uri);
            assertEquals("MSBNK-HBM4EU-HB001222", secondCall.getAnnotation(CompoundMetaData.class).orElseThrow().getCompoundId());
        }
    }
}

