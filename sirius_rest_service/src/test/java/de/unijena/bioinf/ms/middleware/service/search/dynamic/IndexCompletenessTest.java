package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.persistence.model.core.PersistentSearchIndex;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-5 index completeness:
 * <ul>
 *   <li>M5 — an index that is not marked complete (e.g. an interrupted (re)build) must not be persisted on
 *       close, so it is rebuilt on the next open instead of being served as complete.</li>
 *   <li>M4 — a failed write marks the index incomplete, so the drift is not persisted and triggers a rebuild.</li>
 * </ul>
 */
public class IndexCompletenessTest {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "name", fullTextSearch = true, defaultSearchField = true)
        public String name;
    }

    private static NoSQLProjectImpl mockProject(String id) {
        NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
        Mockito.when(project.getProjectId()).thenReturn(id);
        return project;
    }

    @Test
    public void completeIndexIsPersisted() throws Exception {
        Path dbFile = Files.createTempFile("m5-complete", ".db");
        try {
            Metadata md = MsProjectDocumentDatabase.buildMetadata();
            try (NitriteDatabase warm = new NitriteDatabase(dbFile, md)) {
                warm.flush();
            }
            NitriteDatabase db = new NitriteDatabase(dbFile, md);
            NoSQLProjectImpl project = mockProject("m5");
            SearchService service = new SearchServiceImpl(p -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap()));
            service.openOrCreateProjectIndex(project);
            service.addDocument("m5", new TestPojo("1", "alpha"));
            service.close(); // complete by default -> persisted
            db.close();

            try (NitriteDatabase db2 = new NitriteDatabase(dbFile, md)) {
                assertTrue(db2.getByPrimaryKey("TestPojo", PersistentSearchIndex.class).isPresent(),
                        "a complete index must be persisted on close");
            }
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void incompleteIndexIsNotPersisted() throws Exception {
        Path dbFile = Files.createTempFile("m5-incomplete", ".db");
        try {
            Metadata md = MsProjectDocumentDatabase.buildMetadata();
            try (NitriteDatabase warm = new NitriteDatabase(dbFile, md)) {
                warm.flush();
            }
            NitriteDatabase db = new NitriteDatabase(dbFile, md);
            NoSQLProjectImpl project = mockProject("m5");
            SearchService service = new SearchServiceImpl(p -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap()));
            service.openOrCreateProjectIndex(project);
            service.addDocument("m5", new TestPojo("1", "alpha"));
            // Simulate an interrupted (re)build / dirtied index.
            service.setIndexComplete("m5", TestPojo.class, false);
            service.close();
            db.close();

            try (NitriteDatabase db2 = new NitriteDatabase(dbFile, md)) {
                assertFalse(db2.getByPrimaryKey("TestPojo", PersistentSearchIndex.class).isPresent(),
                        "an incomplete index must not be persisted on close (M5)");
            }
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void failedWriteMarksIndexIncomplete() throws Exception {
        SinglePojoLuceneIndexManager<TestPojo> mgr = new SinglePojoLuceneIndexManager<>(
                new ByteBuffersDirectory(), TestPojo.class, null, name -> ValueType.TEXT);
        assertTrue(mgr.isComplete(), "a fresh index is complete by default");

        mgr.addDocument(new TestPojo("1", "alpha"));
        assertTrue(mgr.isComplete(), "a successful write keeps the index complete");

        // Close the underlying writer, then a write must fail and mark the index incomplete.
        mgr.close();
        assertThrows(Exception.class, () -> mgr.addDocument(new TestPojo("2", "beta")));
        assertFalse(mgr.isComplete(), "a failed write must mark the index incomplete (M4)");
    }
}
