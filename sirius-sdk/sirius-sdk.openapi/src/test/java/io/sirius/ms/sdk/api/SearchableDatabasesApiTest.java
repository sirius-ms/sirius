package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.SearchableDatabase;
import io.sirius.ms.sdk.model.SearchableDatabaseParameters;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SearchableDatabasesApiTest {

    private SearchableDatabasesApi instance;

    @BeforeEach
    public void setUp() {
        TestSetup.getInstance().loginIfNeeded();
        instance = TestSetup.getInstance().getSiriusClient().databases();
        boolean customDbIsLoaded = instance.getCustomDatabases(false, false).stream()
                .anyMatch(db -> db.getDatabaseId().equals("customdbid_db"));
        if (!customDbIsLoaded) {
            instance.addDatabases(List.of(TestSetup.getInstance().getSearchableCustomDB().toString()));
        }
    }

    @Test
    public void instanceTest() {
        assertNotNull(instance);
    }

    @Test
    public void createDatabaseTest() {
        String databaseId = UUID.randomUUID().toString();
        Path tempDir = TestSetup.getInstance().getTempDir().resolve(databaseId);
        String displayName = "testDB";
        SearchableDatabaseParameters params = new SearchableDatabaseParameters()
                .displayName(displayName).location(tempDir.toAbsolutePath().toString());

        try {
            SearchableDatabase response = instance.createDatabase(databaseId, params);
            assertNotNull(response);
            assertEquals(displayName, response.getDisplayName());

            SearchableDatabase retrievedDb = instance.getDatabase(databaseId, null);
            assertEquals(displayName, retrievedDb.getDisplayName());

            instance.removeDatabase(databaseId, true);
        } finally {
            TestSetup.getInstance().deleteTestSearchableDatabase(databaseId, tempDir.toString());
        }
    }

    @Test
    public void getDatabasesTest() {
        var response = instance.getDatabases(null, null);
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    public void startDatabaseImportTest() {
        String databaseId = UUID.randomUUID().toString();
        Path tempDir = TestSetup.getInstance().getTempDir().resolve(databaseId);

        try {
            String displayName = "testDB";
            SearchableDatabaseParameters params = new SearchableDatabaseParameters()
                    .displayName(displayName).location(tempDir.toAbsolutePath().toString());
            instance.createDatabase(databaseId, params);

            List<File> inputFiles = List.of(
                    TestSetup.getInstance().getDbImportStructures().toFile(),
                    TestSetup.getInstance().getDbImportSpectrum().toFile());

                var db = instance.importIntoDatabaseExperimental(databaseId, null, inputFiles);
                assertEquals(22, db.getNumberOfStructures());
                assertEquals(1, db.getNumberOfReferenceSpectra());

            instance.removeDatabase(databaseId, true);
        } finally {
            TestSetup.getInstance().deleteTestSearchableDatabase(databaseId, tempDir.toString());
        }
    }
}
