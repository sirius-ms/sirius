package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.ms.middleware.model.compute.JobProgress;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
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
        protected Project<ProjectSpaceManager> createProject(String projectId, ProjectSpaceManager managerToWrap,
                                                             JobProgressEventListener onProgress) {
            reportedProgress = onProgress;
            return createProject(projectId, managerToWrap);
        }

        volatile JobProgressEventListener reportedProgress;

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
    public void buildRunsOffTheWriteLockAndProjectIsInvisibleUntilReady() throws Exception {
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
    public void reservationIsReleasedWhenBuildFails() throws Exception {
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
    public void concurrentOpensOfSameIdGetDistinctIds() throws Exception {
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
    public void openingAnAlreadyOpenIdIsRenamed() throws Exception {
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

    /**
     * Opening in the background returns before the opening has happened.
     * <p>
     * That is the whole point: converting an old project takes minutes, and a request that only answers once it
     * is finished is one a client cannot show anything for.
     */
    @Test
    public void testOpeningAsAJobReturnsBeforeTheProjectIsOpen() throws Exception {
        TestProvider provider = newProvider();
        CountDownLatch buildReached = new CountDownLatch(1);
        CountDownLatch letBuildFinish = new CountDownLatch(1);
        provider.onBuild = id -> {
            buildReached.countDown();
            try {
                assertTrue(letBuildFinish.await(20, TimeUnit.SECONDS), "build was not released");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Job job = provider.openProjectAsJob("slow",
                freshPath("slow.sirius"), EnumSet.of(Job.OptField.progress));

        assertTrue(buildReached.await(20, TimeUnit.SECONDS), "the opening never started");
        assertEquals("slow", job.getId(), "the job is named for the project it is opening");
        assertTrue(provider.getProject("slow").isEmpty(),
                "the project must not be usable while it is still being opened");
        assertTrue(provider.findOpenJob("slow", EnumSet.of(
                Job.OptField.progress)).isPresent(),
                "and the job doing it must be findable while it runs");

        letBuildFinish.countDown();
        awaitOpen(provider, "slow");
        assertTrue(provider.getProject("slow").isPresent(), "the project is usable once the job is done");
    }

    /** A second open of the same project is refused while the first is still running. */
    @Test
    public void testTheIdIsReservedBeforeTheJobReturns() throws Exception {
        TestProvider provider = newProvider();
        CountDownLatch letBuildFinish = new CountDownLatch(1);
        provider.onBuild = id -> {
            try {
                assertTrue(letBuildFinish.await(20, TimeUnit.SECONDS), "build was not released");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        provider.openProjectAsJob("taken", freshPath("taken.sirius"),
                EnumSet.of(Job.OptField.progress));
        Job second = provider.openProjectAsJob("taken",
                freshPath("taken2.sirius"), EnumSet.of(Job.OptField.progress));

        assertNotEquals("taken", second.getId(),
                "the id was already reserved by the job still opening, so the second gets its own");

        letBuildFinish.countDown();
        awaitOpen(provider, "taken");
    }

    /** How the opening ended is still answerable after it ended, for whoever missed the event. */
    @Test
    public void testTheJobIsStillFindableAfterTheProjectIsOpen() throws Exception {
        TestProvider provider = newProvider();
        provider.openProjectAsJob("done", freshPath("done.sirius"),
                EnumSet.of(Job.OptField.progress));
        awaitOpen(provider, "done");

        Job finished = provider.findOpenJob("done",
                EnumSet.of(Job.OptField.progress)).orElseThrow();
        assertEquals(JJob.JobState.DONE, finished.getProgress().getState());
    }

    /**
     * The progress a conversion reports has to come out of the job as a moving fraction.
     * <p>
     * What a client draws is (progress - min) / (max - min), so a report that carries the counts but leaves the
     * range wrong renders as a bar that never moves while the message underneath it changes - which looks
     * exactly like a conversion that has hung.
     */
    @Test
    public void testConversionProgressIsReportedAsAMovingFraction() throws Exception {
        TestProvider provider = newProvider();
        CountDownLatch letBuildFinish = new CountDownLatch(1);
        CountDownLatch progressForwarded = new CountDownLatch(1);
        provider.onBuild = id -> {
            try {
                // what the conversion of an old project reports on its way through
                provider.reportedProgress.progressChanged(
                        new JobProgressEvent(this, 0, 14312, 8412, "Updating project to the current SIRIUS format (features)"));
                progressForwarded.countDown();
                assertTrue(letBuildFinish.await(20, TimeUnit.SECONDS), "build was not released");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        provider.openProjectAsJob("progressing", freshPath("progressing.sirius"),
                EnumSet.of(Job.OptField.progress));
        assertTrue(progressForwarded.await(20, TimeUnit.SECONDS), "the conversion never reported anything");

        JobProgress reported = provider.findOpenJob("progressing", EnumSet.of(Job.OptField.progress))
                .orElseThrow().getProgress();
        assertEquals(8412L, reported.getCurrentProgress(), "how far the conversion has come");
        assertEquals(14312L, reported.getMaxProgress(), "and how far there is to go");
        assertFalse(reported.isIndeterminate(), "a counted conversion is not an indeterminate one");
        assertTrue(reported.getMessage().contains("Updating project"),
                "and it says a conversion is what is taking the time: " + reported.getMessage());

        letBuildFinish.countDown();
        awaitOpen(provider, "progressing");
    }

    /**
     * A preparation step that cannot count (the index build) reports itself as an indeterminate event with a
     * message, and it has to come out of the job that way: forwarding it as "0 of 100" would draw an empty bar
     * that looks like a conversion that just lost all its progress.
     */
    @Test
    public void testAnIndeterminateStepStaysIndeterminateAndKeepsItsMessage() throws Exception {
        TestProvider provider = newProvider();
        CountDownLatch letBuildFinish = new CountDownLatch(1);
        CountDownLatch progressForwarded = new CountDownLatch(1);
        provider.onBuild = id -> {
            try {
                provider.reportedProgress.progressChanged(new JobProgressEvent(this, "Building search index"));
                progressForwarded.countDown();
                assertTrue(letBuildFinish.await(20, TimeUnit.SECONDS), "build was not released");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        provider.openProjectAsJob("indet", freshPath("indet.sirius"), EnumSet.of(Job.OptField.progress));
        assertTrue(progressForwarded.await(20, TimeUnit.SECONDS), "the build never reported anything");

        JobProgress reported = provider.findOpenJob("indet", EnumSet.of(Job.OptField.progress))
                .orElseThrow().getProgress();
        assertTrue(reported.isIndeterminate(), "a step that cannot count must not pretend to");
        assertEquals("Building search index", reported.getMessage(), "but it still says what it is doing");

        letBuildFinish.countDown();
        awaitOpen(provider, "indet");
    }

    /**
     * Before the conversion starts counting, the opening is doing things that cannot be counted - resolving the
     * location, opening the storage, opening the search index. Reporting those as "0 of 1" draws a bar frozen at
     * zero for however long they take; they have to be reported as indeterminate, so the bar shows activity
     * until the first real count arrives.
     */
    @Test
    public void testTheStepsBeforeTheConversionAreIndeterminateNotZeroPercent() throws Exception {
        TestProvider provider = newProvider();
        CountDownLatch buildReached = new CountDownLatch(1);
        CountDownLatch letBuildFinish = new CountDownLatch(1);
        provider.onBuild = id -> {
            buildReached.countDown();
            try {
                assertTrue(letBuildFinish.await(20, TimeUnit.SECONDS), "build was not released");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        provider.openProjectAsJob("early", freshPath("early.sirius"), EnumSet.of(Job.OptField.progress));
        assertTrue(buildReached.await(20, TimeUnit.SECONDS), "the opening never started");

        JobProgress reported = provider.findOpenJob("early", EnumSet.of(Job.OptField.progress))
                .orElseThrow().getProgress();
        assertTrue(reported.isIndeterminate(),
                "steps that cannot be counted must not be drawn as a bar frozen at zero");
        assertNotNull(reported.getMessage(), "but they still say what is happening");

        letBuildFinish.countDown();
        awaitOpen(provider, "early");
    }

    /**
     * A failed open has to end as a failure a poller can read - and it must not be announced as an opened
     * project. PROJECT_OPENED on a project that never opened sends every listening client looking for a
     * project that is not there.
     */
    @Test
    public void testAFailedOpenSaysSoAndDoesNotAnnounceAnOpenedProject() throws Exception {
        ProjectSpaceManager psm = Mockito.mock(ProjectSpaceManager.class);
        Mockito.when(psm.getLocation()).thenReturn("loc");
        Mockito.when(psm.getType()).thenReturn(Optional.empty());
        EventService<?> events = Mockito.mock(EventService.class);
        TestProvider provider = new TestProvider(loc -> psm, events, Mockito.mock(ComputeService.class));
        provider.onBuild = id -> {
            throw new RuntimeException("simulated build failure");
        };

        provider.openProjectAsJob("broken", freshPath("broken.sirius"), EnumSet.of(Job.OptField.progress));

        JobProgress reported = awaitTerminal(provider, "broken");
        assertEquals(JJob.JobState.FAILED, reported.getState(), "a failed open must say it failed");
        assertNotNull(reported.getErrorMessage(), "and why");

        Mockito.verify(events, Mockito.never()).sendEvent(Mockito.argThat(evt ->
                evt.getEventType() == de.unijena.bioinf.ms.middleware.model.events.ServerEvent.Type.PROJECT));
    }

    private static JobProgress awaitTerminal(TestProvider provider, String projectId) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            JobProgress progress = provider.findOpenJob(projectId, EnumSet.of(Job.OptField.progress))
                    .orElseThrow().getProgress();
            if (progress.getState().ordinal() > JJob.JobState.RUNNING.ordinal())
                return progress;
            Thread.sleep(20);
        }
        fail("the open job for '" + projectId + "' never finished");
        return null;
    }

    private static void awaitOpen(TestProvider provider, String projectId) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            if (provider.getProject(projectId).isPresent())
                return;
            Thread.sleep(20);
        }
        fail("project '" + projectId + "' was never opened");
    }
}
