package de.unijena.bioinf.storage.db.nitrite;

import com.google.api.client.util.Lists;
import com.google.common.collect.Streams;
import de.unijena.bioinf.storage.db.NoSQLDatabase;
import de.unijena.bioinf.storage.db.NoSQLFilter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;
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

    @Indices({
            @Index(value = "name", type = IndexType.Unique)
    })
    private static class NitriteTestEntry extends NitritePOJO {

        public String name;

        public NitriteTestEntry() {
            super();
        }

        public NitriteTestEntry(String name) {
            super();
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "NitriteTestEntry{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
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
                    new NoSQLFilter().in("a", 42, "foo"),
                    new NoSQLFilter().notIn("a", 42, "foo"),
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
                    ObjectFilters.in("a", 42, "foo"),
                    ObjectFilters.notIn("a", 42, "foo"),
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
