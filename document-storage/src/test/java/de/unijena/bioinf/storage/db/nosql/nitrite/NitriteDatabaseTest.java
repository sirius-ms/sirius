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


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import de.unijena.bioinf.storage.db.nosql.*;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.burningwave.core.assembler.StaticComponentContainer;
import org.dizitart.no2.Document;
import org.dizitart.no2.filters.Filters;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class NitriteDatabaseTest {

    static {
        StaticComponentContainer.Modules.exportAllToAll();
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class NitriteTestEntry {

        @Id
        long primaryKey;

        public String name;
        public DoubleList dlist;
        public double[] darr;
        public String data;

    }

    private static class TestSerializer extends JsonSerializer<NitriteTestEntry> {

        @Override
        public void serialize(NitriteTestEntry value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("primaryKey", value.primaryKey);
            gen.writeStringField("name", value.name + "_S");
            gen.writeObjectField("dlist", value.dlist);
            gen.writeObjectField("darr", value.darr);
            gen.writeEndObject();
        }

    }

    private static class TestDeserializer extends JsonDeserializer<NitriteTestEntry> {

        @Override
        public NitriteTestEntry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            long primaryKey = 0;
            String name = null;
            DoubleList dlist = null;
            double[] darr = null;
            JsonToken jsonToken = p.nextToken();
            while (!jsonToken.isStructEnd()) {
                if (jsonToken == JsonToken.FIELD_NAME) {
                    String fieldName = p.currentName();
                    switch (fieldName) {
                        case "primaryKey":
                            primaryKey = p.nextLongValue(0);
                            break;
                        case "name":
                            name = p.nextTextValue() + "_D";
                            break;
                        case "dlist":
                            p.nextToken();
                            double[] dvalue = p.readValueAs(double[].class);
                            if (dvalue != null)
                                dlist = new DoubleArrayList(dvalue);
                            break;
                        case "darr":
                            p.nextToken();
                            darr = p.readValueAs(double[].class);
                            break;
                    }
                }
                jsonToken = p.nextToken();
            }
            return NitriteTestEntry.builder()
                    .primaryKey(primaryKey)
                    .name(name)
                    .dlist(dlist)
                    .darr(darr)
                    .build();
        }

    }

    private static class DoubleArrayDeserializer extends JsonDeserializer<DoubleList> {
        @Override
        public DoubleList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            double[] dvalue = p.readValueAs(double[].class);
            return dvalue != null ? new DoubleArrayList(dvalue) : null;
        }
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class NitriteChildTestEntry {

        @Id
        public long primaryKey;

        public String name;

        public long parentKey;

    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class NitriteFamilyTestEntry {

        @Id
        public long primaryKey;

        public String name;

        public List<NitriteChildTestEntry> children;

    }

    @Builder @NoArgsConstructor @AllArgsConstructor private static class IntKeyEntry { @Id int pk; }

    @Builder @NoArgsConstructor @AllArgsConstructor private static class LongKeyEntry { @Id Long pk; }

    @Builder @NoArgsConstructor @AllArgsConstructor private static class DoubleKeyEntry { @Id double pk; }

    @Builder @NoArgsConstructor @AllArgsConstructor private static class BigIntKeyEntry { @Id BigInteger pk; }

    @Builder @NoArgsConstructor @AllArgsConstructor private static class BigDecimalKeyEntry { @Id BigDecimal pk; }

    @Builder @NoArgsConstructor @AllArgsConstructor private static class StringKeyEntry { @Id String pk; }

    @Test
    public void testPrimaryKeys() throws IOException {

        assertThrows(IOException.class, () -> {
            Path file = Files.createTempFile("nitrite-test", "");
            file.toFile().deleteOnExit();
            new NitriteDatabase(file, Metadata.build()
                    .addRepository(IntKeyEntry.class)
                    .addPrimaryKeySupplier(IntKeyEntry.class, new Supplier<Long>() {
                        @Override
                        public Long get() {
                            return 1L;
                        }
                    }));
        });

        assertThrows(RuntimeException.class, () -> {
            Path file = Files.createTempFile("nitrite-test", "");
            file.toFile().deleteOnExit();
            NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                    .addRepository(IntKeyEntry.class)
                    .addPrimaryKeySupplier(IntKeyEntry.class, () -> 1L));
            db.insert(IntKeyEntry.builder().build());
        });

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                .addRepository(IntKeyEntry.class)
                .addRepository(LongKeyEntry.class)
                .addRepository(DoubleKeyEntry.class)
                .addRepository(BigIntKeyEntry.class)
                .addRepository(BigDecimalKeyEntry.class)
                .addRepository(StringKeyEntry.class)
        )) {
            assertThrows(IOException.class, () -> {
                db.insert(IntKeyEntry.builder().build());
            });
            assertThrows(IOException.class, () -> {
                db.insertAll(List.of(IntKeyEntry.builder().build()));
            });
            assertEquals(1, db.insert(IntKeyEntry.builder().pk(1).build()));
            assertEquals(1, db.insert(LongKeyEntry.builder().build()));
            assertEquals(1, db.insert(DoubleKeyEntry.builder().build()));
            assertEquals(1, db.insert(BigIntKeyEntry.builder().build()));
            assertEquals(1, db.insert(BigDecimalKeyEntry.builder().build()));
            assertEquals(1, db.insert(StringKeyEntry.builder().build()));
        }

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
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE)).addDeserializer(NitriteTestEntry.class, new TestDeserializer()))) {
            List<NitriteTestEntry> in = new ArrayList<>(Arrays.asList(
                    NitriteTestEntry.builder().name("A").build(),
                    NitriteTestEntry.builder().name("B").build(),
                    NitriteTestEntry.builder().name("C").build()
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
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                .addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE))
                .addSerialization(NitriteTestEntry.class, new TestSerializer(), new TestDeserializer()))) {
            NitriteTestEntry in = NitriteTestEntry.builder().name("A").dlist(DoubleList.of(1, 2, 3)).darr(new double[]{1, 2, 3}).build();
            db.insert(in);
            NitriteTestEntry[] out = Iterables.toArray(db.findAll(NitriteTestEntry.class), NitriteTestEntry.class);
            assertEquals("jackson db size", 1, out.length);
            assertEquals("jackson module used", in.name + "_S_D", out[0].name);
            assertEquals("id assignment", in.primaryKey, out[0].primaryKey);
            assertTrue("id assignment", out[0].primaryKey > 0);
            assertTrue("jackson fastutil", EqualsBuilder.reflectionEquals(in.dlist.toDoubleArray(), out[0].dlist.toDoubleArray()));
            assertTrue("jackson primitive array", EqualsBuilder.reflectionEquals(in.darr, out[0].darr));
        }

        file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE)).addDeserializer(DoubleList.class, new DoubleArrayDeserializer()))) {
            NitriteTestEntry in = NitriteTestEntry.builder().name("A").dlist(DoubleList.of(1, 2, 3)).darr(new double[]{1, 2, 3}).build();
            db.insert(in);
            NitriteTestEntry[] out = Iterables.toArray(db.findAll(NitriteTestEntry.class), NitriteTestEntry.class);
            assertEquals("jackson db size", 1, out.length);
            assertEquals("jackson module used", in.name, out[0].name);
            assertEquals("id assignment", in.primaryKey, out[0].primaryKey);
            assertTrue("id assignment", out[0].primaryKey > 0);
            assertTrue("jackson fastutil", EqualsBuilder.reflectionEquals(in.dlist.toDoubleArray(), out[0].dlist.toDoubleArray()));
            assertTrue("jackson primitive array", EqualsBuilder.reflectionEquals(in.darr, out[0].darr));
        }

    }

    @Test
    public void testJoin() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        NitriteTestEntry parent = NitriteTestEntry.builder().name("parent").build();

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                .addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE))
                .addRepository(NitriteChildTestEntry.class, new Index("name", IndexType.NON_UNIQUE))
                .addDeserializer(DoubleList.class, new DoubleArrayDeserializer())
        )) {

            db.insert(parent);

            List<NitriteChildTestEntry> children = new ArrayList<>(Arrays.asList(
                    NitriteChildTestEntry.builder().name("A").parentKey(parent.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("B").parentKey(parent.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("C").parentKey(parent.primaryKey).build()
            ));

            db.insertAll(children);

            Iterable<NitriteTestEntry> outParent = db.findAll(NitriteTestEntry.class);
            Iterable<NitriteChildTestEntry> outChildren = db.findAll(NitriteChildTestEntry.class);
            Iterable<NitriteChildTestEntry> outChildrenF = db.find(new Filter().or().eq("name", "A").eq("name", "B"), NitriteChildTestEntry.class);

            assertEquals("1 parent", 1, Lists.newArrayList(outParent).size());
            assertTrue("parent okay", EqualsBuilder.reflectionEquals(parent, outParent.iterator().next(), false, null, true));

            List<NitriteFamilyTestEntry> results = Lists.newArrayList(db.joinAllChildren(NitriteFamilyTestEntry.class, NitriteChildTestEntry.class, outParent, "primaryKey", "parentKey", "children"));

            assertEquals("1 joined parent", 1, results.size());

            assertEquals("joined parent okay", parent.primaryKey, results.get(0).primaryKey);
            assertEquals("joined parent okay", parent.name, results.get(0).name);

            assertTrue("joined children okay", EqualsBuilder.reflectionEquals(
                    Lists.newArrayList(outChildren),
                    results.get(0).children, false, null, true
            ));

            NitriteTestEntry parentB = NitriteTestEntry.builder().name("parentB").build();

            db.insert(parentB);

            List<NitriteChildTestEntry> childrenB = new ArrayList<>(Arrays.asList(
                    NitriteChildTestEntry.builder().name("A").parentKey(parentB.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("B").parentKey(parentB.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("C").parentKey(parentB.primaryKey).build()
            ));

            db.insertAll(childrenB);

            results = Lists.newArrayList(db.joinChildren(
                    NitriteFamilyTestEntry.class,
                    NitriteChildTestEntry.class,
                    Filter.build().or().eq("name", "A").eq("name", "B").end(),
                    db.findAll(NitriteTestEntry.class),
                    "primaryKey", "parentKey", "children"));
            results.sort(Comparator.comparing(entry -> entry.name));

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", parent.primaryKey, results.get(0).primaryKey);
            assertEquals("joined filtered parent okay", parent.name, results.get(0).name);

            assertEquals("joined filtered parent okay", parentB.primaryKey, results.get(1).primaryKey);
            assertEquals("joined filtered parent okay", parentB.name, results.get(1).name);

            assertTrue("joined filtered children okay", EqualsBuilder.reflectionEquals(
                    Lists.newArrayList(outChildrenF),
                    results.get(0).children, false, null, true
            ));
            assertTrue("joined filtered children okay", EqualsBuilder.reflectionEquals(
                    Lists.newArrayList(outChildrenF),
                    results.get(1).children, false, null, true
            ));

            results = Lists.newArrayList(db.joinChildren(
                    NitriteFamilyTestEntry.class,
                    NitriteChildTestEntry.class,
                    Filter.build().and().eq("name", "A").eq("name", "B").end(),
                    db.findAll(NitriteTestEntry.class),
                    "primaryKey", "parentKey", "children"));
            results.sort(Comparator.comparing(entry -> entry.name));

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", parent.primaryKey, results.get(0).primaryKey);
            assertEquals("joined filtered parent okay", parent.name, results.get(0).name);

            assertEquals("joined filtered parent okay", parentB.primaryKey, results.get(1).primaryKey);
            assertEquals("joined filtered parent okay", parentB.name, results.get(1).name);

            assertNull("zero joined children", results.get(0).children);
        }

    }

    @Test
    @SuppressWarnings("unchecked")
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

            List<Document> results = Lists.newArrayList(db.joinAllChildren("children", outParent, "_id", "parentId", "children"));

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

            outParent = db.findAll("entries");

            results = Lists.newArrayList(db.joinChildren("children", new Filter().or().eq("name", "A").eq("name", "B"), outParent, "_id", "parentId", "children"));

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", parent.getId(), results.get(0).getId());
            assertEquals("joined filtered parent okay", parent.get("name"), results.get(0).get("name"));

            assertEquals("joined filtered parent okay", parentB.getId(), results.get(1).getId());
            assertEquals("joined filtered parent okay", parentB.get("name"), results.get(1).get("name"));

            List<Document> outJoinedFChildren = Lists.newArrayList((Collection<Document>) results.get(0).get("children"));
            outJoinedFChildren.sort(Comparator.comparing((Document d) -> ((String) d.get("name"))));

            assertEquals("joined filtered children okay", Lists.newArrayList(outChildrenF), outJoinedFChildren);

            outParent = db.findAll("entries");

            results = Lists.newArrayList(db.joinChildren("children", new Filter().and().eq("name", "A").eq("name", "B"), outParent, "_id", "parentId", "children"));

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", parent.getId(), results.get(0).getId());
            assertEquals("joined filtered parent okay", parent.get("name"), results.get(0).get("name"));

            assertEquals("joined filtered parent okay", parentB.getId(), results.get(1).getId());
            assertEquals("joined filtered parent okay", parentB.get("name"), results.get(1).get("name"));

            assertNull("no children", results.get(0).get("children"));
            assertNull("no children", results.get(1).get("children"));
        }

    }

    @Test
    public void testJoinReflection() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        NitriteFamilyTestEntry parent = NitriteFamilyTestEntry.builder().name("parent").build();

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                .addRepository(NitriteFamilyTestEntry.class, new Index("name", IndexType.UNIQUE))
                .addRepository(NitriteChildTestEntry.class, new Index("name", IndexType.NON_UNIQUE)
                ))) {

            db.insert(parent);

            List<NitriteChildTestEntry> children = new ArrayList<>(Arrays.asList(
                    NitriteChildTestEntry.builder().name("A").parentKey(parent.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("B").parentKey(parent.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("C").parentKey(parent.primaryKey).build()
            ));

            db.insertAll(children);

            Iterable<NitriteFamilyTestEntry> outParent = db.findAll(NitriteFamilyTestEntry.class);
            Iterable<NitriteChildTestEntry> outChildren = db.findAll(NitriteChildTestEntry.class);
            Iterable<NitriteChildTestEntry> outChildrenF = db.find(new Filter().or().eq("name", "A").eq("name", "B"), NitriteChildTestEntry.class);

            assertEquals("1 parent", 1, Lists.newArrayList(outParent).size());
            assertTrue("parent okay", EqualsBuilder.reflectionEquals(parent, outParent.iterator().next(), false, null, true));

            List<NitriteFamilyTestEntry> results = Lists.newArrayList(db.joinAllChildren(
                    NitriteChildTestEntry.class,
                    Lists.newArrayList(outParent),
                    "primaryKey", "parentKey", "children"));

            assertEquals("1 joined parent", 1, results.size());

            assertEquals("joined parent okay", parent.primaryKey, results.get(0).primaryKey);
            assertEquals("joined parent okay", parent.name, results.get(0).name);

            assertTrue("joined children okay", EqualsBuilder.reflectionEquals(
                    Lists.newArrayList(outChildren),
                    results.get(0).children, false, null, true
            ));

            NitriteFamilyTestEntry parentB = NitriteFamilyTestEntry.builder().name("parentB").build();

            db.insert(parentB);

            List<NitriteChildTestEntry> childrenB = new ArrayList<>(Arrays.asList(
                    NitriteChildTestEntry.builder().name("A").parentKey(parentB.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("B").parentKey(parentB.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("C").parentKey(parentB.primaryKey).build()
            ));

            db.insertAll(childrenB);

            results = Lists.newArrayList(db.joinChildren(
                    NitriteChildTestEntry.class,
                    Filter.build().or().eq("name", "A").eq("name", "B"),
                    Lists.newArrayList(db.findAll(NitriteFamilyTestEntry.class)),
                    "primaryKey", "parentKey", "children"));
            results.sort(Comparator.comparing(entry -> entry.name));

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", parent.primaryKey, results.get(0).primaryKey);
            assertEquals("joined filtered parent okay", parent.name, results.get(0).name);

            assertEquals("joined filtered parent okay", parentB.primaryKey, results.get(1).primaryKey);
            assertEquals("joined filtered parent okay", parentB.name, results.get(1).name);

            assertTrue("joined filtered children okay", EqualsBuilder.reflectionEquals(
                    Lists.newArrayList(outChildrenF),
                    results.get(0).children, false, null, true
            ));
            assertTrue("joined filtered children okay", EqualsBuilder.reflectionEquals(
                    Lists.newArrayList(outChildrenF),
                    results.get(1).children, false, null, true
            ));

            results = Lists.newArrayList(db.joinChildren(
                    NitriteChildTestEntry.class,
                    Filter.build().and().eq("name", "A").eq("name", "B"),
                    Lists.newArrayList(db.findAll(NitriteFamilyTestEntry.class)),
                    "primaryKey", "parentKey", "children"));
            results.sort(Comparator.comparing(entry -> entry.name));

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", parent.primaryKey, results.get(0).primaryKey);
            assertEquals("joined filtered parent okay", parent.name, results.get(0).name);

            assertEquals("joined filtered parent okay", parentB.primaryKey, results.get(1).primaryKey);
            assertEquals("joined filtered parent okay", parentB.name, results.get(1).name);

            assertNull("no children", results.get(0).children);
            assertNull("no children", results.get(1).children);
        }

    }

    @Test
    public void testConcurrency() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        List<NitriteTestEntry> entries = IntStream.range(0, 100).mapToObj((int num) -> NitriteTestEntry.builder().name(Integer.toString(num)).build()).collect(Collectors.toList());

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE)).addDeserializer(NitriteTestEntry.class, new TestDeserializer()))) {
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

    @Test
    public void testOptionals() throws IOException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE)).setOptionalFields(NitriteTestEntry.class, "data").addDeserializer(DoubleList.class, new DoubleArrayDeserializer()))) {

            NitriteTestEntry object = NitriteTestEntry.builder().name("TEST").data("BIGDATA").build();

            db.insert(object);

            NitriteTestEntry res1 = db.getByPrimaryKey(object.primaryKey, NitriteTestEntry.class).orElseThrow();
            NitriteTestEntry res2 = db.getByPrimaryKey(object.primaryKey, NitriteTestEntry.class,"data").orElseThrow();

            List<NitriteTestEntry> res3 = Lists.newArrayList(db.findAll(NitriteTestEntry.class));
            List<NitriteTestEntry> res4 = Lists.newArrayList(db.findAll(NitriteTestEntry.class, "data"));

            assertNotNull(res1);
            assertNotNull(res2);

            assertEquals("1 document", 1, res3.size());
            assertEquals("1 document", 1, res4.size());

            assertNull("no data", res1.data);
            assertNull("no data", res3.get(0).data);

            assertNotNull("has data", res2.data);
            assertNotNull("has data", res4.get(0).data);

            NitriteTestEntry res5 = db.injectOptionalFields(res1, "data");
            List<NitriteTestEntry> res6 = Lists.newArrayList(db.injectOptionalFields(NitriteTestEntry.class, db.findAll(NitriteTestEntry.class), "data"));
            List<NitriteTestEntry> res7 = Lists.newArrayList(db.injectOptionalFields(NitriteTestEntry.class, Lists.newArrayList(db.findAll(NitriteTestEntry.class)), "data"));

            assertNotNull(res5);

            assertEquals("1 document", 1, res6.size());
            assertEquals("1 document", 1, res7.size());

            assertNotNull("has data", res5.data);
            assertNotNull("has data", res6.get(0).data);
            assertNotNull("has data", res7.get(0).data);

        }

    }

    @Test
    public void testOptionalDocuments() throws IOException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addCollection("test", new Index("name", IndexType.UNIQUE)).setOptionalFields("test", "data"))) {

            Document doc = Document.createDocument("name", "TEST").put("data", "BIGDATA");
            assertNotNull(doc);

            db.insert("test", doc);

            Document res1 = db.getByNitriteId("test", doc.getId()).orElseThrow();
            Document res2 = db.getByNitriteId("test", doc.getId(), "data").orElseThrow();

            List<Document> res3 = Lists.newArrayList(db.findAll("test"));
            List<Document> res4 = Lists.newArrayList(db.findAll("test", "data"));

            assertNotNull(res1);
            assertNotNull(res2);

            assertEquals("1 document", 1, res3.size());
            assertEquals("1 document", 1, res4.size());

            assertFalse("no data", res1.containsKey("data"));
            assertFalse("no data", res3.get(0).containsKey("data"));

            assertTrue("has data", res2.containsKey("data"));
            assertTrue("has data", res4.get(0).containsKey("data"));

            Document res5 = db.injectOptionalFields("test", res1, "data");
            List<Document> res6 = Lists.newArrayList(db.injectOptionalFields("test", db.findAll("test"), "data"));
            List<Document> res7 = Lists.newArrayList(db.injectOptionalFields("test", Lists.newArrayList(db.findAll("test")), "data"));

            assertNotNull(res5);

            assertEquals("1 document", 1, res6.size());
            assertEquals("1 document", 1, res7.size());

            assertTrue("has data", res5.containsKey("data"));
            assertTrue("has data", res6.get(0).containsKey("data"));
            assertTrue("has data", res7.get(0).containsKey("data"));

        }

    }

    @Test
    public void testEvents() throws IOException, InterruptedException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        final BlockingQueue<Long> idQueue = new ArrayBlockingQueue<>(3);

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE)).addDeserializer(NitriteTestEntry.class, new TestDeserializer()))) {
            List<NitriteTestEntry> in = List.of(
                    NitriteTestEntry.builder().name("A").build(),
                    NitriteTestEntry.builder().name("B").build(),
                    NitriteTestEntry.builder().name("C").build()
            );

            Set<Long> insertIds = new HashSet<>();
            Set<Long> updateIds = new HashSet<>();
            Set<Long> removeIds = new HashSet<>();
            db.onInsert(NitriteTestEntry.class, idQueue::add);
            db.onUpdate(NitriteTestEntry.class, idQueue::add);
            db.onRemove(NitriteTestEntry.class, idQueue::add);

            db.insertAll(in);
            for (int i = 0; i < 3; i++) {
                insertIds.add(idQueue.poll(1L, TimeUnit.SECONDS));
            }
            db.upsertAll(in.stream().peek(e -> e.name += "_U").toList());
            for (int i = 0; i < 3; i++) {
                updateIds.add(idQueue.poll(1L, TimeUnit.SECONDS));
            }
            db.removeAll(in);
            for (int i = 0; i < 3; i++) {
                removeIds.add(idQueue.poll(1L, TimeUnit.SECONDS));
            }

            Set<Long> expectedIds = in.stream().map(e -> e.primaryKey).collect(Collectors.toSet());
            Assert.assertEquals(expectedIds, insertIds);
            Assert.assertEquals(expectedIds, updateIds);
            Assert.assertEquals(expectedIds, removeIds);
        }

    }

    @Test
    public void testEventsWithObjects() throws IOException, InterruptedException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        final BlockingQueue<Long> idQueue = new ArrayBlockingQueue<>(3);
        final BlockingQueue<String> nameQueue = new ArrayBlockingQueue<>(3);

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, new Index("name", IndexType.UNIQUE)).addDeserializer(NitriteTestEntry.class, new TestDeserializer()))) {
            List<NitriteTestEntry> in = List.of(
                    NitriteTestEntry.builder().name("A").build(),
                    NitriteTestEntry.builder().name("B").build(),
                    NitriteTestEntry.builder().name("C").build()
            );

            Set<Long> insertIds = new HashSet<>();
            Set<Long> updateIds = new HashSet<>();
            Set<Long> removeIds = new HashSet<>();
            Set<String> inserted = new HashSet<>();
            Set<String> updated = new HashSet<>();
            Set<String> removed = new HashSet<>();

            db.onInsert(NitriteTestEntry.class, (Long id, NitriteTestEntry entry) -> {
                idQueue.add(id);
                nameQueue.add(entry.name);
            });
            db.onUpdate(NitriteTestEntry.class, (Long id, NitriteTestEntry entry) -> {
                idQueue.add(id);
                nameQueue.add(entry.name);
            });
            db.onRemove(NitriteTestEntry.class, (Long id, NitriteTestEntry entry) -> {
                idQueue.add(id);
                nameQueue.add(entry.name);
            });

            db.insertAll(in);
            for (int i = 0; i < 3; i++) {
                insertIds.add(idQueue.poll(1L, TimeUnit.SECONDS));
                inserted.add(nameQueue.poll(1L, TimeUnit.SECONDS));
            }

            Set<String> expectedNames = in.stream().map(e -> e.name + "_D").collect(Collectors.toSet());
            in.forEach(e -> e.name += "_U");
            db.upsertAll(in);
            for (int i = 0; i < 3; i++) {
                updateIds.add(idQueue.poll(1L, TimeUnit.SECONDS));
                updated.add(nameQueue.poll(1L, TimeUnit.SECONDS));
            }

            db.removeAll(in);
            for (int i = 0; i < 3; i++) {
                removeIds.add(idQueue.poll(1L, TimeUnit.SECONDS));
                removed.add(nameQueue.poll(1L, TimeUnit.SECONDS));
            }

            Set<Long> expectedIds = in.stream().map(e -> e.primaryKey).collect(Collectors.toSet());
            Set<String> updatedNames = in.stream().map(e -> e.name + "_D").collect(Collectors.toSet());

            Assert.assertEquals(expectedIds, insertIds);
            Assert.assertEquals(expectedIds, updateIds);
            Assert.assertEquals(expectedIds, removeIds);


            Assert.assertEquals(expectedNames, inserted);
            Assert.assertEquals(updatedNames, updated);
            Assert.assertEquals(updatedNames, removed);
        }

    }

    @Test
    public void testEventsWithDocuments() throws IOException, InterruptedException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        final BlockingQueue<String> nameQueue = new ArrayBlockingQueue<>(3);

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addCollection("entries", new Index("name", IndexType.UNIQUE)))) {
            List<Document> in = Lists.newArrayList(
                    Document.createDocument("name", "A"),
                    Document.createDocument("name", "B"),
                    Document.createDocument("name", "C")
            );

            Set<String> inserted = new HashSet<>();
            Set<String> updated = new HashSet<>();
            Set<String> removed = new HashSet<>();
            db.onInsert("entries", e -> nameQueue.add(e.get("name", String.class)));
            db.onUpdate("entries", e -> nameQueue.add(e.get("name", String.class)));
            db.onRemove("entries", e -> nameQueue.add(e.get("name", String.class)));

            db.insertAll("entries", in);
            for (int i = 0; i < 3; i++) {
                inserted.add(nameQueue.poll(1L, TimeUnit.SECONDS));
            }

            Set<String> expectedNames = in.stream().map(e -> e.get("name", String.class)).collect(Collectors.toSet());
            in.forEach(e -> e.put("name", e.get("name", String.class) + "_U"));
            Set<String> updatedNames = in.stream().map(e -> e.get("name", String.class)).collect(Collectors.toSet());

            db.upsertAll("entries",in);
            for (int i = 0; i < 3; i++) {
                updated.add(nameQueue.poll(1L, TimeUnit.SECONDS));
            }
            db.removeAll("entries", in);
            for (int i = 0; i < 3; i++) {
                removed.add(nameQueue.poll(1L, TimeUnit.SECONDS));
            }

            Assert.assertEquals(expectedNames, inserted);
            Assert.assertEquals(updatedNames, updated);
            Assert.assertEquals(updatedNames, removed);
        }

    }

}
