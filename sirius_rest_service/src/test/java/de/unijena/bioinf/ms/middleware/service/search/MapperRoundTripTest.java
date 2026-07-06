package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-0 harness T0.2: mapper round-trip coverage for {@code GenericPojoMapper}/{@code LuceneMappingUtils}.
 * <p>
 * Exercises {@code toDocument} -> {@code toPojo} for the full range of supported field kinds and pins several
 * known defects from the merge review. Tests tagged {@code [RED]} are expected to FAIL against the current
 * branch; they document a bug and become green once the corresponding fix lands.
 * <ul>
 *   <li>{@code [RED] B4} — locale-dependent numeric parsing corrupts mass/mz range queries.</li>
 *   <li>{@code [RED] H2} — {@code java.util.Date} fields cannot be round-tripped.</li>
 *   <li>{@code [RED] M11} — a null nested object is resurrected as an empty instance.</li>
 * </ul>
 */
public class MapperRoundTripTest {

    private static final String PROJECT_ID = "mapper-roundtrip-project";

    public enum Mode {POSITIVE, NEGATIVE}

    @NoArgsConstructor
    @AllArgsConstructor
    public static class NumericPojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "name", fullTextSearch = true, defaultSearchField = true)
        public String name;
        @IndexField(name = "count")
        public int count;
        @IndexField(name = "bigCount")
        public long bigCount;
        @IndexField(name = "mz", sortable = true)
        public double mz;
        @IndexField(name = "intensity")
        public float intensity;
        @IndexField(name = "flag")
        public boolean flag;
        @IndexField(name = "mode")
        public Mode mode;
        @IndexField(name = "aliases")
        public List<String> aliases;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatePojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "created")
        public Date created;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Child {
        @IndexField(name = "label")
        public String label;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentPojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "child")
        public Child child;
    }

    private Project<?> mockProject;

    /** Fresh in-memory search service + opened project index. */
    private SearchService newService() throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(PROJECT_ID);
        Mockito.when(mockProject.getSystemUID()).thenReturn("mapper-roundtrip-uid");
        Mockito.when(mockProject.findTags()).thenReturn(Collections.emptyList());

        SearchService service = new SearchServiceImpl(project -> new PerPojoSearchContext(null, new HashMap<>()));
        service.openOrCreateProjectIndex(mockProject);
        return service;
    }

    private static <T> T single(Page<T> page) {
        assertEquals(1, page.getTotalElements(), "expected exactly one round-tripped document");
        return page.getContent().getFirst();
    }

    @Test
    public void testNumericAndScalarRoundTrip() throws IOException {
        SearchService service = newService();
        try {
            NumericPojo in = new NumericPojo("1", "glucose", 42, 9_000_000_000L,
                    180.06339, 1234.5f, true, Mode.NEGATIVE, List.of("dextrose", "grape sugar"));
            service.addDocument(PROJECT_ID, in);

            NumericPojo out = single(service.search(PROJECT_ID, null, Pageable.unpaged(), NumericPojo.class));

            assertEquals("glucose", out.name);
            assertEquals(42, out.count, "int field must round-trip");
            assertEquals(9_000_000_000L, out.bigCount, "long field must round-trip (and not truncate to int)");
            assertEquals(180.06339, out.mz, 0.0, "double must round-trip without precision loss");
            assertEquals(1234.5f, out.intensity, 0.0f, "float must round-trip");
            assertTrue(out.flag, "boolean must round-trip");
            assertEquals(Mode.NEGATIVE, out.mode, "enum must round-trip");
            assertNotNull(out.aliases);
            assertTrue(out.aliases.containsAll(List.of("dextrose", "grape sugar")), "collection must round-trip");
        } finally {
            service.closeProjectIndex(mockProject, true);
        }
    }

    @Test
    public void testNumericRangeQueryDefaultLocale() throws IOException {
        SearchService service = newService();
        try {
            service.addDocument(PROJECT_ID, mz("in", 150.0));
            service.addDocument(PROJECT_ID, mz("out", 250.0));

            Page<NumericPojo> hits = service.search(PROJECT_ID, "mz:[100.0 TO 200.0]", Pageable.unpaged(), NumericPojo.class);
            assertEquals(1, hits.getTotalElements(), "mz range query must select exactly the in-range document");
            assertEquals("in", hits.getContent().getFirst().id);
        } finally {
            service.closeProjectIndex(mockProject, true);
        }
    }

    /**
     * [RED] B4 — numeric PointsConfig is built with {@code DecimalFormat.getInstance()} (default locale).
     * Under a locale that uses '.' as a grouping separator (e.g. Germany), "100.0" parses as 1000 and the
     * range query returns wrong results. Expected to fail until B4 is fixed (Locale.ROOT).
     */
    @Test
    public void testNumericRangeQueryUnderGermanLocale_RED_B4() throws IOException {
        java.util.Locale previous = java.util.Locale.getDefault();
        java.util.Locale.setDefault(java.util.Locale.GERMANY);
        try {
            // build service + index manager + points config all under the German locale
            SearchService service = newService();
            try {
                service.addDocument(PROJECT_ID, mz("in", 150.0));
                service.addDocument(PROJECT_ID, mz("out", 250.0));

                Page<NumericPojo> hits = service.search(PROJECT_ID, "mz:[100.0 TO 200.0]", Pageable.unpaged(), NumericPojo.class);
                assertEquals(1, hits.getTotalElements(),
                        "mz range query must be locale-independent (B4): expected the single in-range document");
                assertEquals("in", hits.getContent().getFirst().id);
            } finally {
                service.closeProjectIndex(mockProject, true);
            }
        } finally {
            java.util.Locale.setDefault(previous);
        }
    }

    /**
     * [RED] H2 — {@code java.util.Date} is not handled as a simple type, so it is neither indexed as a
     * LongPoint nor restored on read-back. Expected to fail until H2 is fixed.
     */
    @Test
    public void testDateFieldRoundTrip_RED_H2() throws IOException {
        SearchService service = newService();
        try {
            Date created = new Date(1_600_000_000_000L);
            service.addDocument(PROJECT_ID, new DatePojo("1", created));

            DatePojo out = assertDoesNotThrow(
                    () -> single(service.search(PROJECT_ID, null, Pageable.unpaged(), DatePojo.class)),
                    "Date round-trip must not throw (H2)");
            assertEquals(created, out.created, "Date field must round-trip (H2)");
        } finally {
            service.closeProjectIndex(mockProject, true);
        }
    }

    /**
     * [RED] M11 — a null nested object is re-instantiated as an empty (non-null) instance on read-back.
     * Expected to fail until M11 is fixed.
     */
    @Test
    public void testNullNestedStaysNull_RED_M11() throws IOException {
        SearchService service = newService();
        try {
            service.addDocument(PROJECT_ID, new ParentPojo("1", null));

            ParentPojo out = single(service.search(PROJECT_ID, null, Pageable.unpaged(), ParentPojo.class));
            assertNull(out.child, "a null nested object must remain null after round-trip (M11)");
        } finally {
            service.closeProjectIndex(mockProject, true);
        }
    }

    @Test
    public void deepPaginationDoesNotOverflow_C6() throws IOException {
        SearchService service = newService();
        try {
            service.addDocument(PROJECT_ID, mz("1", 1.0));

            // offset = page * size = 3_000_000 * 1000 = 3e9, which exceeds Integer.MAX_VALUE.
            Page<NumericPojo> page = service.search(PROJECT_ID, null, PageRequest.of(3_000_000, 1000), NumericPojo.class);

            assertEquals(1, page.getTotalElements(), "total hits must still be reported");
            assertTrue(page.getContent().isEmpty(), "a page far past the end must be empty, not overflow (C6)");
        } finally {
            service.closeProjectIndex(mockProject, true);
        }
    }

    private static NumericPojo mz(String id, double mz) {
        return new NumericPojo(id, "feature-" + id, 0, 0L, mz, 0f, false, Mode.POSITIVE, List.of());
    }
}
