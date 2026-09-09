/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.storage.db.nosql.nitrite;

import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import org.dizitart.no2.collection.Document;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreTool;
import org.h2.mvstore.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A store written with a page budget too small for its records keeps one record
 * per page for good: a compaction copies live pages verbatim. Only rewriting the
 * records into a fresh store re-splits them, which is what
 * {@link NitriteDatabase#compact()} does when it finds a store written against a
 * smaller budget than it is opened with.
 * <p>
 * That a plain compaction cannot is asserted here too, since it is the whole
 * reason the rewrite exists: measured on a real 6.5.4 project, a compaction takes
 * 1011 MiB to 860 MiB and leaves every page exactly as it was.
 */
public class PageLayoutRepairTest {
    private static final String COLLECTION = "PAGES";

    private static final int RECORDS = 6000;

    /**
     * The page budget every release before the fix wrote with: Nitrite's own
     * {@code MVStoreModuleBuilder} default, which is smaller than any single record and
     * therefore leaves one record per page.
     */
    private static final int BROKEN_BUDGET = 16;

    private static Metadata metadata() {
        return Metadata.build().addCollection(COLLECTION, Index.unique("key"));
    }

    private static NitriteDatabase open(Path file, int pageSplitSizeByte, boolean readOnly) throws IOException {
        return open(file, pageSplitSizeByte, readOnly, 64);
    }

    private static NitriteDatabase open(Path file, int pageSplitSizeByte, boolean readOnly, int cacheSizeMiB) throws IOException {
        return new NitriteDatabase(file, metadata(), NitriteDatabase.MVStoreCompression.LZF,
                cacheSizeMiB, 8192, pageSplitSizeByte, readOnly);
    }

    private static Path write(Path dir, String name, int pageSplitSizeByte) throws IOException {
        return write(dir, name, pageSplitSizeByte, 64);
    }

    private static Path write(Path dir, String name, int pageSplitSizeByte, int cacheSizeMiB) throws IOException {
        Path file = dir.resolve(name + ".db");
        try (NitriteDatabase db = open(file, pageSplitSizeByte, false, cacheSizeMiB)) {
            Random rnd = new Random(42);
            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < RECORDS; i++) {
                batch.add(Document.createDocument("key", "k-" + i)
                        .put("rank", i % 50)
                        .put("score", rnd.nextDouble())
                        .put("label", "value-" + i));
                if (batch.size() == 500) {
                    db.insertAll(COLLECTION, batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) db.insertAll(COLLECTION, batch);
        }
        return file;
    }

    /**
     * A store as a release before the fix left one: written against a budget smaller than
     * its records, and carrying no record of what that budget was.
     * <p>
     * The marker has to be cleared explicitly because {@link NitriteDatabase} writes it
     * for every store it creates, this fixture included - and it is the missing marker,
     * not a small one, that every existing project actually has.
     */
    private static Path writePreFixStore(Path dir, String name) throws IOException {
        Path file = write(dir, name, BROKEN_BUDGET);
        try (MVStore store = new MVStore.Builder().fileName(file.toString()).open()) {
            store.setStoreVersion(0);
            store.commit();
        }
        return file;
    }

    /** { records, leaves, minDepth, maxDepth } of the collection's document map. */
    private static long[] geometry(Path file) {
        try (MVStore store = new MVStore.Builder().fileName(file.toString()).readOnly().open()) {
            MVMap<Object, Object> map = store.openMap(COLLECTION);
            long[] acc = {0, 0, Integer.MAX_VALUE, 0};
            walk(map.getRootPage(), acc, 1);
            return acc;
        }
    }

    private static void walk(Page<Object, Object> page, long[] acc, int depth) {
        if (page.isLeaf()) {
            acc[0] += page.getKeyCount();
            acc[1]++;
            acc[2] = Math.min(acc[2], depth);
            acc[3] = Math.max(acc[3], depth);
            return;
        }
        for (int i = 0; i < page.getRawChildPageCount(); i++) walk(page.getChildPage(i), acc, depth + 1);
    }

    /** The page budget recorded in the store, 0 for one written before it was recorded. */
    private static int recordedBudget(Path file) {
        try (MVStore store = new MVStore.Builder().fileName(file.toString()).readOnly().open()) {
            return store.getStoreVersion();
        }
    }

    private static List<String> readAllKeys(Path file) throws IOException {
        List<String> keys = new ArrayList<>();
        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, true)) {
            for (Document d : db.findAll(COLLECTION)) keys.add(d.get("key", String.class));
        }
        return keys;
    }

    @Test
    public void testCompactingFillsThePagesOfAnOutdatedStoreAndKeepsEveryRecord(@TempDir Path dir) throws IOException {
        Path file = writePreFixStore(dir, "broken");

        long[] before = geometry(file);
        assertEquals(RECORDS, before[0]);
        assertEquals(1.0, (double) before[0] / before[1], 0.0001,
                "a budget smaller than one record has to leave one record per page");
        long sizeBefore = Files.size(file);
        List<String> keysBefore = readAllKeys(file);

        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false)) {
            assertTrue(db.isCompactionRecommended(),
                    "a store written against a smaller budget has to be reported as worth compacting");
            db.compact();
        }

        long[] after = geometry(file);
        assertEquals(RECORDS, after[0], "a rewrite must not lose or duplicate records");
        assertTrue((double) after[0] / after[1] > 4,
                () -> "expected filled pages, got " + (double) after[0] / after[1] + " per page");
        long sizeAfter = Files.size(file);
        assertTrue(sizeAfter < sizeBefore / 2,
                () -> "expected the file to shrink markedly, " + sizeBefore + " -> " + sizeAfter);
        assertEquals(keysBefore, readAllKeys(file), "every record has to come back unchanged, in the same order");
        assertFalse(Files.exists(file.resolveSibling(file.getFileName() + ".repair")), "temp file left behind");
    }

    @Test
    public void testCompactingShallowsAndBalancesAnOutdatedTree(@TempDir Path dir) throws IOException {
        Path file = writePreFixStore(dir, "deep");

        long[] before = geometry(file);
        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false)) {
            db.compact();
        }
        long[] after = geometry(file);

        // fewer, fuller pages means fewer levels to walk to reach one
        assertTrue(after[3] < before[3],
                () -> "the rewritten tree has to be shallower, " + before[2] + ".." + before[3]
                        + " -> " + after[2] + ".." + after[3]);
        // a B-tree should hold every leaf at one depth; a store written past its budget does
        // not, and measured on a real 6.5.4 project ranged from 4 to 1644
        assertEquals(after[2], after[3],
                () -> "a rewritten tree has to be balanced, got depths " + after[2] + ".." + after[3]);
    }

    @Test
    public void testARewriteIsRecordedSoItHappensOnlyOnce(@TempDir Path dir) throws IOException {
        Path file = writePreFixStore(dir, "twice");
        assertEquals(0, recordedBudget(file), "a store written before the marker existed records nothing");

        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false)) {
            db.compact();
        }
        assertEquals(NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, recordedBudget(file),
                "the rewrite has to record the budget it wrote with");

        long firstSize = Files.size(file);
        List<String> keys = readAllKeys(file);
        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false)) {
            assertFalse(db.isCompactionRecommended(), "a rewritten store must not ask to be rewritten again");
            db.compact();
        }
        assertEquals(keys, readAllKeys(file), "a second compaction must change nothing about the contents");
        long secondSize = Files.size(file);
        assertTrue(Math.abs(secondSize - firstSize) < firstSize / 10,
                () -> "a second compaction should not change the size much, " + firstSize + " -> " + secondSize);
    }

    @Test
    public void testAStoreWrittenByThisBuildRecordsItsBudgetAndNeedsNoRewrite(@TempDir Path dir) throws IOException {
        Path file = write(dir, "healthy", NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE);
        assertEquals(NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, recordedBudget(file),
                "a store this build creates has to record the budget it was created with");
        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false)) {
            assertFalse(db.isCompactionRecommended(),
                    "a store written with the current budget has no layout to repair");
        }
    }

    @Test
    public void testTheRecordedBudgetIsTheOneTheReadCacheAllowed(@TempDir Path dir) throws IOException {
        // MVStore caps the budget at a 256th of the read cache, so a 4 MiB cache writes 16 KiB pages
        // however much was asked for - and recording the requested budget would claim a layout the
        // store does not have
        Path file = write(dir, "capped", NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, 4);
        assertEquals(16 * 1024, recordedBudget(file), "the effective budget has to be the one recorded");

        // opened with a cache that allows more, the same store can be filled further - so it says so
        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false, 64)) {
            assertTrue(db.isCompactionRecommended(),
                    "a store whose pages could hold more has to be reported as worth compacting");
        }
    }

    @Test
    public void testASmallerReadCacheDoesNotAskForADowngradeRewrite(@TempDir Path dir) throws IOException {
        Path file = write(dir, "wide", NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, 64);
        // a 4 MiB cache caps the budget at 16 KiB, below what this store was written with. Rewriting it
        // against that would make the file bigger, so it must not be suggested.
        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false, 4)) {
            assertFalse(db.isCompactionRecommended(),
                    "a smaller page budget than the store already has is not a repair");
        }
    }

    @Test
    public void testAPlainCompactionCannotResplitPages(@TempDir Path dir) throws IOException {
        // The premise the rewrite exists for, asserted against h2 itself rather than through our own
        // decision about which path to take: a compaction copies live pages verbatim, so it reclaims
        // garbage and keeps whatever layout it finds.
        Path file = writePreFixStore(dir, "plain");

        long[] before = geometry(file);
        assertEquals(1.0, (double) before[0] / before[1], 0.0001);

        MVStoreTool.compact(file.toString(), true);

        long[] after = geometry(file);
        assertEquals(RECORDS, after[0], "a compaction must not lose or duplicate records");
        assertEquals(1.0, (double) after[0] / after[1], 0.0001,
                "a plain compaction copies live pages verbatim, so it cannot re-split them");
    }

    @Test
    public void testAnInterruptedCompactionIsPutBackTogetherOnOpen(@TempDir Path dir) throws IOException {
        Path file = write(dir, "interrupted", NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE);
        List<String> keys = readAllKeys(file);

        // What MVStoreTool.moveAtomicReplace's fallback leaves behind when it is killed between deleting
        // the destination and moving the replacement into place: the store exists only under .newFile,
        // and h2 renames it back only if something asks it to.
        Path newFile = file.resolveSibling(file.getFileName() + org.h2.engine.Constants.SUFFIX_MV_STORE_NEW_FILE);
        Files.move(file, newFile);
        assertFalse(Files.exists(file), "the fixture has to look like an interrupted replace");

        // goes through a read-only open, which has to recover too - the alternative is a project that
        // cannot be opened at all
        assertEquals(keys, readAllKeys(file), "the interrupted compaction has to be recovered on open");
        assertFalse(Files.exists(newFile), "the replacement has to be moved back, not copied");
    }

    @Test
    public void testALeftoverRewriteTargetIsDiscardedOnOpen(@TempDir Path dir) throws IOException {
        Path file = write(dir, "leftover", NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE);
        Path leftover = file.resolveSibling(file.getFileName() + ".repair");
        Files.writeString(leftover, "half a rewrite");

        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false)) {
            assertFalse(db.isCompactionRecommended());
        }
        assertFalse(Files.exists(leftover), "a partial rewrite from a killed compaction has to be dropped");
    }

    @Test
    public void testAnOlderReleaseWritingUnderASmallerBudgetIsNoticed(@TempDir Path dir) throws IOException {
        // The scenario the recorded budget alone cannot see: a project repaired by this build, then written
        // to by a release that predates the marker. It adds pages under its own budget and cannot lower the
        // marker, so the project would otherwise stay silently oversized for good.
        Path file = write(dir, "downgraded", NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE);
        assertEquals(NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, recordedBudget(file));

        try (NitriteDatabase old = open(file, BROKEN_BUDGET, false)) {
            assertFalse(old.isCompactionRecommended(), "the older release does not know about any of this");
            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < 20_000; i++) {
                batch.add(Document.createDocument("key", "old-" + i).put("rank", i % 50).put("label", "v" + i));
                if (batch.size() == 500) {
                    old.insertAll(COLLECTION, batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) old.insertAll(COLLECTION, batch);
        }
        assertEquals(NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, recordedBudget(file),
                "the older release cannot lower the marker, which is the whole problem");

        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false)) {
            assertTrue(db.isCompactionRecommended(),
                    "pages written under a smaller budget have to be noticed even though the marker looks current");
        }
    }

    @Test
    public void testIndexesSurviveARewrite(@TempDir Path dir) throws IOException {
        Path file = writePreFixStore(dir, "indexed");
        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, false)) {
            db.compact();
        }
        try (NitriteDatabase db = open(file, NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE, true)) {
            // goes through the unique index on "key", which lives in its own maps
            List<Document> hit = new ArrayList<>();
            for (Document d : db.find(COLLECTION, Filter.where("key").eq("k-4711"))) hit.add(d);
            assertEquals(1, hit.size(), "the unique index has to still find a record after a rewrite");
            assertEquals(4711 % 50, hit.get(0).get("rank", Integer.class));
        }
    }
}
