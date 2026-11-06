package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.storage.IncompatibleProjectSchemaException;
import de.unijena.bioinf.ms.persistence.storage.ProjectSchemaVersion;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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
                    project.getProject().findProjectSchemaVersion().orElse(null),
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
                    project.getProject().findProjectSchemaVersion().orElse(null),
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
            created.getProject().upsertProjectSchemaVersion(new ProjectSchemaVersion(0, 1));
            created.close();

            NoSQLProjectSpaceManager reopened = new NitriteProjectSpaceManagerFactory().createOrOpen(location);
            try {
                assertEquals(new ProjectSchemaVersion(0, 1), reopened.getProject().findProjectSchemaVersion().orElse(null),
                        "an existing project keeps what it recorded, so it still gets converted");
            } finally {
                reopened.close();
            }
        } finally {
            Files.deleteIfExists(location);
        }
    }

    /**
     * A project of a newer major schema is refused, and refused before opening writes anything to it - the newer
     * SIRIUS that wrote it must find it exactly as it left it - and closed again, so it can still be opened.
     */
    @Test
    public void testAProjectOfANewerMajorSchemaIsRefusedUntouched() throws IOException {
        Path location = Files.createTempDirectory("sirius-newer-project-test").resolve("newer.sirius");
        ProjectSchemaVersion newerMajor = new ProjectSchemaVersion(
                SiriusProjectDocumentDatabase.CURRENT_PROJECT_SCHEMA_VERSION.major() + 1, 0);
        try {
            try (NitriteSirirusProject written = new NitriteSirirusProject(location)) {
                written.upsertProjectSchemaVersion(newerMajor);
            }

            assertThrows(IncompatibleProjectSchemaException.class,
                    () -> new NitriteProjectSpaceManagerFactory().createOrOpen(location));

            try (NitriteSirirusProject reopened = new NitriteSirirusProject(location)) {
                assertEquals(newerMajor, reopened.findProjectSchemaVersion().orElse(null));
                assertEquals(0, reopened.getStorage().countAll(TagDefinition.class),
                        "opening wrote nothing - not even the default tags it adds to every project it opens");
                assertTrue(reopened.findProjectType().isEmpty());
            }
        } finally {
            Files.deleteIfExists(location);
        }
    }
}
