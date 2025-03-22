package de.unijena.bioinf.ms;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class OutputOptionsTest extends CLITest {

    @Test
    void testDotInFileName() throws IOException {
        ps.close();
        runAssert(expectedInstanceIds());

        projectLocation = FileUtils.createTmpProjectSpaceLocation(".with_dot" + SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(projectLocation);
        ps.close();

        assertThrows(CommandLine.ParameterException.class, () -> runAssert(expectedInstanceIds()));
    }

}
