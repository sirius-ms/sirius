package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.NoSqlProjectSearchContextProvider;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoDatabaseSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.storage.db.nosql.Database;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-0 harness T0.1: exercises the on-disk {@code FSDirectory} index path and the real
 * {@code NoSqlProjectSearchContextProvider} wiring — both of which have zero coverage in the existing
 * suite (every existing test uses an in-memory {@code ByteBuffersDirectory} and a hand-built lambda provider).
 * <p>
 * These tests are expected to PASS on the current branch; they establish the production-path safety net
 * that later phases (esp. Phase 5 persistence work) rely on.
 */
public class OnDiskAndProviderTest {

    private static final String PROJECT_ID = "ondisk-project";
    private static final String SYSTEM_UID = "ondisk-system-uid";

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "name", fullTextSearch = true, defaultSearchField = true)
        public String name;
    }

    private Project<?> mockBaseProject() {
        Project<?> p = Mockito.mock(Project.class);
        Mockito.when(p.getProjectId()).thenReturn(PROJECT_ID);
        Mockito.when(p.getSystemUID()).thenReturn(SYSTEM_UID);
        Mockito.when(p.findTags()).thenReturn(Collections.emptyList());
        return p;
    }

    @Test
    public void testOnDiskFsDirectoryCreatedAndSearchable() throws IOException {
        Path indexRoot = Files.createTempDirectory("sirius-ondisk-index");
        try {
            Project<?> project = mockBaseProject();
            SearchService service = new SearchServiceImpl(p -> new PerPojoSearchContext(indexRoot, new HashMap<>()));
            service.openOrCreateProjectIndex(project);

            service.addDocument(PROJECT_ID, new TestPojo("1", "glucose on disk"));

            Page<TestPojo> hits = service.search(PROJECT_ID, "glucose", Pageable.unpaged(), TestPojo.class);
            assertEquals(1, hits.getTotalElements(), "on-disk index must be searchable");

            // The FSDirectory must have materialised real files under <indexRoot>/<pojoSimpleName>.
            Path pojoDir = indexRoot.resolve(TestPojo.class.getSimpleName());
            assertTrue(Files.isDirectory(pojoDir), "on-disk index directory must exist for the POJO type");
            try (Stream<Path> files = Files.list(pojoDir)) {
                assertTrue(files.findAny().isPresent(), "on-disk index directory must contain Lucene segment files");
            }

            service.closeProjectIndex(project, false); // keep files on disk
        } finally {
            deleteRecursively(indexRoot);
        }
    }

    @Test
    public void testOnDiskIndexPersistsAcrossReopen() throws IOException {
        Path indexRoot = Files.createTempDirectory("sirius-ondisk-reopen");
        try {
            // First session: add and close (keeping files).
            Project<?> project1 = mockBaseProject();
            SearchService service1 = new SearchServiceImpl(p -> new PerPojoSearchContext(indexRoot, new HashMap<>()));
            service1.openOrCreateProjectIndex(project1);
            service1.addDocument(PROJECT_ID, new TestPojo("1", "persisted glucose"));
            service1.closeProjectIndex(project1, false);

            // Second session over the same on-disk directory: the document must still be found
            // directly from the FSDirectory, with no DB round-trip involved.
            Project<?> project2 = mockBaseProject();
            SearchService service2 = new SearchServiceImpl(p -> new PerPojoSearchContext(indexRoot, new HashMap<>()));
            service2.openOrCreateProjectIndex(project2);
            Page<TestPojo> hits = service2.search(PROJECT_ID, "glucose", Pageable.unpaged(), TestPojo.class);
            assertEquals(1, hits.getTotalElements(), "on-disk index must survive close/reopen");
            service2.closeProjectIndex(project2, false);
        } finally {
            deleteRecursively(indexRoot);
        }
    }

    @Test
    public void testOnDiskIndexDeletedOnCloseWithDelete() throws IOException {
        Path indexRoot = Files.createTempDirectory("sirius-ondisk-delete");
        Project<?> project = mockBaseProject();
        SearchService service = new SearchServiceImpl(p -> new PerPojoSearchContext(indexRoot, new HashMap<>()));
        service.openOrCreateProjectIndex(project);
        service.addDocument(PROJECT_ID, new TestPojo("1", "to be deleted"));

        assertTrue(Files.exists(indexRoot), "index root must exist before delete-close");
        service.closeProjectIndex(project, true); // delete=true
        assertFalse(Files.exists(indexRoot), "close(delete=true) must remove the on-disk index root");
    }

    /**
     * The real production provider must create a per-project index directory under
     * {@code indexingHome/<systemUID>} and clean the whole indexing home up on {@code destroy()}.
     */
    @Test
    public void testProviderCreatesIndexDirAndCleansUp() throws IOException {
        Path indexingHome = Files.createTempDirectory("sirius-provider-home");
        try {
            NoSqlProjectSearchContextProvider provider =
                    new NoSqlProjectSearchContextProvider(false, indexingHome, true);

            NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
            Mockito.when(project.getSystemUID()).thenReturn(SYSTEM_UID);
            @SuppressWarnings("rawtypes")
            SiriusProjectDocumentDatabase projDb = Mockito.mock(SiriusProjectDocumentDatabase.class);
            Mockito.doReturn(projDb).when(project).project();
            Mockito.doReturn(Stream.empty()).when(projDb).findAllTagDefinitionsStr();
            Mockito.doReturn(Mockito.mock(Database.class)).when(project).storage();

            SearchContext ctx = provider.create(project);
            assertNotNull(ctx, "provider must create a search context");
            assertTrue(ctx instanceof PerPojoDatabaseSearchContext, "provider must create a DB-backed context");

            Path expectedProjectIndexDir = indexingHome.resolve(SYSTEM_UID);
            assertTrue(Files.isDirectory(expectedProjectIndexDir),
                    "provider must create the per-project index dir under indexingHome/<systemUID>");

            provider.destroy();
            assertFalse(Files.exists(indexingHome), "destroy() must recursively remove the indexing home");
        } finally {
            deleteRecursively(indexingHome);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root))
            return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
