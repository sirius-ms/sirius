package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-4/5 (H11): the (potentially slow) project + index build runs WITHOUT holding the provider write lock,
 * so it never freezes other project operations; a project is only visible/usable once its build finishes
 * ("index ready == project open"); and the id reservation prevents concurrent opens from colliding or leaking.
 */
public class ProjectOpenConcurrencyTest {

    /** Provider whose build step (createProject) runs a configurable hook, simulating the index build. */
    private static class TestProvider extends ProjectSpaceManagerProvider<ProjectSpaceManager, Project<ProjectSpaceManager>> {
        volatile Consumer<String> onBuild = id -> {
        };

        TestProvider(ProjectSpaceManagerFactory<ProjectSpaceManager> factory, EventService<?> es, ComputeService cs) {
            super(factory, es, cs);
        }

        @Override
        protected Project<ProjectSpaceManager> createProject(String projectId, ProjectSpaceManager managerToWrap) {
            onBuild.accept(projectId);
            @SuppressWarnings("unchecked")
            Project<ProjectSpaceManager> project = Mockito.mock(Project.class);
            Mockito.when(project.getProjectSpaceManager()).thenReturn(managerToWrap);
            return project;
        }

        @Override
        protected void validateExistingLocation(java.nio.file.Path location) {
        }

        @Override
        protected void copyProject(String projectId, ProjectSpaceManager psm, java.nio.file.Path copyPath) {
        }

        @Override
        protected void registerEventListeners(String id, ProjectSpaceManager psm) {
        }

        @Override
        public ProjectInfo createTempProject(EnumSet<ProjectInfo.OptField> optFields) {
            throw new UnsupportedOperationException("not needed for this test");
        }
    }

    private static TestProvider newProvider() {
        ProjectSpaceManager psm = Mockito.mock(ProjectSpaceManager.class);
        Mockito.when(psm.getLocation()).thenReturn("loc");
        Mockito.when(psm.getType()).thenReturn(Optional.empty());
        return new TestProvider(loc -> psm, Mockito.mock(EventService.class), Mockito.mock(ComputeService.class));
    }

    /** A path that does not exist, so createProject skips existence validation and goes straight to the build. */
    private static String freshPath(String name) throws IOException {
        return Files.createTempDirectory("h11").resolve(name).toString();
    }

    private static ProjectInfo open(TestProvider provider, String id, String path) {
        return provider.createProject(id, path, EnumSet.noneOf(ProjectInfo.OptField.class), false, false);
    }

    @Test
    public void buildRunsOffTheWriteLockAndProjectIsInvisibleUntilReady_H11() throws Exception {
        TestProvider provider = newProvider();
        CountDownLatch buildStarted = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        provider.onBuild = id -> {
            buildStarted.countDown();
            await(proceed);
        };

        String path = freshPath("p1");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ProjectInfo> opening = pool.submit(() -> open(provider, "p1", path));
            assertTrue(buildStarted.await(5, TimeUnit.SECONDS), "build should have started");

            // Read ops must not be blocked by the write lock while a build is in progress.
            Future<List<ProjectInfo>> listFuture = pool.submit(provider::listAllProjectSpaces);
            List<ProjectInfo> listed = assertDoesNotThrow(() -> listFuture.get(3, TimeUnit.SECONDS),
                    "listAllProjectSpaces must not block while a project is building (H11)");
            assertTrue(listed.isEmpty(), "a still-building project must not be listed (invariant)");
            assertTrue(provider.getProject("p1").isEmpty(), "a still-building project must not be resolvable (invariant)");

            proceed.countDown();
            assertEquals("p1", opening.get(5, TimeUnit.SECONDS).getProjectId());
            assertTrue(provider.getProject("p1").isPresent(), "project must be published once its build is ready");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void reservationIsReleasedWhenBuildFails_H11() throws Exception {
        TestProvider provider = newProvider();

        // First attempt fails during the build.
        provider.onBuild = id -> {
            throw new RuntimeException("simulated build failure");
        };
        assertThrows(RuntimeException.class, () -> open(provider, "p1", freshPath("p1a")),
                "a failing build must propagate");
        assertFalse(provider.getProject("p1").isPresent(), "a failed open must not publish the project");

        // The id must not be stuck reserved: a retry gets the ORIGINAL id (not a renamed 'p1_2').
        provider.onBuild = id -> {
        };
        ProjectInfo retry = open(provider, "p1", freshPath("p1b"));
        assertEquals("p1", retry.getProjectId(),
                "reservation must be released after a failed build so the id is reusable (H11)");
    }

    @Test
    public void concurrentOpensOfSameIdGetDistinctIds_H11() throws Exception {
        TestProvider provider = newProvider();
        provider.onBuild = id -> await(20); // small overlap window

        int n = 5;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            List<Future<ProjectInfo>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final String path = freshPath("shared-" + i);
                futures.add(pool.submit(() -> open(provider, "shared", path)));
            }
            Set<String> ids = new java.util.HashSet<>();
            for (Future<ProjectInfo> f : futures)
                ids.add(f.get(10, TimeUnit.SECONDS).getProjectId());

            assertEquals(n, ids.size(), "concurrent opens of the same id must yield distinct project ids (H11)");
            assertEquals(n, provider.listAllProjectSpaces().size(), "all concurrently opened projects must be published");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void openingAnAlreadyOpenIdIsRenamed_H11() throws Exception {
        TestProvider provider = newProvider();
        assertEquals("p1", open(provider, "p1", freshPath("p1")).getProjectId());
        assertEquals("p1_2", open(provider, "p1", freshPath("p1-again")).getProjectId(),
                "opening an id that is already open must rename to a unique id (H11)");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
