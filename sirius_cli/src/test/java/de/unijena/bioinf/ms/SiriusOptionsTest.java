package de.unijena.bioinf.ms;

import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiriusOptionsTest extends CLITest {

    /**
     * The formula tool is not only reachable from the root command, but also as a subtool of the config tool
     * (which is what the GUI and the REST API always use) and of the preprocessing tools. Option constraints
     * have to be enforced identically on all of these entry points.
     */
    static Stream<Arguments> formulaToolEntryPoints() {
        return Stream.of(
                Arguments.of("root", List.of()),
                Arguments.of("config", List.of("config", "--AlgorithmProfile=orbitrap")),
                Arguments.of("lcms-align", List.of("--input", dummyLcmsInput(), "lcms-align")),
                Arguments.of("spectra-search", List.of("spectra-search"))
        );
    }

    /**
     * The LC/MS preprocessing tool needs input files to create its job. The file is never read, because the tests below
     * only create the workflow and do not compute it.
     */
    private static String dummyLcmsInput() {
        try {
            Path input = Files.createTempFile("sirius-cli-test", ".mzml");
            input.toFile().deleteOnExit();
            return input.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ParameterizedTest(name = "via {0}")
    @MethodSource("formulaToolEntryPoints")
    void testDatabaseAndBottomUpSearchAreMutuallyExclusive(String entryPoint, List<String> entryPointArgs) {
        CommandLine.ParameterException e = assertThrows(CommandLine.ParameterException.class, () ->
                makeWorkflowWithFreshCLI(formulaCommand(entryPointArgs, "--database", "PUBCHEM", "--bottom-up-search", "BOTTOM_UP_ONLY")));
        assertTrue(e.getMessage().contains("--database") && e.getMessage().contains("--bottom-up-search"),
                "Unexpected error message: " + e.getMessage());
    }

    @ParameterizedTest(name = "via {0}")
    @MethodSource("formulaToolEntryPoints")
    void testDatabaseAloneIsAllowed(String entryPoint, List<String> entryPointArgs) throws IOException {
        makeWorkflowWithFreshCLI(formulaCommand(entryPointArgs, "--database", "PUBCHEM"));
    }

    @ParameterizedTest(name = "via {0}")
    @MethodSource("formulaToolEntryPoints")
    void testBottomUpSearchAloneIsAllowed(String entryPoint, List<String> entryPointArgs) throws IOException {
        makeWorkflowWithFreshCLI(formulaCommand(entryPointArgs, "--bottom-up-search", "BOTTOM_UP_ONLY"));
    }

    @ParameterizedTest(name = "via {0}")
    @MethodSource("formulaToolEntryPoints")
    void testNeitherOptionIsAllowed(String entryPoint, List<String> entryPointArgs) throws IOException {
        makeWorkflowWithFreshCLI(formulaCommand(entryPointArgs));
    }

    /**
     * The --database option performs a database only search, so it has to switch off de novo and bottom-up search.
     */
    @Test
    void testDatabaseOptionDisablesDeNovoAndBottomUpSearch() throws IOException {
        ParameterConfig config = makeWorkflowWithFreshCLI(formulaCommand(List.of(), "--database", "PUBCHEM"));

        assertEquals("PUBCHEM", config.getConfigValue("FormulaSearchDB"));
        assertEquals(0d, Double.parseDouble(config.getConfigValue("FormulaSearchSettings.performDeNovoBelowMz")));
        assertEquals(Double.POSITIVE_INFINITY, Double.parseDouble(config.getConfigValue("FormulaSearchSettings.performBottomUpAboveMz")));
    }

    /**
     * De novo, bottom-up and database search can still be combined, but only via the config tool. Its parameters must
     * not be overwritten by the formula tool.
     */
    @Test
    void testDatabaseConfigParameterKeepsDeNovoAndBottomUpSearch() throws IOException {
        ParameterConfig config = makeWorkflowWithFreshCLI(formulaCommand(List.of("config",
                "--FormulaSearchDB=PUBCHEM",
                "--FormulaSearchSettings.performDeNovoBelowMz=400",
                "--FormulaSearchSettings.performBottomUpAboveMz=0")));

        assertEquals("PUBCHEM", config.getConfigValue("FormulaSearchDB"));
        assertEquals(400d, Double.parseDouble(config.getConfigValue("FormulaSearchSettings.performDeNovoBelowMz")));
        assertEquals(0d, Double.parseDouble(config.getConfigValue("FormulaSearchSettings.performBottomUpAboveMz")));
    }

    /**
     * Database search has to work in a complete run, not just during workflow creation.
     */
    @Test
    void testDatabaseSearchRunsCompleteWorkflow() throws IOException {
        runWithFreshCLI(formulaCommand(List.of(), "--database", "PUBCHEM"));
    }

    private String[] formulaCommand(List<String> entryPointArgs, String... formulaArgs) {
        List<String> args = new ArrayList<>(List.of("-p", projectLocation.toString()));
        args.addAll(entryPointArgs);
        args.add("formula");
        args.addAll(List.of(formulaArgs));
        return args.toArray(String[]::new);
    }
}
