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


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import de.unijena.bioinf.storage.db.nosql.*;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.dizitart.no2.Document;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.filters.Filters;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NitriteDatabaseTest {

    static {
        if (!EqualsBuilder.class.getModule().isNamed()) {
            ClassLoader.class.getModule().addOpens(ArrayList.class.getPackageName(), EqualsBuilder.class.getModule());
        }
    }

    private static class NitriteTestEntry extends NitritePOJO implements NitriteWriteString {

        public String name;

        public NitriteTestEntry() {
            super();
        }

        public NitriteTestEntry(String name) {
            super();
            this.name = name;
        }

    }

    private static class NitriteNoPOJOTestEntry implements NitriteWriteString {

        Long id = 1L;

        public String name;
        public DoubleList dlist;
        public TDoubleList dtlist;
        public double[] darr;

        public NitriteNoPOJOTestEntry() {
            super();
        }

        public NitriteNoPOJOTestEntry(String name, DoubleList dlist, TDoubleList dtlist, double[] darr) {
            this.name = name;
            this.dlist = dlist;
            this.dtlist = dtlist;
            this.darr = darr;
        }
    }

    private static class TestSerializer extends JsonSerializer<NitriteNoPOJOTestEntry> {

        @Override
        public void serialize(NitriteNoPOJOTestEntry value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name + "_S");
            gen.writeObjectField("dlist", value.dlist);
            gen.writeObjectField("dtlist", value.dtlist.toArray());
            gen.writeObjectField("darr", value.darr);
            gen.writeEndObject();
        }

    }

    private static class TestDeserializer extends JsonDeserializer<NitriteNoPOJOTestEntry> {

        @Override
        public NitriteNoPOJOTestEntry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String name = null;
            DoubleList dlist = null;
            TDoubleList dtlist = null;
            double[] darr = null;
            JsonToken jsonToken = p.nextToken();
            while (!jsonToken.isStructEnd()) {
                if (jsonToken == JsonToken.FIELD_NAME) {
                    String fieldName = p.currentName();
                    switch (fieldName) {
                        case "name":
                            name = p.nextTextValue() + "_D";
                            break;
                        case "dlist":
                            jsonToken = p.nextToken();
                            dlist = new DoubleArrayList(p.readValueAs(double[].class));
                            break;
                        case "dtlist":
                            jsonToken = p.nextToken();
                            dtlist = new TDoubleArrayList(p.readValueAs(double[].class));
                            break;
                        case "darr":
                            jsonToken = p.nextToken();
                            darr = p.readValueAs(double[].class);
                            break;
                    }
                }
                jsonToken = p.nextToken();
            }
            return new NitriteNoPOJOTestEntry(name, dlist, dtlist, darr);
        }

    }

    private static class TestModule extends SimpleModule {

        public TestModule() {
            super("test");

            addSerializer(NitriteNoPOJOTestEntry.class, new TestSerializer());
            addDeserializer(NitriteNoPOJOTestEntry.class, new TestDeserializer());
        }
    }

    private static class NitriteChildTestEntry extends NitritePOJO implements NitriteWriteString {

        public String name;

        public NitriteId parentId;

        public NitriteChildTestEntry() { super(); }

        public NitriteChildTestEntry(String name, NitriteId parentId) {
            super();
            this.name = name;
            this.parentId = parentId;
        }

    }

    private static class NitriteFamilyTestEntry extends NitritePOJO implements NitriteWriteString {

        public String name;

        public List<NitriteChildTestEntry> children;

        public NitriteFamilyTestEntry() {
            super();
        }

    }

    private static class A {

    }

    @Test
    public void testAnno() {

    }

    @Test
    public void testFilters() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build())) {
            Filter[] nof = {
                    // SIMPLE FILTERS
                    new Filter().eq("a", 42),
                    new Filter().gt("a", 42),
                    new Filter().gte("a", 42),
                    new Filter().lt("a", 42),
                    new Filter().lte("a", 42),
                    new Filter().text("a", "foo"),
                    new Filter().regex("a", "foo"),
                    new Filter().inByte("a", (byte) 12, (byte) 42),
                    new Filter().inShort("a", (short) 12, (short) 42),
                    new Filter().inInt("a", 12, 42),
                    new Filter().inLong("a", 12L, 42L),
                    new Filter().inFloat("a", 12f, 42f),
                    new Filter().inDouble("a", 12d, 42d),
                    new Filter().inChar("a", 't', 'f'),
                    new Filter().inBool("a", true, false),
                    new Filter().in("a", "foo", "bar"),
                    new Filter().notInByte("a", (byte) 12, (byte) 42),
                    new Filter().notInShort("a", (short) 12, (short) 42),
                    new Filter().notInInt("a", 12, 42),
                    new Filter().notInLong("a", 12L, 42L),
                    new Filter().notInFloat("a", 12f, 42f),
                    new Filter().notInDouble("a", 12d, 42d),
                    new Filter().notInChar("a", 't', 'f'),
                    new Filter().notInBool("a", true, false),
                    new Filter().notIn("a", "foo", "bar"),
                    // CONJUGATE FILTERS
                    new Filter().not().eq("a", 42),
                    new Filter().and().eq("a", 42).gt("a", 42).gte("a", 42),
                    new Filter().not().and().eq("a", 42).gt("a", 42),
                    new Filter().and().not().eq("a", 42).gt("a", 42),
                    new Filter().and().eq("a", 42).not().gt("a", 42),
                    new Filter().or().eq("a", 42).gt("a", 42).gte("a", 42),
                    new Filter().not().or().eq("a", 42).gt("a", 42),
                    new Filter().or().not().eq("a", 42).gt("a", 42),
                    new Filter().or().eq("a", 42).not().gt("a", 42),
                    new Filter().and().or().eq("a", 42).eq("a", 41).end().eq("a", "foo"),
                    new Filter().or().and().eq("a", 42).eq("a", 41).end().eq("a", "foo")
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
                    ObjectFilters.and(ObjectFilters.or(ObjectFilters.eq("a", 42), ObjectFilters.eq("a", 41)), ObjectFilters.eq("a", "foo")),
                    ObjectFilters.or(ObjectFilters.and(ObjectFilters.eq("a", 42), ObjectFilters.eq("a", 41)), ObjectFilters.eq("a", "foo")),
            };

            Streams.forEachPair(
                    Arrays.stream(of),
                    Arrays.stream(nof),
                    (expected, actual) -> assertTrue(expected.toString(), EqualsBuilder.reflectionEquals(expected, db.getObjectFilter(actual), false, null, true))
            );

        }

    }

    @Test
    public void testFiltersDocument() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build())) {
            Filter[] nof = {
                    // SIMPLE FILTERS
                    new Filter().eq("a", 42),
                    new Filter().gt("a", 42),
                    new Filter().gte("a", 42),
                    new Filter().lt("a", 42),
                    new Filter().lte("a", 42),
                    new Filter().text("a", "foo"),
                    new Filter().regex("a", "foo"),
                    new Filter().inByte("a", (byte) 12, (byte) 42),
                    new Filter().inShort("a", (short) 12, (short) 42),
                    new Filter().inInt("a", 12, 42),
                    new Filter().inLong("a", 12L, 42L),
                    new Filter().inFloat("a", 12f, 42f),
                    new Filter().inDouble("a", 12d, 42d),
                    new Filter().inChar("a", 't', 'f'),
                    new Filter().inBool("a", true, false),
                    new Filter().in("a", "foo", "bar"),
                    new Filter().notInByte("a", (byte) 12, (byte) 42),
                    new Filter().notInShort("a", (short) 12, (short) 42),
                    new Filter().notInInt("a", 12, 42),
                    new Filter().notInLong("a", 12L, 42L),
                    new Filter().notInFloat("a", 12f, 42f),
                    new Filter().notInDouble("a", 12d, 42d),
                    new Filter().notInChar("a", 't', 'f'),
                    new Filter().notInBool("a", true, false),
                    new Filter().notIn("a", "foo", "bar"),
                    // CONJUGATE FILTERS
                    new Filter().not().eq("a", 42),
                    new Filter().and().eq("a", 42).gt("a", 42).gte("a", 42),
                    new Filter().not().and().eq("a", 42).gt("a", 42),
                    new Filter().and().not().eq("a", 42).gt("a", 42),
                    new Filter().and().eq("a", 42).not().gt("a", 42),
                    new Filter().or().eq("a", 42).gt("a", 42).gte("a", 42),
                    new Filter().not().or().eq("a", 42).gt("a", 42),
                    new Filter().or().not().eq("a", 42).gt("a", 42),
                    new Filter().or().eq("a", 42).not().gt("a", 42),
                    new Filter().and().or().eq("a", 42).eq("a", 41).end().eq("a", "foo"),
                    new Filter().or().and().eq("a", 42).eq("a", 41).end().eq("a", "foo")
            };

            org.dizitart.no2.Filter[] of = {
                    // SIMPLE FILTERS
                    Filters.eq("a", 42),
                    Filters.gt("a", 42),
                    Filters.gte("a", 42),
                    Filters.lt("a", 42),
                    Filters.lte("a", 42),
                    Filters.text("a", "foo"),
                    Filters.regex("a", "foo"),
                    Filters.in("a", (byte) 12, (byte) 42),
                    Filters.in("a", (short) 12, (short) 42),
                    Filters.in("a", 12, 42),
                    Filters.in("a", 12L, 42L),
                    Filters.in("a", 12f, 42f),
                    Filters.in("a", 12d, 42d),
                    Filters.in("a", 't', 'f'),
                    Filters.in("a", true, false),
                    Filters.in("a", "foo", "bar"),
                    Filters.notIn("a", (byte) 12, (byte) 42),
                    Filters.notIn("a", (short) 12, (short) 42),
                    Filters.notIn("a", 12, 42),
                    Filters.notIn("a", 12L, 42L),
                    Filters.notIn("a", 12f, 42f),
                    Filters.notIn("a", 12d, 42d),
                    Filters.notIn("a", 't', 'f'),
                    Filters.notIn("a", true, false),
                    Filters.notIn("a", "foo", "bar"),
                    // CONJUGATE FILTERS
                    Filters.not(Filters.eq("a", 42)),
                    Filters.and(Filters.eq("a", 42), Filters.gt("a", 42), Filters.gte("a", 42)),
                    Filters.not(Filters.and(Filters.eq("a", 42), Filters.gt("a", 42))),
                    Filters.and(Filters.not(Filters.eq("a", 42)), Filters.gt("a", 42)),
                    Filters.and(Filters.eq("a", 42), Filters.not(Filters.gt("a", 42))),
                    Filters.or(Filters.eq("a", 42), Filters.gt("a", 42), Filters.gte("a", 42)),
                    Filters.not(Filters.or(Filters.eq("a", 42), Filters.gt("a", 42))),
                    Filters.or(Filters.not(Filters.eq("a", 42)), Filters.gt("a", 42)),
                    Filters.or(Filters.eq("a", 42), Filters.not(Filters.gt("a", 42))),
                    Filters.and(Filters.or(Filters.eq("a", 42), Filters.eq("a", 41)), Filters.eq("a", "foo")),
                    Filters.or(Filters.and(Filters.eq("a", 42), Filters.eq("a", 41)), Filters.eq("a", "foo")),
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
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE)))) {
            List<NitriteTestEntry> in = new ArrayList<>(Arrays.asList(
                    new NitriteTestEntry("A"),
                    new NitriteTestEntry("B"),
                    new NitriteTestEntry("C")
            ));

            assertEquals("insert all", 3, db.insertAll(in));
            List<NitriteTestEntry> inPrefix = Lists.newArrayList(in.subList(0, 2));
            List<NitriteTestEntry> inSuffix = Lists.newArrayList(in.subList(1, 3));
            List<NitriteTestEntry> inMid = Lists.newArrayList(in.subList(1, 2));

            List<NitriteTestEntry> ascFindAll = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", Database.SortOrder.ASCENDING));
            List<NitriteTestEntry> descFindAll = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", Database.SortOrder.DESCENDING));
            Collections.reverse(descFindAll);
            List<NitriteTestEntry> ascFindAllOff0 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, 0, 2, "name", Database.SortOrder.ASCENDING));
            List<NitriteTestEntry> descFindAllOff0 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, 0, 2, "name", Database.SortOrder.DESCENDING));
            Collections.reverse(descFindAllOff0);
            List<NitriteTestEntry> ascFindAllOff1 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, 1, 2, "name", Database.SortOrder.ASCENDING));
            List<NitriteTestEntry> descFindAllOff1 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, 1, 2, "name", Database.SortOrder.DESCENDING));
            Collections.reverse(descFindAllOff1);

            assertTrue("ascending findAll", EqualsBuilder.reflectionEquals(in, ascFindAll, false, null, true));
            assertTrue("descending findAll", EqualsBuilder.reflectionEquals(in, descFindAll, false, null, true));
            assertTrue("ascending findAll offset 0 limit 2", EqualsBuilder.reflectionEquals(inPrefix, ascFindAllOff0, false, null, true));
            assertTrue("descending findAll offset 0 limit 2", EqualsBuilder.reflectionEquals(inSuffix, descFindAllOff0, false, null, true));
            assertTrue("ascending findAll offset 1 limit 2", EqualsBuilder.reflectionEquals(inSuffix, ascFindAllOff1, false, null, true));
            assertTrue("descending findAll offset 1 limit 2", EqualsBuilder.reflectionEquals(inPrefix, descFindAllOff1, false, null, true));

            assertEquals("count name == A", 1, db.count(new Filter().eq("name", "A"), NitriteTestEntry.class));
            assertEquals("count name < C", 2, db.count(new Filter().lt("name", "C"), NitriteTestEntry.class));
            assertEquals("count name in A, B", 2, db.count(new Filter().in("name", "A", "B"), NitriteTestEntry.class));
            assertEquals("count name in A, B", 2, db.count(new Filter().in("name", "A", "B"), NitriteTestEntry.class));
            assertEquals("count name in A, B limit 1", 1, db.count(new Filter().in("name", "A", "B"), NitriteTestEntry.class, 0, 1));

            assertEquals("count all", 3, db.countAll(NitriteTestEntry.class));

            assertEquals("remove", 1, db.remove(in.get(0)));
            List<NitriteTestEntry> ascFindAllDel0 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", Database.SortOrder.ASCENDING));
            assertEquals("remove name == C", 1, db.removeAll(new Filter().eq("name", "C"), NitriteTestEntry.class));
            List<NitriteTestEntry> ascFindAllDel2 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", Database.SortOrder.ASCENDING));

            assertTrue("remove object", EqualsBuilder.reflectionEquals(inSuffix, ascFindAllDel0, false, null, true));
            assertTrue("remove name == C", EqualsBuilder.reflectionEquals(inMid, ascFindAllDel2, false, null, true));

        }
    }

    @Test
    public void testCRUDDocuments() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addCollection("entries", new Index("name", IndexType.NON_UNIQUE)))) {
            List<Document> in = Lists.newArrayList(
                    Document.createDocument("name", "A"),
                    Document.createDocument("name", "B"),
                    Document.createDocument("name", "C")
            );

            assertEquals("insert all", 3, db.insertAll("entries", in));
            List<Document> inPrefix = Lists.newArrayList(in.subList(0, 2));
            List<Document> inSuffix = Lists.newArrayList(in.subList(1, 3));
            List<Document> inMid = Lists.newArrayList(in.subList(1, 2));

            List<Document> ascFindAll = Lists.newArrayList(db.findAll("entries", "name", Database.SortOrder.ASCENDING));
            List<Document> descFindAll = Lists.newArrayList(db.findAll("entries", "name", Database.SortOrder.DESCENDING));
            Collections.reverse(descFindAll);
            List<Document> ascFindAllOff0 = Lists.newArrayList(db.findAll("entries", 0, 2, "name", Database.SortOrder.ASCENDING));
            List<Document> descFindAllOff0 = Lists.newArrayList(db.findAll("entries", 0, 2, "name", Database.SortOrder.DESCENDING));
            Collections.reverse(descFindAllOff0);
            List<Document> ascFindAllOff1 = Lists.newArrayList(db.findAll("entries", 1, 2, "name", Database.SortOrder.ASCENDING));
            List<Document> descFindAllOff1 = Lists.newArrayList(db.findAll("entries", 1, 2, "name", Database.SortOrder.DESCENDING));
            Collections.reverse(descFindAllOff1);

            assertTrue("ascending findAll", EqualsBuilder.reflectionEquals(in, ascFindAll, false, null, true));
            assertTrue("descending findAll", EqualsBuilder.reflectionEquals(in, descFindAll, false, null, true));
            assertTrue("ascending findAll offset 0 limit 2", EqualsBuilder.reflectionEquals(inPrefix, ascFindAllOff0, false, null, true));
            assertTrue("descending findAll offset 0 limit 2", EqualsBuilder.reflectionEquals(inSuffix, descFindAllOff0, false, null, true));
            assertTrue("ascending findAll offset 1 limit 2", EqualsBuilder.reflectionEquals(inSuffix, ascFindAllOff1, false, null, true));
            assertTrue("descending findAll offset 1 limit 2", EqualsBuilder.reflectionEquals(inPrefix, descFindAllOff1, false, null, true));

            assertEquals("count name == A", 1, db.count("entries", new Filter().eq("name", "A")));
            assertEquals("count name < C", 2, db.count("entries", new Filter().lt("name", "C")));
            assertEquals("count name in A, B", 2, db.count("entries", new Filter().in("name", "A", "B")));
            assertEquals("count name in A, B", 2, db.count("entries", new Filter().in("name", "A", "B")));
            assertEquals("count name in A, B limit 1", 1, db.count("entries", new Filter().in("name", "A", "B"), 0, 1));

            assertEquals("count all", 3, db.countAll("entries"));

            assertEquals("remove", 1, db.remove("entries", in.get(0)));
            List<Document> ascFindAllDel0 = Lists.newArrayList(db.findAll("entries", "name", Database.SortOrder.ASCENDING));
            assertEquals("remove name == C", 1, db.removeAll("entries", new Filter().eq("name", "C")));
            List<Document> ascFindAllDel2 = Lists.newArrayList(db.findAll("entries", "name", Database.SortOrder.ASCENDING));

            assertTrue("remove object", EqualsBuilder.reflectionEquals(inSuffix, ascFindAllDel0, false, null, true));
            assertTrue("remove name == C", EqualsBuilder.reflectionEquals(inMid, ascFindAllDel2, false, null, true));

        }
    }

    @Test
    public void testJackson() throws IOException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteNoPOJOTestEntry.class, "id", new TestSerializer(), new TestDeserializer(), new Index("name", IndexType.UNIQUE)))) {
            NitriteNoPOJOTestEntry in = new NitriteNoPOJOTestEntry("A", DoubleList.of(1, 2, 3), new TDoubleArrayList(new double[]{1, 2, 3}), new double[]{1, 2, 3});
            db.insert(in);
            NitriteNoPOJOTestEntry[] out = Iterables.toArray(db.findAll(NitriteNoPOJOTestEntry.class), NitriteNoPOJOTestEntry.class);
            assertEquals("jackson db size", 1, out.length);
            assertEquals("jackson module used", in.name + "_S_D", out[0].name);
            assertEquals("jackson id mapping", 1L, (long) out[0].id);
            assertTrue("jackson fastutil", EqualsBuilder.reflectionEquals(in.dlist.toDoubleArray(), out[0].dlist.toDoubleArray()));
            assertTrue("jackson trove", EqualsBuilder.reflectionEquals(in.dtlist, out[0].dtlist));
            assertTrue("jackson primitive array", EqualsBuilder.reflectionEquals(in.darr, out[0].darr));
        }

    }

    @Test
    public void testJoin() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        NitriteTestEntry parent = new NitriteTestEntry("parent");

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                .addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE))
                .addRepository(NitriteChildTestEntry.class, new Index("name", IndexType.NON_UNIQUE)
        ))) {

            db.insert(parent);

            List<NitriteChildTestEntry> children = new ArrayList<>(Arrays.asList(
                    new NitriteChildTestEntry("A", parent.getId()),
                    new NitriteChildTestEntry("B", parent.getId()),
                    new NitriteChildTestEntry("C", parent.getId())
            ));

            db.insertAll(children);

            Iterable<NitriteTestEntry> outParent = db.findAll(NitriteTestEntry.class);
            Iterable<NitriteChildTestEntry> outChildren = db.findAll(NitriteChildTestEntry.class);
            Iterable<NitriteChildTestEntry> outChildrenF = db.find(new Filter().or().eq("name", "A").eq("name", "B"), NitriteChildTestEntry.class);

            assertEquals("1 parent", 1, Lists.newArrayList(outParent).size());
            assertTrue("parent okay", EqualsBuilder.reflectionEquals(parent, outParent.iterator().next(), false, null, true));

            List<NitriteFamilyTestEntry> results = Lists.newArrayList(db.joinAllChildren(NitriteFamilyTestEntry.class, NitriteTestEntry.class, NitriteChildTestEntry.class, outParent, "parentId", "children"));

            assertEquals("1 joined parent", 1, results.size());

            assertEquals("joined parent okay", Lists.newArrayList(outParent).get(0).getId(), results.get(0).getId());
            assertEquals("joined parent okay", Lists.newArrayList(outParent).get(0).name, results.get(0).name);

            assertTrue("joined children okay", EqualsBuilder.reflectionEquals(
                    Lists.newArrayList(outChildren),
                    results.get(0).children, false, null, true
            ));

            NitriteTestEntry parentB = new NitriteTestEntry("parentB");

            db.insert(parentB);

            List<NitriteChildTestEntry> childrenB = new ArrayList<>(Arrays.asList(
                    new NitriteChildTestEntry("A", parentB.getId()),
                    new NitriteChildTestEntry("B", parentB.getId()),
                    new NitriteChildTestEntry("C", parentB.getId())
            ));

            db.insertAll(childrenB);

            results = Lists.newArrayList(db.joinChildren(NitriteFamilyTestEntry.class, NitriteTestEntry.class, NitriteChildTestEntry.class, new Filter().or().eq("name", "A").eq("name", "B"), List.of(parent, parentB), "parentId", "children"));

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", Lists.newArrayList(outParent).get(0).getId(), results.get(0).getId());
            assertEquals("joined filtered parent okay", Lists.newArrayList(outParent).get(0).name, results.get(0).name);

            assertEquals("joined filtered parent okay", parentB.getId(), results.get(1).getId());
            assertEquals("joined filtered parent okay", parentB.name, results.get(1).name);

            assertTrue("joined filtered children okay", EqualsBuilder.reflectionEquals(
                    Lists.newArrayList(outChildrenF),
                    results.get(0).children, false, null, true
            ));

            results = Lists.newArrayList(db.joinChildren(NitriteFamilyTestEntry.class, NitriteTestEntry.class, NitriteChildTestEntry.class, new Filter().and().eq("name", "A").eq("name", "B"), List.of(parent, parentB), "parentId", "children"));

            assertEquals("0 joined filtered parent", 0, results.size());
        }

    }

    @Test
    public void testJoinDocuments() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        Document parent = Document.createDocument("name", "parent");

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                .addCollection("entries", new Index("name", IndexType.NON_UNIQUE))
                .addCollection("children", new Index("name", IndexType.NON_UNIQUE))
        )) {

            db.insert("entries", parent);

            List<Document> children = Lists.newArrayList(
                    Document.createDocument("name", "A").put("parentId", parent.getId().getIdValue()),
                    Document.createDocument("name", "B").put("parentId", parent.getId().getIdValue()),
                    Document.createDocument("name", "C").put("parentId", parent.getId().getIdValue())
            );

            db.insertAll("children", children);

            Iterable<Document> outParent = db.findAll("entries");
            Iterable<Document> outChildren = db.findAll("children");
            Iterable<Document> outChildrenF = db.find("children", new Filter().or().eq("name", "A").eq("name", "B"));

            assertEquals("1 parent", 1, Lists.newArrayList(outParent).size());
            assertEquals("parent okay", parent, outParent.iterator().next());

            List<Document> results = Lists.newArrayList(db.joinAllChildren("entries", "children", outParent, "parentId", "children"));

            assertEquals("1 joined parent", 1, results.size());

            assertEquals("joined parent okay", Lists.newArrayList(outParent).get(0).getId(), results.get(0).getId());
            assertEquals("joined parent okay", Lists.newArrayList(outParent).get(0).get("name"), results.get(0).get("name"));

            List<Document> outJoinedChildren = Lists.newArrayList((Collection<Document>) results.get(0).get("children"));
            outJoinedChildren.sort(Comparator.comparing((Document d) -> ((String) d.get("name"))));

            assertEquals("joined children okay", Lists.newArrayList(outChildren), outJoinedChildren);

            Document parentB = Document.createDocument("name", "parentB");

            db.insert("entries", parentB);

            List<Document> childrenB = Lists.newArrayList(
                    Document.createDocument("name", "A").put("parentId", parentB.getId().getIdValue()),
                    Document.createDocument("name", "B").put("parentId", parentB.getId().getIdValue()),
                    Document.createDocument("name", "C").put("parentId", parentB.getId().getIdValue())
            );

            db.insertAll("children", childrenB);

            results = Lists.newArrayList(db.joinChildren("entries", "children", new Filter().or().eq("name", "A").eq("name", "B"), List.of(parent, parentB), "parentId", "children"));

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", Lists.newArrayList(outParent).get(0).getId(), results.get(0).getId());
            assertEquals("joined filtered parent okay", Lists.newArrayList(outParent).get(0).get("name"), results.get(0).get("name"));

            assertEquals("joined filtered parent okay", parentB.getId(), results.get(1).getId());
            assertEquals("joined filtered parent okay", parentB.get("name"), results.get(1).get("name"));

            List<Document> outJoinedFChildren = Lists.newArrayList((Collection<Document>) results.get(0).get("children"));
            outJoinedFChildren.sort(Comparator.comparing((Document d) -> ((String) d.get("name"))));

            assertEquals("joined filtered children okay", Lists.newArrayList(outChildrenF), outJoinedFChildren);


            results = Lists.newArrayList(db.joinChildren("entries", "children", new Filter().and().eq("name", "A").eq("name", "B"), List.of(parent, parentB), "parentId", "children"));

            assertEquals("0 joined filtered parent", 0, results.size());
        }

    }

    @Test
    public void testConcurrency() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        List<NitriteTestEntry> entries = IntStream.range(0, 100).mapToObj((int num) -> new NitriteTestEntry(Integer.toString(num))).collect(Collectors.toList());

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE)))) {
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

            List<NitriteTestEntry> out = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "name", Database.SortOrder.ASCENDING));

            assertTrue("concurrent insert", EqualsBuilder.reflectionEquals(entries, out, false, null, true));
        }
    }

}
