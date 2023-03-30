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

package de.unijena.bioinf.storage.db.nitrite;

import com.google.api.client.util.Lists;
import com.google.common.collect.Streams;
import de.unijena.bioinf.storage.db.NoSQLDatabase;
import de.unijena.bioinf.storage.db.NoSQLFilter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NitriteDatabaseTest {

    private static class NitriteTestEntry extends NitritePOJO {

        public static final Index[] index = {new Index("name", IndexType.UNIQUE)};

        public String name;

        public NitriteTestEntry() {
            super();
        }

        public NitriteTestEntry(String name) {
            super();
            this.name = name;
        }

    }

    private static class NitriteChildTestEntry extends NitritePOJO {

        public static final Index[] index = {new Index("name", IndexType.NON_UNIQUE)};

        public String name;

        public NitriteId parentId;

        public NitriteChildTestEntry() { super(); }

        public NitriteChildTestEntry(String name, NitriteId parentId) {
            super();
            this.name = name;
            this.parentId = parentId;
        }

    }

    private static class NitriteFamilyTestEntry extends NitritePOJO {

        public static final Index[] index = {new Index("name", IndexType.UNIQUE)};

        public String name;

        public List<NitriteChildTestEntry> children;

        public NitriteFamilyTestEntry() {
            super();
        }

    }

    @Test
    public void testFilters() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file)) {
            NoSQLFilter[] nof = {
                    // SIMPLE FILTERS
                    new NoSQLFilter().eq("a", 42),
                    new NoSQLFilter().gt("a", 42),
                    new NoSQLFilter().gte("a", 42),
                    new NoSQLFilter().lt("a", 42),
                    new NoSQLFilter().lte("a", 42),
                    new NoSQLFilter().text("a", "foo"),
                    new NoSQLFilter().regex("a", "foo"),
                    new NoSQLFilter().inByte("a", (byte) 12, (byte) 42),
                    new NoSQLFilter().inShort("a", (short) 12, (short) 42),
                    new NoSQLFilter().inInt("a", 12, 42),
                    new NoSQLFilter().inLong("a", 12L, 42L),
                    new NoSQLFilter().inFloat("a", 12f, 42f),
                    new NoSQLFilter().inDouble("a", 12d, 42d),
                    new NoSQLFilter().inChar("a", 't', 'f'),
                    new NoSQLFilter().inBool("a", true, false),
                    new NoSQLFilter().in("a", "foo", "bar"),
                    new NoSQLFilter().notInByte("a", (byte) 12, (byte) 42),
                    new NoSQLFilter().notInShort("a", (short) 12, (short) 42),
                    new NoSQLFilter().notInInt("a", 12, 42),
                    new NoSQLFilter().notInLong("a", 12L, 42L),
                    new NoSQLFilter().notInFloat("a", 12f, 42f),
                    new NoSQLFilter().notInDouble("a", 12d, 42d),
                    new NoSQLFilter().notInChar("a", 't', 'f'),
                    new NoSQLFilter().notInBool("a", true, false),
                    new NoSQLFilter().notIn("a", "foo", "bar"),
                    // CONJUGATE FILTERS
                    new NoSQLFilter().not().eq("a", 42),
                    new NoSQLFilter().and().eq("a", 42).gt("a", 42).gte("a", 42),
                    new NoSQLFilter().not().and().eq("a", 42).gt("a", 42),
                    new NoSQLFilter().and().not().eq("a", 42).gt("a", 42),
                    new NoSQLFilter().and().eq("a", 42).not().gt("a", 42),
                    new NoSQLFilter().or().eq("a", 42).gt("a", 42).gte("a", 42),
                    new NoSQLFilter().not().or().eq("a", 42).gt("a", 42),
                    new NoSQLFilter().or().not().eq("a", 42).gt("a", 42),
                    new NoSQLFilter().or().eq("a", 42).not().gt("a", 42),
            };

            ObjectFilter[] of = {
                    // SIMPLE FILTERS
                    ObjectFilters.eq("a", 42),
                    ObjectFilters.gt("a", 42),
                    ObjectFilters.gte("a", 42),
                    ObjectFilters.lt("a", 42),
                    ObjectFilters.lte("a", 42),
                    ObjectFilters.text("a", "foo"),
                    ObjectFilters.regex("a", "foo"),
                    ObjectFilters.in("a", (byte) 12, (byte) 42),
                    ObjectFilters.in("a", (short) 12, (short) 42),
                    ObjectFilters.in("a", 12, 42),
                    ObjectFilters.in("a", 12L, 42L),
                    ObjectFilters.in("a", 12f, 42f),
                    ObjectFilters.in("a", 12d, 42d),
                    ObjectFilters.in("a", 't', 'f'),
                    ObjectFilters.in("a", true, false),
                    ObjectFilters.in("a", "foo", "bar"),
                    ObjectFilters.notIn("a", (byte) 12, (byte) 42),
                    ObjectFilters.notIn("a", (short) 12, (short) 42),
                    ObjectFilters.notIn("a", 12, 42),
                    ObjectFilters.notIn("a", 12L, 42L),
                    ObjectFilters.notIn("a", 12f, 42f),
                    ObjectFilters.notIn("a", 12d, 42d),
                    ObjectFilters.notIn("a", 't', 'f'),
                    ObjectFilters.notIn("a", true, false),
                    ObjectFilters.notIn("a", "foo", "bar"),
                    // CONJUGATE FILTERS
                    ObjectFilters.not(ObjectFilters.eq("a", 42)),
                    ObjectFilters.and(ObjectFilters.eq("a", 42), ObjectFilters.gt("a", 42), ObjectFilters.gte("a", 42)),
                    ObjectFilters.not(ObjectFilters.and(ObjectFilters.eq("a", 42), ObjectFilters.gt("a", 42))),
                    ObjectFilters.and(ObjectFilters.not(ObjectFilters.eq("a", 42)), ObjectFilters.gt("a", 42)),
                    ObjectFilters.and(ObjectFilters.eq("a", 42), ObjectFilters.not(ObjectFilters.gt("a", 42))),
                    ObjectFilters.or(ObjectFilters.eq("a", 42), ObjectFilters.gt("a", 42), ObjectFilters.gte("a", 42)),
                    ObjectFilters.not(ObjectFilters.or(ObjectFilters.eq("a", 42), ObjectFilters.gt("a", 42))),
                    ObjectFilters.or(ObjectFilters.not(ObjectFilters.eq("a", 42)), ObjectFilters.gt("a", 42)),
                    ObjectFilters.or(ObjectFilters.eq("a", 42), ObjectFilters.not(ObjectFilters.gt("a", 42))),
            };

            Streams.forEachPair(
                    Arrays.stream(of),
                    Arrays.stream(nof),
                    (expected, actual) -> assertTrue(expected.toString(), EqualsBuilder.reflectionEquals(expected, db.getFilter(actual), false, null, true))
            );

        }

    }

    @Test
    public void testCRUD() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, NitriteTestEntry.class)) {
            List<NitriteTestEntry> in = new ArrayList<>(Arrays.asList(
                    new NitriteTestEntry("A"),
                    new NitriteTestEntry("B"),
                    new NitriteTestEntry("C")
            ));

            assertEquals("insert all", 3, db.insertAll(in));
            List<NitriteTestEntry> inPrefix = Lists.newArrayList(in.subList(0, 2));
            List<NitriteTestEntry> inSuffix = Lists.newArrayList(in.subList(1, 3));
            List<NitriteTestEntry> inMid = Lists.newArrayList(in.subList(1, 2));

            List<NitriteTestEntry> ascFindAll = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", NoSQLDatabase.SortOrder.ASCENDING));
            List<NitriteTestEntry> descFindAll = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", NoSQLDatabase.SortOrder.DESCENDING));
            Collections.reverse(descFindAll);
            List<NitriteTestEntry> ascFindAllOff0 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, 0, 2, "name", NoSQLDatabase.SortOrder.ASCENDING));
            List<NitriteTestEntry> descFindAllOff0 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, 0, 2, "name", NoSQLDatabase.SortOrder.DESCENDING));
            Collections.reverse(descFindAllOff0);
            List<NitriteTestEntry> ascFindAllOff1 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, 1, 2, "name", NoSQLDatabase.SortOrder.ASCENDING));
            List<NitriteTestEntry> descFindAllOff1 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, 1, 2, "name", NoSQLDatabase.SortOrder.DESCENDING));
            Collections.reverse(descFindAllOff1);

            assertTrue("ascending findAll", EqualsBuilder.reflectionEquals(in, ascFindAll, false, null, true));
            assertTrue("descending findAll", EqualsBuilder.reflectionEquals(in, descFindAll, false, null, true));
            assertTrue("ascending findAll offset 0 limit 2", EqualsBuilder.reflectionEquals(inPrefix, ascFindAllOff0, false, null, true));
            assertTrue("descending findAll offset 0 limit 2", EqualsBuilder.reflectionEquals(inSuffix, descFindAllOff0, false, null, true));
            assertTrue("ascending findAll offset 1 limit 2", EqualsBuilder.reflectionEquals(inSuffix, ascFindAllOff1, false, null, true));
            assertTrue("descending findAll offset 1 limit 2", EqualsBuilder.reflectionEquals(inPrefix, descFindAllOff1, false, null, true));

            assertEquals("count name == A", 1, db.count(new NoSQLFilter().eq("name", "A"), NitriteTestEntry.class));
            assertEquals("count name < C", 2, db.count(new NoSQLFilter().lt("name", "C"), NitriteTestEntry.class));
            assertEquals("count name in A, B", 2, db.count(new NoSQLFilter().in("name", "A", "B"), NitriteTestEntry.class));
            assertEquals("count name in A, B", 2, db.count(new NoSQLFilter().in("name", "A", "B"), NitriteTestEntry.class));
            assertEquals("count name in A, B limit 1", 2, db.count(new NoSQLFilter().in("name", "A", "B"), NitriteTestEntry.class, 0, 1));

            assertEquals("count all", 3, db.countAll(NitriteTestEntry.class));

            assertEquals("remove", 1, db.remove(in.get(0)));
            List<NitriteTestEntry> ascFindAllDel0 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", NoSQLDatabase.SortOrder.ASCENDING));
            assertEquals("remove all", 1, db.removeAll(new NoSQLFilter().eq("name", "C"), NitriteTestEntry.class));
            List<NitriteTestEntry> ascFindAllDel2 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", NoSQLDatabase.SortOrder.ASCENDING));

            assertTrue("remove object", EqualsBuilder.reflectionEquals(inSuffix, ascFindAllDel0, false, null, true));
            assertTrue("remove name == C", EqualsBuilder.reflectionEquals(inMid, ascFindAllDel2, false, null, true));

        }
    }

    @Test
    public void testJoin() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        NitriteTestEntry parent = new NitriteTestEntry("parent");

        try (NitriteDatabase db = new NitriteDatabase(file, NitriteTestEntry.class, NitriteChildTestEntry.class)) {

            db.insert(parent);

            List<NitriteChildTestEntry> children = new ArrayList<>(Arrays.asList(
                    new NitriteChildTestEntry("A", parent.id),
                    new NitriteChildTestEntry("B", parent.id),
                    new NitriteChildTestEntry("C", parent.id)
            ));

            db.insertAll(children);

            Iterable<NitriteTestEntry> outParent = db.findAll(NitriteTestEntry.class);
            Iterable<NitriteChildTestEntry> outChildren = db.findAll(NitriteChildTestEntry.class);

            assertEquals("1 parent", 1, Lists.newArrayList(outParent).size());
            assertTrue("parent okay", EqualsBuilder.reflectionEquals(parent, outParent.iterator().next(), false, null, true));

            List<NitriteFamilyTestEntry> results = Lists.newArrayList(db.joinChildren(NitriteFamilyTestEntry.class, NitriteTestEntry.class, NitriteChildTestEntry.class, outParent, "parentId", "children"));

            assertEquals("1 joined parent", 1, results.size());

            assertEquals("joined parent okay", Lists.newArrayList(outParent).get(0).id, results.get(0).id);
            assertEquals("joined parent okay", Lists.newArrayList(outParent).get(0).name, results.get(0).name);

            assertTrue("joined children okay", EqualsBuilder.reflectionEquals(
                    Lists.newArrayList(outChildren),
                    results.get(0).children, false, null, true
            ));

        }

    }

    @Test
    public void testConcurrency() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        List<NitriteTestEntry> entries = Lists.newArrayList(IntStream.range(0, 100).mapToObj((int num) -> new NitriteTestEntry(Integer.toString(num))).collect(Collectors.toList()));

        try (NitriteDatabase db = new NitriteDatabase(file, NitriteTestEntry.class)) {
            List<Callable<Void>> jobs = entries.stream().map((NitriteTestEntry entry) -> (Callable<Void>) () -> {
                assertEquals("insert", 1, db.insert(entry));
                return null;
            }).collect(Collectors.toList());

            ExecutorService executorService = Executors.newFixedThreadPool(10);
            try {
                List<Future<Void>> futures = executorService.invokeAll(jobs);
                for (Future<Void> future : futures) {
                    future.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            } finally {
                executorService.shutdown();
            }

            List<NitriteTestEntry> out = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", NoSQLDatabase.SortOrder.ASCENDING));

            assertTrue("concurrent insert", EqualsBuilder.reflectionEquals(entries, out, false, null, true));
        }
    }

}
