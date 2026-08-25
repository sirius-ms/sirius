package de.unijena.bioinf.ms;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LcmsAlignOptionsTest extends CLITest {

    @Test
    void testMissingInputIsReportedAsParameterError() {
        CommandLine.ParameterException e = assertThrows(CommandLine.ParameterException.class, () ->
                makeWorkflowWithFreshCLI("-p", projectLocation.toString(), "lcms-align"));
        assertTrue(e.getMessage().contains("--input"), "Unexpected error message: " + e.getMessage());
    }

    @Test
    void testInputWithoutLcmsRunsIsReportedAsParameterError() throws IOException {
        CommandLine.ParameterException e = assertThrows(CommandLine.ParameterException.class, () ->
                makeWorkflowWithFreshCLI("-p", projectLocation.toString(),
                        "--input", dummyInputFile(".mgf").toString(), "lcms-align"));
        assertTrue(e.getMessage().contains(".mzml"), "Unexpected error message: " + e.getMessage());
    }

    @Test
    void testLcmsRunInputIsAccepted() throws IOException {
        makeWorkflowWithFreshCLI("-p", projectLocation.toString(),
                "--input", dummyInputFile(".mzml").toString(), "lcms-align");
    }

    /**
     * The file is never read, because the tests only create the workflow and do not compute it.
     */
    private Path dummyInputFile(String extension) throws IOException {
        Path input = Files.createTempFile("sirius-cli-test", extension);
        input.toFile().deleteOnExit();
        return input;
    }
}
