package de.unijena.bioinf.ms.frontend;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiriusCLIApplicationTest {

    @CommandLine.Command(name = "formulas", mixinStandardHelpOptions = true)
    private static class DummyTool {
    }

    @Test
    void testInvalidCommandLineIsReportedWithoutStackTrace() {
        CommandLine cmd = new CommandLine(new DummyTool());
        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        boolean handled = SiriusCLIApplication.handleUserError(new CommandLine.ParameterException(cmd,
                "--database and --bottom-up-search must not be combined."));

        assertTrue(handled, "Invalid command line input has to be reported as user error.");
        String message = err.toString();
        assertTrue(message.contains("--database and --bottom-up-search must not be combined."), "Unexpected output: " + message);
        assertTrue(message.contains("--help"), "The error should tell the user how to get help: " + message);
        assertFalse(message.contains("\tat "), "The error should not contain a stack trace: " + message);
    }

    @Test
    void testUnexpectedErrorIsNotReportedAsUserError() {
        assertFalse(SiriusCLIApplication.handleUserError(new NullPointerException("Cannot invoke something")));
    }
}
