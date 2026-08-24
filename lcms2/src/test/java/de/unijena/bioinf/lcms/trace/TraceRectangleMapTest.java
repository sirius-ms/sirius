package de.unijena.bioinf.lcms.trace;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A rectangle that was put into the map has to be found by a query that lies inside it.
 * <p>
 * This is the whole contract the trace pickers rely on: a rectangle stands for a whole trace, and a lookup asks
 * whether a peak is already covered by one. A lookup that misses a rectangle which does cover the peak makes the
 * picker create a second trace over the same peaks, and on a real experiment that happened for about one seed in a
 * hundred thousand - differently on every run, because the misses depend on the index's internals rather than on the
 * data.
 */
public class TraceRectangleMapTest {

    private static MVStore store() {
        return store(0);
    }

    /** @param cacheMegabytes as {@code LCMSStorage} sizes it; zero leaves the default */
    private static MVStore store(int cacheMegabytes) {
        final MVStore.Builder b = new MVStore.Builder().fileStore(new OffHeapStore()).autoCommitDisabled();
        return (cacheMegabytes > 0 ? b.cacheSize(cacheMegabytes) : b).open();
    }

    /** a file backed store, which is what the pipeline actually uses for a sample */
    private static MVStore fileStore(int cacheMegabytes) {
        try {
            final java.io.File f = java.io.File.createTempFile("rectmap-test_", ".mvstore");
            f.deleteOnExit();
            return new MVStore.Builder().fileName(f.getAbsolutePath()).autoCommitDisabled()
                    .cacheSize(cacheMegabytes).open();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** rectangles shaped like real MS2 fragment traces: narrow in mass, a few seconds wide */
    private static List<Rect> realisticTraces(int count) {
        final List<Rect> rects = new ArrayList<>(count);
        for (int k = 0; k < count; ++k) {
            // masses spread over 30 Da as a DIA channel does, retention times over a 900 s run
            final double mz = 909d + (k * 31d) % 30_000 / 1000d;
            final double rt = (k * 137d) % 900d;
            final double width = 1.5 + (k % 7) * 0.4;
            rects.add(new Rect(mz - 0.002, mz + 0.002, rt, rt + width, mz));
        }
        return rects;
    }

    /**
     * The same contract with the map used the way the picker uses it: a rectangle is added, then queried against, then
     * another is added, with the store committed as the pipeline commits it and the cache bounded as
     * {@code LCMSStorage} bounds it. A pipeline run has many samples in flight, so each store's cache holds far less
     * than its data.
     */
    @Test
    public void aRectangleIsStillFoundWhenAddsAndQueriesInterleaveUnderASmallCache() {
        try (MVStore store = store(1)) {
            final TraceRectangleMap map = new TraceRectangleMapByRVMap(store, "test");
            final List<Rect> rects = realisticTraces(20_000);
            final List<Rect> added = new ArrayList<>(rects.size());
            int missed = 0;
            for (int k = 0; k < rects.size(); ++k) {
                map.addRect(rects.get(k));
                added.add(rects.get(k));
                if (k % 64 == 0) store.commit();
                // query for something added a while ago, as a later seed does
                final Rect earlier = added.get((k * 7919) % added.size());
                final double mz = earlier.avgMz;
                final double rt = (earlier.minRt + earlier.maxRt) / 2d;
                boolean found = false;
                for (Rect hit : map.overlappingRectangle(new Rect(mz - 1e-4, mz + 1e-4, rt, rt, mz))) {
                    if (hit.id == earlier.id) { found = true; break; }
                }
                if (!found) ++missed;
            }
            assertEquals(0, missed, missed + " lookups missed a rectangle that covers the queried point");
        }
    }

    /**
     * The same again, but with several stores in flight at once, each on its own thread and its own map.
     * <p>
     * Nothing is shared between them by design, which is what makes this worth checking: in the pipeline a sample is
     * processed by one thread and owns its store, yet a sample that is reproducible when run alone stops being so as
     * soon as other samples are processed beside it.
     */
    @Test
    public void rectanglesAreStillFoundWhileOtherStoresAreBusy() throws Exception {
        final int threads = 8;
        final java.util.concurrent.atomic.AtomicInteger missed = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; ++t) {
            final Thread worker = new Thread(() -> {
                try (MVStore store = store(1)) {
                    final TraceRectangleMap map = new TraceRectangleMapByRVMap(store, "test");
                    final List<Rect> rects = realisticTraces(20_000);
                    final List<Rect> added = new ArrayList<>(rects.size());
                    for (int k = 0; k < rects.size(); ++k) {
                        map.addRect(rects.get(k));
                        added.add(rects.get(k));
                        if (k % 64 == 0) store.commit();
                        final Rect earlier = added.get((k * 7919) % added.size());
                        final double mz = earlier.avgMz;
                        final double rt = (earlier.minRt + earlier.maxRt) / 2d;
                        boolean found = false;
                        for (Rect hit : map.overlappingRectangle(new Rect(mz - 1e-4, mz + 1e-4, rt, rt, mz))) {
                            if (hit.id == earlier.id) { found = true; break; }
                        }
                        if (!found) missed.incrementAndGet();
                    }
                }
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) worker.join();
        assertEquals(0, missed.get(), missed.get() + " lookups missed a covering rectangle with "
                + threads + " stores in flight");
    }

    /** as above, on the file backed store the pipeline really uses, with several in flight */
    @Test
    public void rectanglesAreStillFoundOnAFileBackedStoreUnderLoad() throws Exception {
        final int threads = 8;
        final java.util.concurrent.atomic.AtomicInteger missed = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; ++t) {
            final Thread worker = new Thread(() -> {
                try (MVStore store = fileStore(1)) {
                    final TraceRectangleMap map = new TraceRectangleMapByRVMap(store, "test");
                    final List<Rect> rects = realisticTraces(20_000);
                    final List<Rect> added = new ArrayList<>(rects.size());
                    for (int k = 0; k < rects.size(); ++k) {
                        map.addRect(rects.get(k));
                        added.add(rects.get(k));
                        if (k % 64 == 0) store.commit();
                        final Rect earlier = added.get((k * 7919) % added.size());
                        final double mz = earlier.avgMz;
                        final double rt = (earlier.minRt + earlier.maxRt) / 2d;
                        boolean found = false;
                        for (Rect hit : map.overlappingRectangle(new Rect(mz - 1e-4, mz + 1e-4, rt, rt, mz))) {
                            if (hit.id == earlier.id) { found = true; break; }
                        }
                        if (!found) missed.incrementAndGet();
                    }
                }
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) worker.join();
        assertEquals(0, missed.get(), missed.get() + " lookups missed a covering rectangle on a file backed store");
    }

    @Test
    public void everyStoredRectangleIsFoundByAQueryInsideIt() {
        try (MVStore store = store()) {
            final TraceRectangleMap map = new TraceRectangleMapByRVMap(store, "test");
            final List<Rect> rects = realisticTraces(20_000);
            for (Rect r : rects) map.addRect(r);

            int missed = 0;
            for (Rect r : rects) {
                // a query at the rectangle's own centre: it cannot be outside itself
                final double mz = r.avgMz;
                final double rt = (r.minRt + r.maxRt) / 2d;
                final Rect query = new Rect(mz - 1e-4, mz + 1e-4, rt, rt, mz);
                boolean found = false;
                for (Rect hit : map.overlappingRectangle(query)) {
                    if (hit.id == r.id) { found = true; break; }
                }
                if (!found) ++missed;
            }
            assertEquals(0, missed, missed + " of " + rects.size()
                    + " rectangles were not found by a query at their own centre");
        }
    }
}
