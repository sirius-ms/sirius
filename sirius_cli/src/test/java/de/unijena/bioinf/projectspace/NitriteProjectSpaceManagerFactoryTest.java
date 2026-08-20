package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What a project records about its own schema when it is made.
 * <p>
 * A project written by the current version is at the current schema by definition, and saying so is what keeps
 * it from being converted later: the conversion is gated on the recorded version, and a project that records
 * nothing looks indistinguishable from one written years ago. On a large project that mistake costs minutes of
 * rereading data that was already correct.
 */
public class NitriteProjectSpaceManagerFactoryTest {

    /** A project asked for by name, which is how the command line and the GUI make one. */
    @Test
    public void testANewProjectRecordsTheCurrentSchemaVersion() throws IOException {
        Path location = Files.createTempDirectory("sirius-new-project-test").resolve("fresh.sirius");
        NoSQLProjectSpaceManager project = new NitriteProjectSpaceManagerFactory().createOrOpen(location);
        try {
            assertEquals(SiriusProjectDocumentDatabase.CURRENT_PROJECT_SCHEMA_VERSION,
                    project.getProject().findProjectSchemaVersion().orElse(0),
                    "a project just created is at the current schema and has to say so");
        } finally {
            project.close();
            Files.deleteIfExists(location);
        }
    }

    /** A project with no location, which is the temporary one a run gets when no output was asked for. */
    @Test
    public void testANewTemporaryProjectRecordsTheCurrentSchemaVersion() throws IOException {
        NoSQLProjectSpaceManager project = new NitriteProjectSpaceManagerFactory().createOrOpen(null);
        Path location = Path.of(project.getLocation());
        try {
            assertEquals(SiriusProjectDocumentDatabase.CURRENT_PROJECT_SCHEMA_VERSION,
                    project.getProject().findProjectSchemaVersion().orElse(0),
                    "a temporary project is just as new as any other");
        } finally {
            project.close();
            Files.deleteIfExists(location);
        }
    }

    /**
     * Reopening must not change what the project says: the recorded version belongs to the data, so opening an
     * existing project may not stamp it with the version of whatever happens to be opening it.
     */
    @Test
    public void testOpeningAnExistingProjectLeavesItsRecordedVersionAlone() throws IOException {
        Path location = Files.createTempDirectory("sirius-reopen-project-test").resolve("reopened.sirius");
        try {
            NoSQLProjectSpaceManager created = new NitriteProjectSpaceManagerFactory().createOrOpen(location);
            created.getProject().upsertProjectSchemaVersion(1);
            created.close();

            NoSQLProjectSpaceManager reopened = new NitriteProjectSpaceManagerFactory().createOrOpen(location);
            try {
                assertEquals(1, reopened.getProject().findProjectSchemaVersion().orElse(0),
                        "an existing project keeps what it recorded, so it still gets converted");
            } finally {
                reopened.close();
            }
        } finally {
            Files.deleteIfExists(location);
        }
    }
}
