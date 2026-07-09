package de.unijena.bioinf.ms;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SiriusOptionsTest extends CLITest {

    @Test
    void testDatabaseAndBottomUpSearchAreMutuallyExclusive() {
        assertThrows(CommandLine.ParameterException.class, () -> runWithArguments(
                "-p", projectLocation.toString(),
                "formula", "--database", "PUBCHEM", "--bottom-up-search", "BOTTOM_UP_ONLY"
        ));
    }

    @Test
    void testDatabaseAloneIsAllowed() {
        runWithArguments("-p", projectLocation.toString(), "formula", "--database", "PUBCHEM");
    }

    @Test
    void testBottomUpSearchAloneIsAllowed() {
        runWithArguments("-p", projectLocation.toString(), "formula", "--bottom-up-search", "BOTTOM_UP_ONLY");
    }

    @Test
    void testNeitherOptionIsAllowed() {
        runWithArguments("-p", projectLocation.toString(), "formula");
    }

}
