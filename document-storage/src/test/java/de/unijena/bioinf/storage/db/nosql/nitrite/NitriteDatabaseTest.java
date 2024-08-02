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
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import jakarta.persistence.Id;
import lombok.*;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.filters.FluentFilter;
import org.dizitart.no2.filters.NitriteFilter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.*;

public class NitriteDatabaseTest {


    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
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
            Filter[] f = {
                    // SIMPLE FILTERS
                    Filter.where("a").eq(42),
                    Filter.where("a").notEq(42),
                    Filter.where("a").gt(42),
                    Filter.where("a").gte(42),
                    Filter.where("a").lt(42),
                    Filter.where("a").lte(42),
                    Filter.where("a").text("foo"),
                    Filter.where("a").regex("foo"),
                    Filter.where("a").in("foo", "bar"),
                    Filter.where("a").notIn("foo", "bar"),
                    Filter.where("a").beetween(41, 43),
                    Filter.where("a").beetweenLeftInclusive(41, 43),
                    Filter.where("a").beetweenRightInclusive(41, 43),
                    Filter.where("a").beetweenBothInclusive(41, 43),
                    Filter.where("a").elemMatch().eq(42),
                    Filter.where("a").elemMatch().elemMatch().eq(42),
                    // CONJUGATE FILTERS
                    Filter.and(Filter.where("a").eq(42), Filter.where("a").gt(42), Filter.where("a").gte(42)),
                    Filter.or(Filter.where("a").eq(42), Filter.where("a").gt(42), Filter.where("a").gte(42)),
                    Filter.and(Filter.where("a").eq("foo"), Filter.or(Filter.where("a").eq(42), Filter.where("a").eq(41))),
                    Filter.or(Filter.where("a").eq("foo"), Filter.and(Filter.where("a").eq(42), Filter.where("a").eq(41))),
                    Filter.and(Filter.where("a").eq("foo"), Filter.where("b").elemMatch().lt(42)),
                    Filter.where("a").elemMatchAnd(Filter.where$().gt(41), Filter.where$().lt(43)),
                    Filter.where("a").elemMatchOr(Filter.where$().lte(41), Filter.where$().gte(43))
            };
            NitriteFilter[] actual = Arrays.stream(f).map(db::getFilter).toArray(NitriteFilter[]::new);

            NitriteFilter[] expected = {
                    // SIMPLE FILTERS
                    FluentFilter.where("a").eq(42),
                    FluentFilter.where("a").notEq(42),
                    FluentFilter.where("a").gt(42),
                    FluentFilter.where("a").gte(42),
                    FluentFilter.where("a").lt(42),
                    FluentFilter.where("a").lte(42),
                    FluentFilter.where("a").text("foo"),
                    FluentFilter.where("a").regex("foo"),
                    FluentFilter.where("a").in("foo", "bar"),
                    FluentFilter.where("a").notIn("foo", "bar"),
                    FluentFilter.where("a").between(41, 43, false),
                    FluentFilter.where("a").between(41, 43, true, false),
                    FluentFilter.where("a").between(41, 43, false, true),
                    FluentFilter.where("a").between(41, 43),
                    FluentFilter.where("a").elemMatch(FluentFilter.$.eq(42)),
                    FluentFilter.where("a").elemMatch(FluentFilter.$.elemMatch(FluentFilter.$.eq(42))),
//                    // CONJUGATE FILTERS
                    (NitriteFilter) FluentFilter.where("a").eq(42).and(FluentFilter.where("a").gt(42).and(FluentFilter.where("a").gte(42))),
                    (NitriteFilter) FluentFilter.where("a").eq(42).or(FluentFilter.where("a").gt(42).or(FluentFilter.where("a").gte(42))),
                    (NitriteFilter) FluentFilter.where("a").eq("foo").and(FluentFilter.where("a").eq(42).or(FluentFilter.where("a").eq(41))),
                    (NitriteFilter) FluentFilter.where("a").eq("foo").or(FluentFilter.where("a").eq(42).and(FluentFilter.where("a").eq(41))),
                    (NitriteFilter) FluentFilter.where("a").eq("foo").and(FluentFilter.where("b").elemMatch(FluentFilter.$.lt(42))),
                    FluentFilter.where("a").elemMatch(FluentFilter.$.gt(41).and(FluentFilter.$.lt(43))),
                    FluentFilter.where("a").elemMatch(FluentFilter.$.lte(41).or(FluentFilter.$.gte(43)))
            };

            for (int i = 0; i < actual.length; i++) {
                assertEquals(expected[i].toString(), actual[i].toString());
            }

        }

    }

    @Test
    public void testCRUD() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, Index.unique("name")).addDeserializer(NitriteTestEntry.class, new TestDeserializer()))) {
            List<NitriteTestEntry> in = new ArrayList<>(Arrays.asList(
                    NitriteTestEntry.builder().name("A").build(),
                    NitriteTestEntry.builder().name("B").build(),
                    NitriteTestEntry.builder().name("C").build()
            ));

            assertEquals("insert all", 3, db.insertAll(in));

            List<String> expected = in.stream().map(e -> e.name + "_D").toList();

            List<String> ascFindAll = db.findAllStr(NitriteTestEntry.class, "name", Database.SortOrder.ASCENDING).map(e -> e.name).toList();
            List<String> descFindAll = db.findAllStr(NitriteTestEntry.class, "name", Database.SortOrder.DESCENDING).map(e -> e.name).toList();
            List<String> ascFindAllLimit2 = db.findAllStr(NitriteTestEntry.class, 0, 2, "name", Database.SortOrder.ASCENDING).map(e -> e.name).toList();
            List<String> ascFindAllOffset1Limit2 = db.findAllStr(NitriteTestEntry.class, 1, 2, "name", Database.SortOrder.ASCENDING).map(e -> e.name).toList();
            List<String> ascFindAllOffset1Limit1 = db.findAllStr(NitriteTestEntry.class, 1, 1, "name", Database.SortOrder.ASCENDING).map(e -> e.name).toList();

            assertEquals("ascending findAll", expected, ascFindAll);
            assertEquals("descending findAll", expected.reversed(), descFindAll);
            assertEquals("ascending findAll offset 0 limit 2", expected.subList(0, 2), ascFindAllLimit2);
            assertEquals("ascending findAll offset 1 limit 2", expected.subList(1, 3), ascFindAllOffset1Limit2);
            assertEquals("ascending findAll offset 1 limit 1", expected.subList(1, 2), ascFindAllOffset1Limit1);

            assertEquals("count name == A", 1, db.count(Filter.where("name").eq("A"), NitriteTestEntry.class));
            assertEquals("count name < C", 2, db.count(Filter.where("name").lt("C"), NitriteTestEntry.class));
            assertEquals("count name in A, B", 2, db.count(Filter.where("name").in("A", "B"), NitriteTestEntry.class));
            assertEquals("count name in A, B limit 1", 1, db.count(Filter.where("name").in("A", "B"), NitriteTestEntry.class, 0, 1));

            assertEquals("count all", 3, db.countAll(NitriteTestEntry.class));

            { //test upsert and modify
                in.getFirst().setName("E");
                assertEquals("Update one", 1, db.upsert(in.getFirst()));
                assertEquals("Check Update Change", "E_D", db.getByPrimaryKey(in.getFirst().primaryKey, NitriteTestEntry.class).map(NitriteTestEntry::getName).orElse(null));
            }

            {
                assertEquals("Update one", 1, db.modify(in.getFirst().getPrimaryKey(), NitriteTestEntry.class, entry -> {entry.setName("A");}));
                assertEquals("Check Modify Change", "A_D", db.getByPrimaryKey(in.getFirst().primaryKey, NitriteTestEntry.class).map(NitriteTestEntry::getName).orElse(null));
            }

            assertEquals("remove", 1, db.remove(in.getFirst()));
            List<String> ascFindAllDel0 = db.findAllStr(NitriteTestEntry.class, "name", Database.SortOrder.ASCENDING).map(e -> e.name).toList();
            assertEquals("remove name == C", 1, db.removeAll(Filter.where("name").eq("C"), NitriteTestEntry.class));
            List<String> ascFindAllDel2 = db.findAllStr(NitriteTestEntry.class, "name", Database.SortOrder.ASCENDING).map(e -> e.name).toList();

            assertEquals("remove object", expected.subList(1, 3), ascFindAllDel0);
            assertEquals("remove name == C", expected.subList(1, 2), ascFindAllDel2);
        }
    }

    @Test
    public void testCRUDDocuments() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addCollection("entries", Index.unique("name")))) {
            List<Document> in = Arrays.asList(
                    Document.createDocument("name", "A"),
                    Document.createDocument("name", "B"),
                    Document.createDocument("name", "C")
            );

            assertEquals("insert all", 3, db.insertAll("entries", in));

            List<String> expected = in.stream().map(e -> e.get("name", String.class)).toList();

            List<String> ascFindAll = db.findAllStr("entries", "name", Database.SortOrder.ASCENDING).map(e -> e.get("name", String.class)).toList();
            List<String> descFindAll = db.findAllStr("entries", "name", Database.SortOrder.DESCENDING).map(e -> e.get("name", String.class)).toList();

            List<String> ascFindAllLimit2 = db.findAllStr("entries", 0, 2, "name", Database.SortOrder.ASCENDING).map(e -> e.get("name", String.class)).toList();
            List<String> ascFindAllOffset1Limit2 = db.findAllStr("entries", 1, 2, "name", Database.SortOrder.ASCENDING).map(e -> e.get("name", String.class)).toList();
            List<String> ascFindAllOffset1Limit1 = db.findAllStr("entries", 1, 1, "name", Database.SortOrder.ASCENDING).map(e -> e.get("name", String.class)).toList();

            assertEquals("ascending findAll", expected, ascFindAll);
            assertEquals("descending findAll", expected.reversed(), descFindAll);
            assertEquals("ascending findAll offset 0 limit 2", expected.subList(0, 2), ascFindAllLimit2);
            assertEquals("ascending findAll offset 1 limit 2", expected.subList(1, 3), ascFindAllOffset1Limit2);
            assertEquals("ascending findAll offset 1 limit 1", expected.subList(1, 2), ascFindAllOffset1Limit1);

            assertEquals("count name == A", 1, db.count("entries", Filter.where("name").eq("A")));
            assertEquals("count name < C", 2, db.count("entries", Filter.where("name").lt("C")));
            assertEquals("count name in A, B", 2, db.count("entries", Filter.where("name").in("A", "B")));
            assertEquals("count name in A, B limit 1", 1, db.count("entries", Filter.where("name").in("A", "B"), 0, 1));

            assertEquals("count all", 3, db.countAll("entries"));

            assertEquals("remove", 1, db.remove("entries", db.findStr("entries", Filter.where("name").eq("A")).findFirst().orElseThrow()));
            List<String> ascFindAllDel0 = db.findAllStr("entries", "name", Database.SortOrder.ASCENDING).map(e -> e.get("name", String.class)).toList();
            assertEquals("remove name == C", 1, db.removeAll("entries", Filter.where("name").eq("C")));
            List<String> ascFindAllDel2 = db.findAllStr("entries", "name", Database.SortOrder.ASCENDING).map(e -> e.get("name", String.class)).toList();

            assertEquals("remove object", expected.subList(1, 3), ascFindAllDel0);
            assertEquals("remove name == C", expected.subList(1, 2), ascFindAllDel2);

        }
    }

    @Test
    public void testJackson() throws IOException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                .addRepository(NitriteTestEntry.class, Index.unique("name"))
                .addSerialization(NitriteTestEntry.class, new TestSerializer(), new TestDeserializer()))) {
            NitriteTestEntry in = NitriteTestEntry.builder().name("A").dlist(DoubleList.of(1, 2, 3)).darr(new double[]{1, 2, 3}).build();
            db.insert(in);
            NitriteTestEntry[] out = db.findAllStr(NitriteTestEntry.class).toArray(NitriteTestEntry[]::new);
            assertEquals("jackson db size", 1, out.length);
            assertEquals("jackson module used", in.name + "_S_D", out[0].name);
            assertEquals("id assignment", in.primaryKey, out[0].primaryKey);
            assertTrue("id assignment", out[0].primaryKey > 0);
            assertArrayEquals("jackson fastutil", in.dlist.toDoubleArray(), out[0].dlist.toDoubleArray(), 0d);
            assertArrayEquals("jackson primitive array", in.darr, out[0].darr, 0d);
        }

        file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();
        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, Index.unique("name")).addDeserializer(DoubleList.class, new DoubleArrayDeserializer()))) {
            NitriteTestEntry in = NitriteTestEntry.builder().name("A").dlist(DoubleList.of(1, 2, 3)).darr(new double[]{1, 2, 3}).build();
            db.insert(in);
            NitriteTestEntry[] out = db.findAllStr(NitriteTestEntry.class).toArray(NitriteTestEntry[]::new);
            assertEquals("jackson db size", 1, out.length);
            assertEquals("jackson module used", in.name, out[0].name);
            assertEquals("id assignment", in.primaryKey, out[0].primaryKey);
            assertTrue("id assignment", out[0].primaryKey > 0);
            assertArrayEquals("jackson fastutil", in.dlist.toDoubleArray(), out[0].dlist.toDoubleArray(), 0d);
            assertArrayEquals("jackson primitive array", in.darr, out[0].darr, 0d);
        }

    }


    @Test
    @SuppressWarnings("unchecked")
    public void testJoinDocuments() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        Document parent = Document.createDocument("name", "parent");

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                .addCollection("entries", Index.unique("name"))
                .addCollection("children", Index.nonUnique("name"))
        )) {

            db.insert("entries", parent);

            List<Document> children = Arrays.asList(
                    Document.createDocument("name", "A").put("parentId", parent.getId().getIdValue()),
                    Document.createDocument("name", "B").put("parentId", parent.getId().getIdValue()),
                    Document.createDocument("name", "C").put("parentId", parent.getId().getIdValue())
            );

            db.insertAll("children", children);

            List<Document> outParent = db.findAllStr("entries").toList();
            List<Document> outChildren = db.findAllStr("children").toList();
            List<Document> outChildrenF = db.findStr("children", Filter.or(Filter.where("name").eq("A"), Filter.where("name").eq("B"))).toList();

            assertEquals("1 parent", 1, outParent.size());
            assertEquals("parent okay", parent.get("name", String.class), outParent.getFirst().get("name", String.class));

            List<Document> results = db.joinAllChildrenStr("children", outParent, "_id", "parentId", "children").toList();

            assertEquals("1 joined parent", 1, results.size());

            assertEquals("joined parent okay", outParent.getFirst().getId(), results.getFirst().getId());
            assertEquals("joined parent okay", outParent.getFirst().get("name"), results.getFirst().get("name"));

            List<Document> outJoinedChildren = new ArrayList<>((Collection<Document>) results.getFirst().get("children"));
            outJoinedChildren.sort(Comparator.comparing((Document d) -> ((String) d.get("name"))));

            assertEquals("joined children okay", outChildren, outJoinedChildren);

            Document parentB = Document.createDocument("name", "parentB");

            db.insert("entries", parentB);

            List<Document> childrenB = Arrays.asList(
                    Document.createDocument("name", "A").put("parentId", parentB.getId().getIdValue()),
                    Document.createDocument("name", "B").put("parentId", parentB.getId().getIdValue()),
                    Document.createDocument("name", "C").put("parentId", parentB.getId().getIdValue())
            );

            db.insertAll("children", childrenB);

            outParent = db.findAllStr("entries").toList();

            results = db.joinChildrenStr("children", Filter.or(Filter.where("name").eq("A"), Filter.where("name").eq("B")), outParent, "_id", "parentId", "children").toList();

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", parent.getId(), results.get(0).getId());
            assertEquals("joined filtered parent okay", parent.get("name"), results.get(0).get("name"));

            assertEquals("joined filtered parent okay", parentB.getId(), results.get(1).getId());
            assertEquals("joined filtered parent okay", parentB.get("name"), results.get(1).get("name"));

            List<Document> outJoinedFChildren = new ArrayList<>((Collection<Document>) results.get(0).get("children"));
            outJoinedFChildren.sort(Comparator.comparing((Document d) -> ((String) d.get("name"))));
            List<Document> outJoinedFChildren1 = new ArrayList<>((Collection<Document>) results.get(1).get("children"));
            outJoinedFChildren.sort(Comparator.comparing((Document d) -> ((String) d.get("name"))));

            assertEquals("joined filtered children okay", outChildrenF.stream().map(e -> e.get("name", String.class)).sorted().toList(), outJoinedFChildren.stream().map(e -> e.get("name", String.class)).sorted().toList());
            assertEquals("joined filtered children okay", outChildrenF.stream().map(e -> e.get("name", String.class)).sorted().toList(), outJoinedFChildren1.stream().map(e -> e.get("name", String.class)).sorted().toList());

            outParent = db.findAllStr("entries").toList();

            results = db.joinChildrenStr("children", Filter.and(Filter.where("name").eq("A"), Filter.where("name").eq("B")), outParent, "_id", "parentId", "children").toList();

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
    public void testJoinObjects() throws IOException {

        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        NitriteFamilyTestEntry parent = NitriteFamilyTestEntry.builder().name("parent").build();

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build()
                .addRepository(NitriteFamilyTestEntry.class, Index.unique("name"))
                .addRepository(NitriteChildTestEntry.class, Index.nonUnique("name")
                ))) {

            db.insert(parent);

            List<NitriteChildTestEntry> children = new ArrayList<>(Arrays.asList(
                    NitriteChildTestEntry.builder().name("A").parentKey(parent.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("B").parentKey(parent.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("C").parentKey(parent.primaryKey).build()
            ));

            db.insertAll(children);

            List<NitriteFamilyTestEntry> outParent = db.findAllStr(NitriteFamilyTestEntry.class).toList();
            List<NitriteChildTestEntry> outChildren = db.findAllStr(NitriteChildTestEntry.class).toList();
            List<NitriteChildTestEntry> outChildrenF = db.findStr(Filter.or(Filter.where("name").eq("A"), Filter.where("name").eq("B")), NitriteChildTestEntry.class).toList();

            assertEquals("1 parent", 1, new ArrayList<>(outParent).size());
            assertEquals("parent okay", parent.primaryKey, outParent.getFirst().primaryKey);
            assertEquals("parent okay", parent.name, outParent.getFirst().name);

            List<NitriteFamilyTestEntry> results = db.joinAllChildrenStr(
                    NitriteChildTestEntry.class,
                    new ArrayList<>(outParent),
                    "primaryKey", "parentKey", "children").toList();

            assertEquals("1 joined parent", 1, results.size());

            assertEquals("joined parent okay", parent.primaryKey, results.getFirst().primaryKey);
            assertEquals("joined parent okay", parent.name, results.getFirst().name);

            assertEquals("joined children okay", outChildren.stream().map(e -> e.name).collect(Collectors.toSet()), results.getFirst().children.stream().map(e -> e.name).collect(Collectors.toSet()));

            NitriteFamilyTestEntry parentB = NitriteFamilyTestEntry.builder().name("parentB").build();

            db.insert(parentB);

            List<NitriteChildTestEntry> childrenB = new ArrayList<>(Arrays.asList(
                    NitriteChildTestEntry.builder().name("A").parentKey(parentB.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("B").parentKey(parentB.primaryKey).build(),
                    NitriteChildTestEntry.builder().name("C").parentKey(parentB.primaryKey).build()
            ));

            db.insertAll(childrenB);

            results = db.joinChildrenStr(
                            NitriteChildTestEntry.class,
                            Filter.or(Filter.where("name").eq("A"), Filter.where("name").eq("B")),
                            db.findAll(NitriteFamilyTestEntry.class),
                            "primaryKey", "parentKey", "children")
                    .collect(Collectors.toCollection(ArrayList::new));
            results.sort(Comparator.comparing(entry -> entry.name));

            assertEquals("2 joined filtered parents", 2, results.size());

            assertEquals("joined filtered parent okay", parent.primaryKey, results.get(0).primaryKey);
            assertEquals("joined filtered parent okay", parent.name, results.get(0).name);

            assertEquals("joined filtered parent okay", parentB.primaryKey, results.get(1).primaryKey);
            assertEquals("joined filtered parent okay", parentB.name, results.get(1).name);

            assertEquals("joined filtered children okay", outChildrenF.stream().map(e -> e.name).collect(Collectors.toSet()), results.get(0).children.stream().map(e -> e.name).collect(Collectors.toSet()));
            assertEquals("joined filtered children okay", outChildrenF.stream().map(e -> e.name).collect(Collectors.toSet()), results.get(1).children.stream().map(e -> e.name).collect(Collectors.toSet()));

            results = db.joinChildrenStr(
                    NitriteChildTestEntry.class,
                    Filter.and(Filter.where("name").eq("A"), Filter.where("name").eq("B")),
                    db.findAll(NitriteFamilyTestEntry.class),
                    "primaryKey", "parentKey", "children")
                    .collect(Collectors.toCollection(ArrayList::new));
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

        List<NitriteTestEntry> entries = IntStream.range(0, 100).mapToObj((int num) -> NitriteTestEntry.builder().name(Integer.toString(num)).build()).toList();
        List<String> expected = entries.stream().map(e -> e.name).toList();

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, Index.unique("name")).addDeserializer(NitriteTestEntry.class, new TestDeserializer()))) {
            List<Callable<Void>> jobs = entries.stream().map((NitriteTestEntry entry) -> (Callable<Void>) () -> {
                assertEquals("insert", 1, db.insert(entry));
                return null;
            }).collect(Collectors.toList());

            try(ExecutorService executorService = Executors.newFixedThreadPool(10)) {
                List<Future<Void>> futures = executorService.invokeAll(jobs);
                for (Future<Void> future : futures) {
                    future.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            List<String> out = db.findAllStr(NitriteTestEntry.class, "name").map(e -> e.name.substring(0, e.name.length() - 2)).sorted(Comparator.comparingInt(Integer::parseInt)).toList();
            assertEquals("concurrent insert", expected, out);
        }
    }

    @Test
    public void testOptionals() throws IOException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, Index.unique("name")).setOptionalFields(NitriteTestEntry.class, "data").addDeserializer(DoubleList.class, new DoubleArrayDeserializer()))) {

            NitriteTestEntry object = NitriteTestEntry.builder().name("TEST").data("BIGDATA").build();

            db.insert(object);

            NitriteTestEntry res1 = db.getByPrimaryKey(object.primaryKey, NitriteTestEntry.class).orElseThrow();
            NitriteTestEntry res2 = db.getByPrimaryKey(object.primaryKey, NitriteTestEntry.class, "data").orElseThrow();

            List<NitriteTestEntry> res3 = db.findAllStr(NitriteTestEntry.class).toList();
            List<NitriteTestEntry> res4 = db.findAllStr(NitriteTestEntry.class, "data").toList();

            assertNotNull(res1);
            assertNotNull(res2);

            assertEquals("1 document", 1, res3.size());
            assertEquals("1 document", 1, res4.size());

            assertNull("no data", res1.data);
            assertNull("no data", res3.getFirst().data);

            assertNotNull("has data", res2.data);
            assertNotNull("has data", res4.getFirst().data);

            NitriteTestEntry res5 = db.injectOptionalFields(res1, "data");
            List<NitriteTestEntry> res6 = db.injectOptionalFieldsStr(NitriteTestEntry.class, db.findAll(NitriteTestEntry.class), "data").toList();
            List<NitriteTestEntry> res7 = db.injectOptionalFieldsStr(NitriteTestEntry.class, db.findAllStr(NitriteTestEntry.class).toList(), "data").toList();

            assertNotNull(res5);

            assertEquals("1 document", 1, res6.size());
            assertEquals("1 document", 1, res7.size());

            assertNotNull("has data", res5.data);
            assertNotNull("has data", res6.getFirst().data);
            assertNotNull("has data", res7.getFirst().data);

        }

    }

    @Test
    public void testOptionalDocuments() throws IOException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addCollection("test", Index.unique("name")).setOptionalFields("test", "data"))) {

            Document doc = Document.createDocument("name", "TEST").put("data", "BIGDATA");
            assertNotNull(doc);

            db.insert("test", doc);

            Document res1 = db.getByNitriteId("test", doc.getId()).orElseThrow();
            Document res2 = db.getByNitriteId("test", doc.getId(), "data").orElseThrow();

            List<Document> res3 = db.findAllStr("test").toList();
            List<Document> res4 = db.findAllStr("test", "data").toList();

            assertNotNull(res1);
            assertNotNull(res2);

            assertEquals("1 document", 1, res3.size());
            assertEquals("1 document", 1, res4.size());

            assertFalse("no data", res1.containsKey("data"));
            assertFalse("no data", res3.getFirst().containsKey("data"));

            assertTrue("has data", res2.containsKey("data"));
            assertTrue("has data", res4.getFirst().containsKey("data"));

            Document res5 = db.injectOptionalFields("test", res1, "data");
            List<Document> res6 = db.injectOptionalFieldsStr("test", db.findAll("test"), "data").toList();
            List<Document> res7 = db.injectOptionalFieldsStr("test", db.findAllStr("test").toList(), "data").toList();

            assertNotNull(res5);

            assertEquals("1 document", 1, res6.size());
            assertEquals("1 document", 1, res7.size());

            assertTrue("has data", res5.containsKey("data"));
            assertTrue("has data", res6.getFirst().containsKey("data"));
            assertTrue("has data", res7.getFirst().containsKey("data"));

        }

    }

    @Test
    public void testEventsWithObjects() throws IOException, InterruptedException {
        Path file = Files.createTempFile("nitrite-test", "");
        file.toFile().deleteOnExit();

        final BlockingQueue<Long> idQueue = new ArrayBlockingQueue<>(3);
        final BlockingQueue<String> nameQueue = new ArrayBlockingQueue<>(3);

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addRepository(NitriteTestEntry.class, Index.unique("name")).addDeserializer(NitriteTestEntry.class, new TestDeserializer()))) {
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

            final Consumer<NitriteTestEntry> listener = (NitriteTestEntry e) -> {
                idQueue.add(e.primaryKey);
                nameQueue.add(e.name);
            };
            db.onInsert(NitriteTestEntry.class, listener);
            db.onUpdate(NitriteTestEntry.class, listener);
            db.onRemove(NitriteTestEntry.class, listener);

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

        try (NitriteDatabase db = new NitriteDatabase(file, Metadata.build().addCollection("entries", Index.unique("name")))) {
            List<Document> in = Arrays.asList(
                    Document.createDocument("name", "A"),
                    Document.createDocument("name", "B"),
                    Document.createDocument("name", "C")
            );

            Set<String> inserted = new HashSet<>();
            Set<String> updated = new HashSet<>();
            Set<String> removed = new HashSet<>();

            final Consumer<Document> listener = (Document e) -> {
                nameQueue.add(e.get("name", String.class));
            };
            db.onInsert("entries", listener);
            db.onUpdate("entries", listener);
            db.onRemove("entries", listener);

            db.insertAll("entries", in);
            for (int i = 0; i < 3; i++) {
                inserted.add(nameQueue.poll(1L, TimeUnit.SECONDS));
            }

            Set<String> expectedNames = in.stream().map(e -> e.get("name", String.class)).collect(Collectors.toSet());
            in.forEach(e -> e.put("name", e.get("name", String.class) + "_U"));


            db.upsertAll("entries", in);
            for (int i = 0; i < 3; i++) {
                updated.add(nameQueue.poll(1L, TimeUnit.SECONDS));
            }
            db.removeAll("entries", in);
            for (int i = 0; i < 3; i++) {
                removed.add(nameQueue.poll(1L, TimeUnit.SECONDS));
            }

            Set<String> updatedNames = in.stream().map(e -> e.get("name", String.class)).collect(Collectors.toSet());

            Assert.assertEquals(expectedNames, inserted);
            Assert.assertEquals(updatedNames, updated);
            Assert.assertEquals(updatedNames, removed);
        }

    }

}
