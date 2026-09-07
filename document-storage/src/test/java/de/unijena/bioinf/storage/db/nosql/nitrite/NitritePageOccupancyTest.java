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

import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import org.dizitart.no2.collection.Document;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MVStore's {@code pageSplitSize} is a byte budget per B-tree page, and a budget
 * too small for a single record splits every leaf down to one record - which
 * turns the tree into a binary tree and inflates the file permanently (#971).
 * These tests read the page geometry back out of the closed file and assert that
 * records actually share leaf pages.
 */
public class NitritePageOccupancyTest {
    private static final String COLLECTION = "PAGES";
    private static final int RECORDS = 4000;

    /** Average number of records per leaf page of the collection's document map. */
    private static double recordsPerLeafPage(Path file, long[] depthOut) {
        try (MVStore store = new MVStore.Builder().fileName(file.toString()).readOnly().open()) {
            assertTrue(store.hasMap(COLLECTION), () -> "collection map missing, found " + store.getMapNames());
            MVMap<Object, Object> map = store.openMap(COLLECTION);
            long[] counts = new long[3]; // { records, leaf pages, max depth }
            countLeaves(map.getRootPage(), counts, 1);
            assertEquals(RECORDS, counts[0], "unexpected record count");
            if (depthOut != null) depthOut[0] = counts[2];
            return (double) counts[0] / counts[1];
        }
    }

    private static void countLeaves(Page<Object, Object> page, long[] counts, int depth) {
        if (page.isLeaf()) {
            counts[0] += page.getKeyCount();
            counts[1]++;
            counts[2] = Math.max(counts[2], depth);
            return;
        }
        for (int i = 0; i < page.getRawChildPageCount(); i++) {
            countLeaves(page.getChildPage(i), counts, depth + 1);
        }
    }

    private static Path write(Path dir, String name, Integer pageSplitSizeByte) throws IOException {
        return write(dir, name, pageSplitSizeByte, 64);
    }

    private static Path write(Path dir, String name, Integer pageSplitSizeByte, int cacheSizeMiB) throws IOException {
        Path file = dir.resolve(name + ".db");
        Metadata meta = Metadata.build().addCollection(COLLECTION, Index.unique("key"));

        NitriteDatabase db = pageSplitSizeByte == null
                ? new NitriteDatabase(file, meta, NitriteDatabase.MVStoreCompression.LZF, cacheSizeMiB, 8192, false)
                : new NitriteDatabase(file, meta, NitriteDatabase.MVStoreCompression.LZF, cacheSizeMiB, 8192,
                        pageSplitSizeByte, false);
        try (NitriteDatabase open = db) {
            Random rnd = new Random(42);
            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < RECORDS; i++) {
                batch.add(Document.createDocument("key", "k-" + i)
                        .put("rank", i % 50)
                        .put("score", rnd.nextDouble()));
                if (batch.size() == 500) {
                    open.insertAll(COLLECTION, batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) open.insertAll(COLLECTION, batch);
        }
        return file;
    }

    @Test
    public void testDefaultConfigurationSharesLeafPages(@TempDir Path dir) throws IOException {
        long[] depth = new long[1];
        double perPage = recordsPerLeafPage(write(dir, "default", null), depth);
        assertTrue(perPage > 4, () -> "expected records to share leaf pages, got " + perPage + " per page");
        assertTrue(depth[0] <= 8, () -> "expected a shallow tree, got depth " + depth[0]);
    }

    @Test
    public void testExplicitBudgetIsHonoured(@TempDir Path dir) throws IOException {
        double small = recordsPerLeafPage(write(dir, "small", 16 * 1024), null);
        double large = recordsPerLeafPage(write(dir, "large", NitriteDatabase.DEFAULT_PAGE_SPLIT_SIZE_BYTE), null);
        assertTrue(large > small, () -> "a larger budget has to fill pages further, got "
                + large + " vs " + small + " per page");
    }

    @Test
    public void testBudgetIsCappedByTheReadCache(@TempDir Path dir) throws IOException {
        // MVStore allows at most a 256th of the read cache per page and says nothing when it caps,
        // so a 4 MiB cache holds the default 64 KiB budget down to 16 KiB
        double capped = recordsPerLeafPage(write(dir, "capped", null, 4), null);
        double uncapped = recordsPerLeafPage(write(dir, "uncapped", null, 64), null);
        assertTrue(capped < uncapped, () -> "a small read cache has to cap the page budget, got "
                + capped + " vs " + uncapped + " records per page");
        assertEquals(recordsPerLeafPage(write(dir, "explicit16k", 16 * 1024, 64), null), capped, 0.5);
    }

    @Test
    public void testTinyBudgetDegeneratesToOneRecordPerPage(@TempDir Path dir) throws IOException {
        // the defect behind #971: a budget smaller than one record leaves exactly
        // one record per leaf page, whatever the data
        assertEquals(1.0, recordsPerLeafPage(write(dir, "tiny", 16), null), 0.0001);
    }
}
