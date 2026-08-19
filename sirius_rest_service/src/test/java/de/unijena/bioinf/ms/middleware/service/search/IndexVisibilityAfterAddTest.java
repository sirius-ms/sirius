package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * When a document added to an existing index can be found.
 * <p>
 * Index writes are near-real-time: they reach the writer, and a query only sees them once the searcher has been
 * refreshed. The read path refreshes best-effort - {@code maybeRefresh()} returns at once if another thread is
 * already refreshing - so a query is allowed to be slightly stale, which is fine for a query and not fine for
 * whoever just wrote and is about to announce it.
 * <p>
 * That is what the peak-list import hit: it adds to the existing index and immediately fires its import event,
 * the GUI answers by reloading its feature list, and two of those reloads race - one refreshes, the other skips
 * and queries the searcher as it was. In a project that was empty, the searcher as it was holds no documents at
 * all, so the reload answers empty and the feature list stays empty until something queries again. The LC/MS
 * import never showed it because it rebuilds the whole index and marking that build complete forces a blocking
 * refresh.
 */
public class IndexVisibilityAfterAddTest {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestFeature {
        @IndexField(documentId = true)
        public String alignedFeatureId;

        @IndexField
        public String name;
    }

    private SearchServiceImpl searchService;
    private Project<?> mockProject;
    private final String projectId = "test-project";

    @BeforeEach
    public void setup() throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(projectId);
        Mockito.when(mockProject.getSystemUID()).thenReturn("test-system-uid");
        searchService = new SearchServiceImpl(project -> new PerPojoSearchContext(null, new HashMap<>()));
        searchService.openOrCreateProjectIndex(mockProject);
        // an empty project: the index exists, and its (empty) build was marked complete
        searchService.setIndexComplete(projectId, TestFeature.class, true);
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(mockProject, true);
    }

    private long searchCount(String query) {
        return searchService.searchIds(projectId, query, PageRequest.of(0, 100), TestFeature.class).getTotalElements();
    }

    /**
     * The easy case, and the reason the gap went unnoticed: with nothing else refreshing, the read path's own
     * best-effort refresh succeeds and the write is found.
     */
    @Test
    public void testAnAddedDocumentIsVisibleToTheNextQuery() {
        searchService.addDocuments(projectId, List.of(new TestFeature("1", "caffeine")));

        assertEquals(1, searchCount("name:caffeine"));
    }

    /**
     * The case an import has to survive: several queries arrive at once, as they do when an import event makes a
     * client reload. Only one of them can hold the refresh lock, so the rest must not be left querying the
     * pre-import searcher - which is what publishing the writes guarantees.
     * <p>
     * Without the {@code makeWritesSearchable} call this fails with the queries that lost the race reporting no
     * hits at all (verified by removing it).
     */
    @Test
    public void testPublishedWritesAreVisibleToEveryConcurrentQuery() throws Exception {
        searchService.addDocuments(projectId, List.of(new TestFeature("1", "caffeine")));
        searchService.makeWritesSearchable(projectId, TestFeature.class);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Long>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++)
                results.add(pool.submit(() -> {
                    start.await();
                    return searchCount("name:caffeine");
                }));
            start.countDown();
            for (Future<Long> hits : results)
                assertEquals(1, hits.get(30, TimeUnit.SECONDS), "a concurrent query must not miss a published write");
        } finally {
            pool.shutdownNow();
        }
    }
}
