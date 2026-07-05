package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-4 (H10): after making reads lock-free while guarding the non-thread-safe query parser/config maps
 * with a dedicated lock, concurrent searches (which parse queries) and concurrent writes must run without
 * racing, deadlocking, or corrupting the index.
 */
public class IndexConcurrencyTest {

    private static final String PROJECT_ID = "concurrency-project";

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "name", fullTextSearch = true, defaultSearchField = true)
        public String name;
    }

    @Test
    public void concurrentReadsAndWritesAreSafe_H10() throws Exception {
        Project<?> mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(PROJECT_ID);
        Mockito.when(mockProject.getSystemUID()).thenReturn("concurrency-uid");
        Mockito.when(mockProject.findTags()).thenReturn(Collections.emptyList());
        SearchService service = new SearchServiceImpl(project -> new PerPojoSearchContext(null, new HashMap<>()));
        service.openOrCreateProjectIndex(mockProject);

        try {
            final int seed = 100;
            for (int i = 0; i < seed; i++)
                service.addDocument(PROJECT_ID, new TestPojo("s" + i, "seed" + i));

            ExecutorService pool = Executors.newFixedThreadPool(10);
            AtomicReference<Throwable> firstError = new AtomicReference<>();
            List<Future<?>> futures = new ArrayList<>();

            // Reader threads: repeatedly parse + search (exercises the parser under configLock).
            for (int t = 0; t < 6; t++) {
                futures.add(pool.submit(() -> {
                    try {
                        for (int i = 0; i < 300; i++) {
                            service.search(PROJECT_ID, "seed" + (i % seed), Pageable.unpaged(), TestPojo.class);
                            service.search(PROJECT_ID, null, Pageable.unpaged(), TestPojo.class);
                        }
                    } catch (Throwable e) {
                        firstError.compareAndSet(null, e);
                    }
                }));
            }

            // Writer threads: add and delete documents concurrently with the readers.
            for (int t = 0; t < 3; t++) {
                final int base = 1000 + t * 1000;
                futures.add(pool.submit(() -> {
                    try {
                        for (int i = 0; i < 100; i++) {
                            service.addDocument(PROJECT_ID, new TestPojo("w" + (base + i), "written" + i));
                            if ((i & 1) == 0)
                                service.removeDocumentById(PROJECT_ID, "w" + (base + i), TestPojo.class);
                        }
                    } catch (Throwable e) {
                        firstError.compareAndSet(null, e);
                    }
                }));
            }

            for (Future<?> f : futures)
                f.get(60, TimeUnit.SECONDS);
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "thread pool must terminate (no deadlock)");

            if (firstError.get() != null)
                fail("concurrent search/index operations must not throw: " + firstError.get(), firstError.get());

            // All seed docs plus every odd-indexed written doc (even ones were deleted) must be present.
            long expected = seed + 3 * 50;
            assertEquals(expected, service.search(PROJECT_ID, null, Pageable.unpaged(), TestPojo.class).getTotalElements(),
                    "final document count must be consistent after concurrent writes");
        } finally {
            service.closeProjectIndex(mockProject, true);
        }
    }
}
