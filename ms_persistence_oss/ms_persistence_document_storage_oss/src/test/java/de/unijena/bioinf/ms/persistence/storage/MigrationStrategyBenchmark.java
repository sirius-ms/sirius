package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureMatch;
import de.unijena.bioinf.ms.persistence.model.sirius.FTreeResult;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Measures the strategies the conversion could use, against a real project.
 * <p>
 * Not a unit test and not part of the suite: run it explicitly with {@code BENCHMARK_PROJECT=<path>}. It only
 * reads, so it can share one copy of the project between variants - but reading warms the store's page cache,
 * so every variant is given its <b>own slice of keys</b> that no other variant has touched. Measuring them all
 * against the same keys would say more about the order they ran in than about the strategies.
 */
@EnabledIfEnvironmentVariable(named = "BENCHMARK_PROJECT", matches = ".+")
public class MigrationStrategyBenchmark {

    /** Keys per variant. Small enough that a run is quick, large enough to swamp the JIT warming up. */
    private static final int SLICE = 8_000;

    private static Path project() {
        return Path.of(System.getenv("BENCHMARK_PROJECT"));
    }

    private record Timing(String what, long millis, long records) {
        double perRecordMicros() {
            return records == 0 ? 0 : millis * 1000.0 / records;
        }

        void report() {
            System.out.printf("BENCH| %-52s %7d ms  %8d records  %8.1f us/record%n",
                    what, millis, records, perRecordMicros());
        }
    }

    private static Timing time(String what, long records, Callable<?> work) throws Exception {
        long start = System.nanoTime();
        work.call();
        return new Timing(what, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), records);
    }

    /**
     * Distinct candidate keys, in the order the conversion would meet them, so the slices resemble the access
     * pattern rather than a synthetic one.
     */
    private static List<String> candidateKeys(Database<?> storage, int howMany) throws Exception {
        Set<String> keys = new LinkedHashSet<>();
        for (CsiStructureMatch match : storage.findAll(CsiStructureMatch.class)) {
            if (match.getCandidateInChiKey() != null)
                keys.add(match.getCandidateInChiKey());
            if (keys.size() >= howMany)
                break;
        }
        return new ArrayList<>(keys);
    }

    private static List<Long> formulaIds(Database<?> storage, int howMany) throws Exception {
        List<Long> ids = new ArrayList<>(howMany);
        for (FormulaCandidate candidate : storage.findAll(FormulaCandidate.class)) {
            ids.add(candidate.getFormulaId());
            if (ids.size() >= howMany)
                break;
        }
        return ids;
    }

    /** Reads each key on the calling thread. */
    private static long readSequentially(Database<?> storage, List<String> keys) throws Exception {
        long links = 0;
        for (String key : keys) {
            FingerprintCandidate candidate = storage.getByPrimaryKey(key, FingerprintCandidate.class).orElse(null);
            if (candidate != null && candidate.getLinks() != null)
                links += candidate.getLinks().size();
        }
        return links;
    }

    /** Reads the same keys spread over a pool, to see whether reads actually overlap. */
    private static long readInParallel(Database<?> storage, List<String> keys, int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicLong links = new AtomicLong();
        try {
            int chunk = Math.max(1, keys.size() / threads);
            List<Future<?>> running = new ArrayList<>();
            for (int start = 0; start < keys.size(); start += chunk) {
                List<String> slice = keys.subList(start, Math.min(keys.size(), start + chunk));
                running.add(pool.submit(() -> links.addAndGet(readSequentially(storage, slice))));
            }
            for (Future<?> f : running)
                f.get();
        } finally {
            pool.shutdownNow();
        }
        return links.get();
    }

    private static long readTreesSequentially(Database<?> storage, List<Long> ids) throws Exception {
        long trees = 0;
        for (Long id : ids)
            if (storage.getByPrimaryKey(id, FTreeResult.class).map(FTreeResult::getFTree).isPresent())
                trees++;
        return trees;
    }

    private static long readTreesInParallel(Database<?> storage, List<Long> ids, int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicLong trees = new AtomicLong();
        try {
            int chunk = Math.max(1, ids.size() / threads);
            List<Future<?>> running = new ArrayList<>();
            for (int start = 0; start < ids.size(); start += chunk) {
                List<Long> slice = ids.subList(start, Math.min(ids.size(), start + chunk));
                running.add(pool.submit(() -> trees.addAndGet(readTreesSequentially(storage, slice))));
            }
            for (Future<?> f : running)
                f.get();
        } finally {
            pool.shutdownNow();
        }
        return trees.get();
    }

    /**
     * How much a candidate cache could ever save. The conversion looks a candidate up once per match, so what
     * caching is worth is exactly how often the same structure is matched to more than one feature - and what
     * it costs in memory is the number of distinct keys, which is what has to stay bounded.
     */
    @Test
    public void measureHowOftenCandidatesRepeatAcrossFeatures() throws Exception {
        try (NitriteSirirusProject db = new NitriteSirirusProject(project())) {
            Set<String> distinct = new LinkedHashSet<>();
            long matches = 0;
            for (CsiStructureMatch match : db.getStorage().findAll(CsiStructureMatch.class)) {
                matches++;
                if (match.getCandidateInChiKey() != null)
                    distinct.add(match.getCandidateInChiKey());
            }
            System.out.printf("BENCH| matches=%d distinct candidates=%d -> %.2f lookups per distinct key%n",
                    matches, distinct.size(), matches / (double) Math.max(1, distinct.size()));
        }
    }

    /**
     * What asking a collection for its size costs. The conversion used it to size a progress bar; whether that
     * was worth paying for depends on whether the store keeps a count or has to go and look.
     */
    @Test
    public void measureWhatCountingACollectionCosts() throws Exception {
        try (NitriteSirirusProject db = new NitriteSirirusProject(project())) {
            Database<?> storage = db.getStorage();
            countOnce(storage, FingerprintCandidate.class);
            countOnce(storage, CsiStructureMatch.class);
            countOnce(storage, FormulaCandidate.class);
            countOnce(storage, de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures.class);
            // again, now that whatever it touches is warm
            countOnce(storage, FingerprintCandidate.class);
        }
    }

    /** Just the sizes, which the store answers from a maintained count rather than by looking. */
    @Test
    public void measureCollectionSizes() throws Exception {
        try (NitriteSirirusProject db = new NitriteSirirusProject(project())) {
            Database<?> storage = db.getStorage();
            for (Class<?> collection : new Class<?>[]{
                    de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures.class,
                    CsiStructureMatch.class,
                    de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureSearchResult.class,
                    FingerprintCandidate.class,
                    FormulaCandidate.class,
                    FTreeResult.class})
                countOnce(storage, collection);
        }
    }

    private static void countOnce(Database<?> storage, Class<?> collection) throws Exception {
        long start = System.nanoTime();
        long count = storage.countAll(collection);
        System.out.printf("BENCH| countAll(%-22s) %8d records  %8d us%n", collection.getSimpleName(), count,
                TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start));
    }

    /**
     * How evenly the work divides. The structure step gives one search result to one worker, so a run is only
     * as balanced as the features are alike - and it ends when the largest one does.
     */
    @Test
    public void measureHowEvenlyTheWorkDivides() throws Exception {
        try (NitriteSirirusProject db = new NitriteSirirusProject(project())) {
            java.util.Map<Long, Integer> perFeature = new java.util.HashMap<>();
            for (CsiStructureMatch match : db.getStorage().findAll(CsiStructureMatch.class))
                perFeature.merge(match.getAlignedFeatureId(), 1, Integer::sum);

            long[] counts = perFeature.values().stream().mapToLong(Integer::longValue).sorted().toArray();
            long total = java.util.Arrays.stream(counts).sum();
            System.out.printf("BENCH| %d features, %d matches: min %d, median %d, mean %.0f, max %d%n",
                    counts.length, total, counts[0], counts[counts.length / 2], total / (double) counts.length,
                    counts[counts.length - 1]);
            // What 16 workers pulling one feature at a time would end up carrying, best case.
            long[] workers = new long[16];
            for (int i = counts.length - 1; i >= 0; i--) {
                int lightest = 0;
                for (int w = 1; w < workers.length; w++)
                    if (workers[w] < workers[lightest])
                        lightest = w;
                workers[lightest] += counts[i];
            }
            long busiest = java.util.Arrays.stream(workers).max().orElse(0);
            System.out.printf("BENCH| perfectly packed over 16 workers: busiest carries %d matches, "
                    + "the even share would be %.0f -> %.0f%% tail%n",
                    busiest, total / 16.0, 100.0 * busiest / (total / 16.0) - 100);
        }
    }

    /**
     * Whether an offset seeks into the collection or walks up to it.
     * <p>
     * Reading the source says it walks: {@code FindOptions.skipBy} becomes a {@code BoundedStream}, whose
     * iterator calls {@code next()} {@code skip} times and throws the results away, over an MVStore
     * {@code entrySet()} iterator that materialises the value of every entry it passes. If that is right, the
     * cost of fetching one record grows with the offset it sits at, at roughly the price of a full scan per
     * record skipped. If it seeks, the cost is flat.
     */
    @Test
    public void measureWhetherAnOffsetSeeksOrWalks() throws Exception {
        try (NitriteSirirusProject db = new NitriteSirirusProject(project())) {
            Database<?> storage = db.getStorage();
            long size = storage.countAll(FingerprintCandidate.class);
            System.out.println("BENCH| " + size + " candidates in the collection");

            for (long offset : new long[]{0, 1_000, 100_000, 400_000, 800_000}) {
                long start = System.nanoTime();
                int read = 0;
                for (FingerprintCandidate ignored : storage.findAll(FingerprintCandidate.class, offset, 10))
                    read++;
                long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
                System.out.printf("BENCH| offset %,9d, page of 10 -> %,10d us for %d records (%6.2f us per record "
                                + "skipped)%n",
                        offset, micros, read, offset == 0 ? 0 : micros / (double) offset);
            }
        }
    }

    /**
     * Whether what an offset costs depends on how big the documents are.
     * <p>
     * MVStore deserialises every value on a leaf page when it loads the page, so walking past an entry costs a
     * share of decoding its whole page - which would mean the price of an offset is set by the size of the
     * documents being walked past, not by the number of them. If so, walking a small index instead of the
     * documents themselves is the cheap way to reach a given position.
     */
    @Test
    public void measureWhetherSkippingCostsDependOnDocumentSize() throws Exception {
        try (NitriteSirirusProject db = new NitriteSirirusProject(project())) {
            Database<?> storage = db.getStorage();
            skipCostOf(storage, FingerprintCandidate.class, 100_000);
            skipCostOf(storage, CsiStructureMatch.class, 100_000);
            skipCostOf(storage, FormulaCandidate.class, 2_000);
        }
    }

    private static void skipCostOf(Database<?> storage, Class<?> collection, long offset) throws Exception {
        long start = System.nanoTime();
        int read = 0;
        for (Object ignored : storage.findAll(collection, offset, 10))
            read++;
        long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
        System.out.printf("BENCH| skip %,7d in %-22s -> %,10d us  (%6.2f us per record skipped), %d read%n",
                offset, collection.getSimpleName(), micros, micros / (double) offset, read);
    }

    /**
     * Reading a collection's primary keys off its index instead of out of its documents.
     * <p>
     * The comparison that matters is against walking the collection, which is the only way to collect keys
     * today - and which costs a full read of every document, because MVStore decodes a leaf page's values
     * whenever it loads the page.
     */
    @Test
    public void measureReadingPrimaryKeysFromTheIndex() throws Exception {
        try (NitriteSirirusProject db = new NitriteSirirusProject(project())) {
            NitriteDatabase storage = (NitriteDatabase) db.getStorage();
            for (Class<?> collection : new Class<?>[]{FormulaCandidate.class, CsiStructureMatch.class,
                    FingerprintCandidate.class}) {
                long start = System.nanoTime();
                List<Object> keys = storage.primaryKeys(collection);
                long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
                System.out.printf("BENCH| primaryKeys(%-22s) %,10d keys in %,9d us  (%5.2f us per key)%n",
                        collection.getSimpleName(), keys.size(), micros, micros / (double) Math.max(1, keys.size()));
            }

            // and what it costs to get the same keys the only way available before: read every document
            long start = System.nanoTime();
            long counted = 0;
            for (FormulaCandidate candidate : storage.findAll(FormulaCandidate.class))
                counted += candidate.getFormulaId();
            System.out.printf("BENCH| walking FormulaCandidate for its keys: %,d in %,d us%n", counted,
                    TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start));
        }
    }

    @Test
    public void benchmarkCandidateAndTreeAccess() throws Exception {
        System.out.println("BENCH| project " + project() + " (" + Files.size(project()) / (1024 * 1024) + " MiB), "
                + Runtime.getRuntime().availableProcessors() + " cores");

        try (NitriteSirirusProject db = new NitriteSirirusProject(project())) {
            Database<?> storage = db.getStorage();

            // Four disjoint slices: each variant meets keys no earlier variant has read.
            List<String> keys = candidateKeys(storage, 4 * SLICE);
            System.out.println("BENCH| distinct candidate keys sampled: " + keys.size());
            List<String> sliceA = keys.subList(0, Math.min(SLICE, keys.size()));
            List<String> sliceB = keys.subList(Math.min(SLICE, keys.size()), Math.min(2 * SLICE, keys.size()));
            List<String> sliceC = keys.subList(Math.min(2 * SLICE, keys.size()), Math.min(3 * SLICE, keys.size()));
            List<String> sliceD = keys.subList(Math.min(3 * SLICE, keys.size()), Math.min(4 * SLICE, keys.size()));

            time("candidates, sequential", sliceA.size(), () -> readSequentially(storage, sliceA)).report();
            time("candidates, 4 threads", sliceB.size(), () -> readInParallel(storage, sliceB, 4)).report();
            time("candidates, 8 threads", sliceC.size(), () -> readInParallel(storage, sliceC, 8)).report();
            time("candidates, 16 threads", sliceD.size(), () -> readInParallel(storage, sliceD, 16)).report();

            // Re-reading a slice already read shows what the page cache is worth, i.e. how much of the cost is
            // reaching the data versus turning it into objects.
            time("candidates, sequential, already read once", sliceA.size(),
                    () -> readSequentially(storage, sliceA)).report();

            // How big the collections actually are decides whether scanning beats looking up.
            System.out.printf("BENCH| counts: matches=%d candidates=%d formulaCandidates=%d trees=%d features=%d%n",
                    storage.countAll(CsiStructureMatch.class), storage.countAll(FingerprintCandidate.class),
                    storage.countAll(FormulaCandidate.class), storage.countAll(FTreeResult.class),
                    storage.countAll(de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures.class));

            // A full sequential pass over the candidates: if most of them are needed anyway, streaming them in
            // storage order may beat asking for them one key at a time.
            time("candidates, full sequential scan", storage.countAll(FingerprintCandidate.class), () -> {
                long links = 0;
                for (FingerprintCandidate candidate : storage.findAll(FingerprintCandidate.class))
                    if (candidate.getLinks() != null)
                        links += candidate.getLinks().size();
                return links;
            }).report();

            List<Long> ids = formulaIds(storage, 2 * SLICE);
            System.out.println("BENCH| formula candidates sampled: " + ids.size());
            // Half each, so the two variants meet different trees and neither inherits the other's warm pages.
            int half = ids.size() / 2;
            List<Long> treesA = ids.subList(0, half);
            List<Long> treesB = ids.subList(half, ids.size());

            time("fragmentation trees, sequential", treesA.size(),
                    () -> readTreesSequentially(storage, treesA)).report();
            time("fragmentation trees, 8 threads", treesB.size(),
                    () -> readTreesInParallel(storage, treesB, 8)).report();
            time("fragmentation trees, 16 threads, already read once", treesB.size(),
                    () -> readTreesInParallel(storage, treesB, 16)).report();
            time("fragmentation trees, sequential, already read once", treesA.size(),
                    () -> readTreesSequentially(storage, treesA)).report();
        }
    }
}
