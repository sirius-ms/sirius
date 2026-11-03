package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * What raising a run's maximum intensity costs to reindex, and what splitting that work across
 * {@link NoSQLProjectImpl#workerCount} cores is worth against running it as a single batch (1 worker). Opens a
 * real multi-run project, picks the run with the most aligned features, raises its maximum by one
 * strictly-higher apex intensity (the same thing a new, more-intense import does), and times
 * {@link NoSQLProjectImpl#addToSearchIndexLongIds} - which folds the raise into the project's maxima and
 * rewrites every other feature of that run.
 * <p>
 * Run explicitly with {@code REINDEX_BENCHMARK_PROJECT=<path to a project with a multi-run LC-MS import>},
 * e.g. the tomato project {@code TomatoProjectSetup} builds (12 runs, ~52,500 run features at last count).
 */
@EnabledIfEnvironmentVariable(named = "REINDEX_BENCHMARK_PROJECT", matches = ".+")
public class ParallelReindexBenchmark {

    private static Path project() {
        return Path.of(System.getenv("REINDEX_BENCHMARK_PROJECT"));
    }

    /** A fresh in-memory search index for {@code ps} - never shared, so each round starts genuinely empty. */
    private static SearchService freshSearchService() throws Exception {
        return new SearchServiceImpl(p -> {
            Map<String, ValueType> tagDefinitions = new HashMap<>();
            for (Object item : p.findTags()) {
                TagDefinition td = (TagDefinition) item;
                tagDefinitions.put(td.getTagName(), td.getValueType());
            }
            return new PerPojoSearchContext(null, tagDefinitions);
        });
    }

    /**
     * What a full, from-scratch index build ({@code NoSQLProjectImpl}'s constructor on an empty index, the same
     * work {@code BackgroundRuns} triggers via {@code createSearchIndex(true)} after an LC-MS import) costs
     * split across every core against running it as one batch - every feature in the project goes through
     * {@link NoSQLProjectImpl#feedAllFeaturesToIndex}, not just one run's worth, so this is the other half of
     * what {@link #benchmarkReindexingTheLargestRun} measures for a single run's fan-out.
     */
    @Test
    public void benchmarkFullProjectRebuild() throws Exception {
        Path location = project();
        Assumptions.assumeTrue(Files.exists(location), "project not found at " + location);

        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            long featureCount = ps.getStorage().countAll(AlignedFeatures.class);
            System.out.printf("BENCH| rebuilding the index for %d features%n", featureCount);

            // Alternated for the same reason benchmarkReindexingTheLargestRun is: a one-shot "1 then all"
            // comparison would let page-cache warming from the first variant favour whichever ran second.
            // Each round gets its own fresh, empty in-memory index - the index built by an earlier round
            // would otherwise make NoSQLProjectImpl's constructor skip the rebuild entirely.
            int[] variants = {1, 0, 1, 0, 1, 0, 1, 0}; // 0 means "all cores", NoSQLProjectImpl.workers()'s default
            for (int workers : variants) {
                NoSQLProjectImpl.workerCount = workers;
                try {
                    long start = System.nanoTime();
                    new NoSQLProjectImpl("rebuild-bench", new NoSQLProjectSpaceManager(ps), freshSearchService(), (a, b) -> false);
                    long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    System.out.printf("BENCH| %2d worker%s: full rebuild of %d features in %d ms%n",
                            workers, workers == 1 ? " " : "s", featureCount, millis);
                } finally {
                    NoSQLProjectImpl.workerCount = 0;
                }
            }
        }
    }

    @Test
    public void benchmarkReindexingTheLargestRun() throws Exception {
        Path location = project();
        Assumptions.assumeTrue(Files.exists(location), "project not found at " + location);

        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            long openStart = System.nanoTime();
            // Builds the (in-memory) search index from scratch over the whole project - setup cost, not
            // what this benchmark times; every run below reindexes against this already-complete index.
            NoSQLProjectImpl project = new NoSQLProjectImpl("reindex-bench", new NoSQLProjectSpaceManager(ps), freshSearchService(), (a, b) -> false);
            System.out.printf("BENCH| opened and indexed project in %d ms%n",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - openStart));

            Map<Long, List<Feature>> byRun = new HashMap<>();
            for (Feature f : ps.getStorage().findAll(Feature.class))
                if (f.getRunId() != null)
                    byRun.computeIfAbsent(f.getRunId(), id -> new ArrayList<>()).add(f);
            Map.Entry<Long, List<Feature>> largest = byRun.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()))
                    .orElseThrow(() -> new IllegalStateException("project has no run features to reindex"));
            long runId = largest.getKey();
            List<Feature> runFeatures = largest.getValue();
            System.out.printf("BENCH| %d runs, largest is run %d with %d run features%n",
                    byRun.size(), runId, runFeatures.size());

            double raisedApex = runFeatures.stream().mapToDouble(f -> f.getApexIntensity() == null ? 0 : f.getApexIntensity())
                    .max().orElse(0);
            Feature toRaise = runFeatures.getFirst();

            // Alternated, not run once each: a one-shot "1 then all" comparison is exactly what
            // build-and-test-sirius warns against - repeatedly reindexing the same run warms the store's
            // page cache round over round, which would make every later variant look faster than an
            // earlier one regardless of worker count. Four rounds each, interleaved, so any such drift
            // lands on both variants rather than favouring whichever ran last.
            int[] variants = {1, 0, 1, 0, 1, 0, 1, 0}; // 0 means "all cores", NoSQLProjectImpl.workers()'s default
            for (int workers : variants) {
                NoSQLProjectImpl.workerCount = workers;
                try {
                    // Monotone maxima: always strictly above whatever is recorded so far, in every
                    // iteration, so raisedSince() sees a genuine rise each time regardless of how
                    // many variants already ran.
                    raisedApex += 1.0;
                    toRaise.setApexIntensity(raisedApex);
                    ps.getStorage().upsert(toRaise);
                    ps.featuresWritten(List.of(toRaise));

                    long start = System.nanoTime();
                    project.addToSearchIndexLongIds(List.of(toRaise.getAlignedFeatureId()), null);
                    long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    System.out.printf("BENCH| %2d worker%s: reindexed run %d (%d features) in %d ms%n",
                            workers, workers == 1 ? " " : "s", runId, runFeatures.size(), millis);
                } finally {
                    NoSQLProjectImpl.workerCount = 0;
                }
            }
        }
    }
}
