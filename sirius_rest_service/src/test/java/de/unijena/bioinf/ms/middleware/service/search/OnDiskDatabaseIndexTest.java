package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoDatabaseSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-5 (M13): a DB-backed on-disk index must restore from the Nitrite-persisted copy when the project
 * is reopened while stale Lucene files from the previous session are still on disk. Without clearing the
 * directory first, deserialize() collides on createOutput and the index silently falls back to empty.
 */
public class OnDiskDatabaseIndexTest {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "name", fullTextSearch = true, defaultSearchField = true)
        public String name;
    }

    @Test
    public void onDiskDbIndexRestoresAfterReopenWithStaleFiles() throws Exception {
        Path dbFile = Files.createTempFile("m13-nitrite", ".db");
        Path indexDir = Files.createTempDirectory("m13-index");
        try {
            Metadata metadata = MsProjectDocumentDatabase.buildMetadata();
            try (NitriteDatabase warmup = new NitriteDatabase(dbFile, metadata)) {
                warmup.flush();
            }

            NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
            Mockito.when(project.getProjectId()).thenReturn("m13");

            // Session 1: add a doc and close. This serializes the index to the DB and leaves the FSDirectory
            // files on disk (close without delete). Close the DB afterwards to commit and anchor the version,
            // mirroring a real project close/reopen cycle.
            NitriteDatabase db = new NitriteDatabase(dbFile, metadata);
            SearchService s1 = new SearchServiceImpl(proj -> new PerPojoDatabaseSearchContext<>(db, indexDir, Collections.emptyMap()));
            s1.openOrCreateProjectIndex(project);
            s1.addDocument("m13", new TestPojo("1", "glucose"));
            s1.close();
            db.close();

            // Precondition: stale Lucene files remain on disk for the POJO type (they persist across the DB reopen).
            Path pojoDir = indexDir.resolve("TestPojo");
            assertTrue(Files.isDirectory(pojoDir), "on-disk index directory should exist after close");
            try (Stream<Path> files = Files.list(pojoDir)) {
                assertTrue(files.findAny().isPresent(), "stale index files should remain on disk after close");
            }

            // Session 2: reopen a fresh DB handle and the same on-disk directory. The version is in sync, so
            // the index is restored from the DB copy - which must deserialize into a cleared directory instead
            // of colliding with the stale files and silently falling back to empty.
            NitriteDatabase db2 = new NitriteDatabase(dbFile, metadata);
            SearchService s2 = new SearchServiceImpl(proj -> new PerPojoDatabaseSearchContext<>(db2, indexDir, Collections.emptyMap()));
            s2.openOrCreateProjectIndex(project);
            Page<TestPojo> hits = s2.search("m13", "glucose", Pageable.unpaged(), TestPojo.class);
            assertEquals(1, hits.getTotalElements(),
                    "DB-backed on-disk index must restore into a cleared directory on reopen (M13)");

            s2.close();
            db2.close();
        } finally {
            FileUtils.deleteRecursively(indexDir);
            Files.deleteIfExists(dbFile);
        }
    }
}
