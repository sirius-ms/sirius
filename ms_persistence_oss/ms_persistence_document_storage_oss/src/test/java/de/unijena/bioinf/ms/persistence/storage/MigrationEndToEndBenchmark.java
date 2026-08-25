package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureSearchResult;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.persistence.storage.ProjectSchemaMigrator.ConversionJob;
import de.unijena.bioinf.ms.persistence.storage.ProjectSchemaMigrator.ConversionJob.Pass;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * What the conversion costs on a real project.
 * <p>
 * Converting writes, so unlike {@link MigrationStrategyBenchmark} no copy can be shared between variants: each
 * one gets its own copy of the pristine project, converts it, and throws it away. Run it explicitly with
 * {@code MIGRATION_SOURCE_PROJECT=<path to an unconverted project>}.
 */
@EnabledIfEnvironmentVariable(named = "MIGRATION_SOURCE_PROJECT", matches = ".+")
public class MigrationEndToEndBenchmark {

    /**
     * Whether to throw the project's pages out of the operating system's cache before converting. Without it a
     * copy made moments ago is still largely in memory, and the conversion is measured against a file it does
     * not have to go to the disk for. Set {@code MIGRATION_COLD=1} to measure it cold.
     */
    private static final boolean COLD = System.getenv("MIGRATION_COLD") != null;

    /**
     * Where the conversion's time actually goes, read off the kernel rather than guessed at.
     * <p>
     * {@code rchar} is what the process asked the kernel for; {@code read_bytes} is what the kernel had to
     * fetch from the block device. The gap between them is the page cache. CPU time against wall time says how
     * many cores were busy - which, set beside the number of workers, is what separates being held up by the
     * disk from being held up by each other.
     */
    private record Io(long rchar, long readBytes, long syscr, long cpuNanos) {

        static Io now() throws Exception {
            long rchar = 0, readBytes = 0, syscr = 0;
            for (String line : Files.readAllLines(Path.of("/proc/self/io"))) {
                if (line.startsWith("rchar:"))
                    rchar = Long.parseLong(line.substring(6).trim());
                else if (line.startsWith("read_bytes:"))
                    readBytes = Long.parseLong(line.substring(11).trim());
                else if (line.startsWith("syscr:"))
                    syscr = Long.parseLong(line.substring(6).trim());
            }
            return new Io(rchar, readBytes, syscr, ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class)
                    .getProcessCpuTime());
        }

        Io minus(Io earlier) {
            return new Io(rchar - earlier.rchar, readBytes - earlier.readBytes, syscr - earlier.syscr,
                    cpuNanos - earlier.cpuNanos);
        }

        void report(String what, long wallMillis) {
            double seconds = wallMillis / 1000.0;
            record(String.format("BENCH| %-40s %7d ms | asked %7.1f MiB (%6.1f MiB/s) | from disk %7.1f MiB "
                            + "(%6.1f MiB/s) | %,10d read syscalls (%,8.0f/s, %5.0f B each) | %4.1f of %d cores%n",
                    what, wallMillis,
                    rchar / 1048576.0, rchar / 1048576.0 / seconds,
                    readBytes / 1048576.0, readBytes / 1048576.0 / seconds,
                    syscr, syscr / seconds, syscr == 0 ? 0 : rchar / (double) syscr,
                    cpuNanos / 1e9 / seconds, Runtime.getRuntime().availableProcessors()));
        }
    }

    /**
     * Watches what the worker threads are actually doing.
     * <p>
     * Cores-busy says how much of the machine is working but not why the rest is not; this says where the
     * threads that are not working are standing. A thread blocked on a monitor names the lock it wants, so a
     * conversion held up by the store serialising its reads looks different from one held up by the disk.
     */
    private static final class Sampler {
        private final Map<String, Integer> whereBlocked = new HashMap<>();
        private final Map<Thread.State, Integer> states = new EnumMap<>(Thread.State.class);
        private final Thread thread;
        private volatile boolean running = true;
        private int samples;

        private Sampler() {
            thread = new Thread(this::sample, "worker-sampler");
            thread.setDaemon(true);
        }

        static Sampler start() {
            Sampler sampler = new Sampler();
            sampler.thread.start();
            return sampler;
        }

        private void sample() {
            ThreadMXBean threads = ManagementFactory.getThreadMXBean();
            while (running) {
                for (ThreadInfo info : threads.dumpAllThreads(false, false)) {
                    if (info == null || !isWorker(info.getThreadName()))
                        continue;
                    samples++;
                    states.merge(info.getThreadState(), 1, Integer::sum);
                    if (info.getThreadState() == Thread.State.RUNNABLE)
                        continue;
                    // where it is standing, named by the first frame that is ours or the store's
                    for (StackTraceElement frame : info.getStackTrace())
                        if (frame.getClassName().contains("nitrite") || frame.getClassName().contains("mvstore")
                                || frame.getClassName().contains("unijena")) {
                            whereBlocked.merge(frame.getClassName() + "." + frame.getMethodName(), 1, Integer::sum);
                            break;
                        }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(5);
                } catch (InterruptedException interrupted) {
                    return;
                }
            }
        }

        /** The pool threads the conversion's sub-jobs run on, plus the thread driving it. */
        private static boolean isWorker(String name) {
            return name.startsWith("ForkJoinPool") || name.startsWith("jjobs") || name.startsWith("Test worker");
        }

        Sampler stop() throws Exception {
            running = false;
            thread.join(1000);
            return this;
        }

        void report() {
            if (samples == 0)
                return;
            System.out.printf("BENCH|   thread states: %s%n", states.entrySet().stream()
                    .sorted(Map.Entry.<Thread.State, Integer>comparingByValue().reversed())
                    .map(e -> String.format("%s %.0f%%", e.getKey(), 100.0 * e.getValue() / samples))
                    .collect(Collectors.joining(", ")));
            whereBlocked.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(6)
                    .forEach(e -> System.out.printf("BENCH|     not running, standing in %-58s %4.1f%%%n",
                            e.getKey(), 100.0 * e.getValue() / samples));
        }
    }

    /**
     * Asks the kernel to forget what it has cached of the file, so the next read really goes to the disk.
     * <p>
     * {@code posix_fadvise(DONTNEED)}, which needs the file written back first - and which quietly does nothing
     * on a memory-backed filesystem, so the copy has to be on a real one for this to mean anything. Verified by
     * the numbers it produces: a run that evicted nothing reports nothing read from the block device.
     */
    private static void dropPageCacheOf(Path file) throws Exception {
        Process python = new ProcessBuilder("python3", "-c",
                "import os,sys\n"
                        + "fd=os.open(sys.argv[1],os.O_RDONLY)\n"
                        + "os.fsync(fd)\n"
                        + "os.posix_fadvise(fd,0,0,os.POSIX_FADV_DONTNEED)\n"
                        + "os.close(fd)", file.toString())
                .redirectErrorStream(true).start();
        if (python.waitFor() != 0)
            System.out.println("BENCH| (could not drop the page cache - numbers below are warm)");
    }

    private interface OnCopy {
        void run(NitriteSirirusProject project, Database<?> storage) throws Exception;
    }

    /**
     * Converts a private copy of the project and reports how long it took, copying excluded. Every variant
     * starts from the same unconverted bytes, so what they cost is comparable.
     */
    private static void onACopy(String what, OnCopy work) throws Exception {
        Path source = Path.of(System.getenv("MIGRATION_SOURCE_PROJECT"));
        // Beside the source rather than in the system temp directory: on this machine /tmp is a tmpfs, so a
        // copy made there lives in memory and the conversion never touches a disk at all. Override with
        // MIGRATION_WORK_DIR.
        Path workDir = Path.of(System.getenv().getOrDefault("MIGRATION_WORK_DIR", source.getParent().toString()));
        Path copy = Files.createTempFile(workDir, "migration-benchmark-", ".sirius");
        try {
            long copied = System.nanoTime();
            Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("BENCH| (copied %d MiB in %d ms, %d GiB free)%n", Files.size(copy) / (1024 * 1024),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - copied), freeGiB(workDir));

            if (COLD)
                dropPageCacheOf(copy);

            Io before = Io.now();
            Sampler sampler = Sampler.start();
            long start = System.nanoTime();
            try (NitriteSirirusProject project = new NitriteSirirusProject(copy)) {
                work.run(project, project.getStorage());
            }
            long wallMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            Io.now().minus(before).report(what, wallMillis);
            sampler.stop().report();
        } finally {
            System.out.printf("BENCH| (%d GiB free before removing the copy)%n", freeGiB(workDir));
            Files.deleteIfExists(copy);
        }
    }

    /**
     * Free space where the copies are made. A copy of a large project starts as a clone sharing the original's
     * blocks and grows apart from it as the conversion writes, so the room it needs is not known up front.
     */
    private static long freeGiB(Path directory) throws Exception {
        return Files.getFileStore(directory).getUsableSpace() / (1024L * 1024 * 1024);
    }

    /**
     * Whether the primary keys can still be read once the indices are dropped.
     * <p>
     * The conversion drops a collection's indices before rewriting it, and reading keys off the index only
     * works while the index is there. {@code disableIndices} says it keeps whatever is declared on the primary
     * key field - this checks that it really does, because if it does not, the keys have to be collected before
     * anything is dropped.
     */
    @Test
    public void benchmarkReadingPrimaryKeysWhileTheIndicesAreDropped() throws Exception {
        onACopy("primary keys with indices dropped", (project, storage) -> {
            NitriteDatabase nitrite = (NitriteDatabase) storage;
            int before = nitrite.primaryKeys(FormulaCandidate.class).size();
            storage.disableIndices(FormulaCandidate.class);
            try {
                int after = nitrite.primaryKeys(FormulaCandidate.class).size();
                System.out.printf("BENCH|   primary keys before dropping: %d, after: %d -> %s%n",
                        before, after, before == after ? "SURVIVES" : "LOST SOME");
            } catch (IllegalArgumentException noIndex) {
                System.out.println("BENCH|   primary keys after dropping: UNAVAILABLE (" + noIndex.getMessage() + ")");
            } finally {
                storage.enableIndices(FormulaCandidate.class);
            }
        });
    }

//    /**
//     * Whether remembering the databases of a candidate is worth its memory.
//     * <p>
//     * Interleaved and repeated, because single runs of this conversion vary by more than a tenth while the
//     * difference being looked for is a few percent - and because consecutive runs drift in one direction, so
//     * running all of one variant and then all of the other measures the drift instead of the variant.
//     */
//    @Test
//    public void benchmarkRememberingCandidates() throws Exception {
//        System.out.println("BENCH| " + Runtime.getRuntime().availableProcessors() + " cores");
//        try {
//            for (int round = 0; round < 3; round++)
//                for (boolean remember : new boolean[]{round % 2 == 0, round % 2 != 0}) {
//                    ProjectSchemaMigrator.ConversionJob.rememberCandidates = remember;
//                    onACopy(String.format("round %d, remembering %s", round + 1, remember ? "on " : "off"),
//                            (project, storage) -> ProjectSchemaMigrator.migrateIfNeeded(project));
//                }
//        } finally {
//            ProjectSchemaMigrator.ConversionJob.rememberCandidates = false;
//        }
//    }

    /**
     * The three ways of getting the databases of a matched candidate, against each other, and what bounding the
     * on-demand one costs.
     * <p>
     * {@code MIGRATION_CACHE} picks one of {@code prefetch|on_demand|none}, {@code MIGRATION_CACHE_LIMIT} the
     * number of candidates the on-demand one may remember (0 for all of them). One variant per run, so a project
     * big enough that a variant takes a quarter of an hour still produces a result.
     */
    @Test
    public void benchmarkTheCandidateCache() throws Exception {
        record("BENCH| " + Runtime.getRuntime().availableProcessors() + " cores, heap ceiling "
                + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MiB");
        String only = System.getenv("MIGRATION_CACHE");
        int limit = Integer.parseInt(System.getenv().getOrDefault("MIGRATION_CACHE_LIMIT",
                String.valueOf(ProjectSchemaMigrator.DEFAULT_CANDIDATE_CACHE_LIMIT)));
        ProjectSchemaMigrator.CandidateCache[] variants = only == null ? ProjectSchemaMigrator.CandidateCache.values()
                : new ProjectSchemaMigrator.CandidateCache[]{ProjectSchemaMigrator.CandidateCache.valueOf(only.toUpperCase())};

        for (ProjectSchemaMigrator.CandidateCache cache : variants)
            onACopy(String.format("cache %-9s limit %-9s", cache,
                            cache == ProjectSchemaMigrator.CandidateCache.ON_DEMAND
                                    ? (limit <= 0 ? "unlimited" : String.valueOf(limit)) : "-"),
                    (project, storage) -> ProjectSchemaMigrator.migrateIfNeeded(project, cache, limit));
    }

    /** The whole conversion, as a user opening an old project would meet it. */
    @Test
    public void benchmarkTheWholeConversion() throws Exception {
        System.out.println("BENCH| " + Runtime.getRuntime().availableProcessors() + " cores");
        onACopy("whole conversion", (project, storage) -> ProjectSchemaMigrator.migrateIfNeeded(project));
    }

    /**
     * How the conversion scales with the number of workers.
     * <p>
     * Every worker takes the next key itself, so this is the number that says whether the work divides - the
     * shape it replaced stalled at about half the machine because it handed out whole features, and one feature
     * in this project carries 17,434 structure matches against a median of 94.
     */
    @Test
    public void benchmarkTheNumberOfWorkers() throws Exception {
        System.out.println("BENCH| " + Runtime.getRuntime().availableProcessors() + " cores");
        try {
            for (int workers : new int[]{1, 4, 16}) {
                ConversionJob.workerCount = workers;
                onACopy(String.format("%2d worker%s", workers, workers == 1 ? " " : "s"),
                        (project, storage) -> ProjectSchemaMigrator.migrateIfNeeded(project));
            }
        } finally {
            ConversionJob.workerCount = 0;
        }
    }

    /** Each pass on its own copy, so what one costs is not hidden behind the other. */
    @Test
    public void benchmarkEachPassOnItsOwn() throws Exception {
        onACopy("features (MS flags, adducts, database links)", (project, storage) -> {
            ConversionJob job = ProjectSchemaMigrator.convert(project, EnumSet.of(Pass.FEATURES));
            System.out.printf("BENCH|   %d features, %d structure-search results%n",
                    job.getFeaturesFilled().get(), job.getStructureResultsFilled().get());
        });
        onACopy("formula candidates (lipid annotations)", (project, storage) -> {
            ConversionJob job = ProjectSchemaMigrator.convert(project, EnumSet.of(Pass.FORMULA_CANDIDATES));
            System.out.printf("BENCH|   %d formula candidates%n", job.getFormulaCandidatesFilled().get());
        });
    }

    /**
     * Opening and closing the copy without converting anything. Every other number here contains this, so it
     * is what has to be subtracted before two of them can be added together.
     */
    @Test
    public void benchmarkOpeningAndClosingAlone() throws Exception {
        onACopy("opening and closing, nothing else", (project, storage) -> {
        });
    }

    /**
     * What the conversion pays for writing without indices: the collections it rewrites are dropped and rebuilt
     * around it, and a rebuild reads every document in the collection back.
     */
    @Test
    public void benchmarkDroppingAndRebuildingIndices() throws Exception {
        onACopy("dropping and rebuilding every index", (project, storage) -> {
            long start = System.nanoTime();
            storage.disableIndices(CsiStructureSearchResult.class);
            storage.disableIndices(FormulaCandidate.class);
            storage.disableIndices(AlignedFeatures.class);
            System.out.printf("BENCH|   dropping alone %d ms%n",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));

            start = System.nanoTime();
            storage.enableIndices(CsiStructureSearchResult.class);
            storage.enableIndices(FormulaCandidate.class);
            storage.enableIndices(AlignedFeatures.class);
            System.out.printf("BENCH|   rebuilding alone %d ms%n",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        });
    }

    /**
     * What writing a batch in one call is worth against writing its records one at a time.
     * <p>
     * {@code upsertAll} is not a bulk store operation - it loops a find-then-replace per object exactly as
     * {@code upsert} does. What it saves is the global write lock, which it takes once for the batch instead of
     * once per record. Whether that is worth batching for is what this measures.
     */
    @Test
    public void benchmarkWritingOneAtATimeAgainstWritingABatch() throws Exception {
        onACopy("writes", (project, storage) -> {
            List<FormulaCandidate> candidates = new ArrayList<>();
            for (FormulaCandidate candidate : storage.findAll(FormulaCandidate.class)) {
                candidates.add(candidate);
                if (candidates.size() >= 2_000)
                    break;
            }
            int half = candidates.size() / 2;
            List<FormulaCandidate> oneAtATime = candidates.subList(0, half);
            List<FormulaCandidate> asABatch = candidates.subList(half, candidates.size());

            long start = System.nanoTime();
            for (FormulaCandidate candidate : oneAtATime)
                storage.upsert(candidate);
            report("upsert, one call per record", oneAtATime.size(), start);

            start = System.nanoTime();
            storage.upsertAll(asABatch);
            report("upsertAll, one call for the batch", asABatch.size(), start);

            // And with the indices dropped, which is how the conversion writes.
            storage.disableIndices(FormulaCandidate.class);
            start = System.nanoTime();
            for (FormulaCandidate candidate : oneAtATime)
                storage.upsert(candidate);
            report("upsert, one call per record, no indices", oneAtATime.size(), start);

            start = System.nanoTime();
            storage.upsertAll(asABatch);
            report("upsertAll, one call for the batch, no indices", asABatch.size(), start);
            storage.enableIndices(FormulaCandidate.class);
        });
    }

    /**
     * Writes a line to the results file as well as to standard output, when one is named.
     * <p>
     * A run of this length can be cut short by a timeout or a stray kill, and what the test printed is only
     * collected when the whole method returns - so a result that is not written as it happens is a result that
     * can be lost after an hour of work.
     */
    private static void record(String line) {
        System.out.println(line);
        String file = System.getenv("MIGRATION_RESULTS_FILE");
        if (file == null)
            return;
        try {
            Files.writeString(Path.of(file), line + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception couldNotRecord) {
            System.out.println("BENCH| (could not append to " + file + ": " + couldNotRecord + ")");
        }
    }

    private static void report(String what, int records, long startNanos) {
        long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNanos);
        System.out.printf("BENCH|   %-48s %7d us  %6d records  %7.1f us/record%n",
                what, micros, records, micros / (double) Math.max(1, records));
    }
}
