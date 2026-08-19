package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils;
import org.apache.lucene.index.IndexableField;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import de.unijena.bioinf.ms.middleware.service.search.description.IndexFacts;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The searchable-field description has to tell a client which values a field accepts and whether it can be
 * sorted by. Enums and booleans can answer that from their type; free text and String-typed vocabularies cannot.
 */
public class EnumSortAndPossibleValuesTest {

    private SearchService searchService;
    private Project<?> mockProject;
    private final String projectId = "test-project";

    @BeforeEach
    public void setup() throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(projectId);
        Mockito.when(mockProject.getSystemUID()).thenReturn("test-system-uid");
        searchService = new SearchServiceImpl(project ->
                new PerPojoSearchContext(null, new HashMap<>(Map.of("sampleType", ValueType.TEXT))));
        searchService.openOrCreateProjectIndex(mockProject);
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(mockProject, true);
    }

    /** Describing is done outside the search engine, over the facts it reports. */
    private SearchableFieldService searchableFields() {
        return new SearchableFieldService(IndexFacts.of(searchService, projectId), null);
    }

    private Map<String, SearchableField> fields(Class<?> pojo) {
        return searchableFields().describe(pojo).stream()
                .collect(Collectors.toMap(SearchableField::getName, Function.identity()));
    }

    @Test
    public void booleanFieldsReportTheValuesTheyAccept() {
        SearchableField hasMsMs = fields(AlignedFeature.class).get("hasMsMs");
        assertEquals(SearchableField.FieldType.BOOLEAN, hasMsMs.getFieldType());
        // exactly as indexed: booleans are keyword indexed from Boolean.toString()
        assertEquals(List.of("true", "false"), hasMsMs.getPossibleValues());
    }

    @Test
    public void enumFieldsReportTheirConstants() {
        SearchableField quality = fields(AlignedFeature.class).get("quality");
        assertEquals(SearchableField.FieldType.ENUM, quality.getFieldType());
        assertEquals(Arrays.stream(DataQuality.values()).map(Enum::name).toList(), quality.getPossibleValues());
    }

    @Test
    public void enumFieldsCanBeSortedBy() {
        assertTrue(fields(AlignedFeature.class).get("quality").isSortable(),
                "an ordered enum such as quality should be sortable");
    }

    @Test
    public void openTextFieldsStillReportNoValues() {
        assertNull(fields(AlignedFeature.class).get("name").getPossibleValues());
        assertNull(fields(Run.class).get("source").getPossibleValues());
        // chromatography holds a closed vocabulary but is declared as String, so its values are not derivable
        assertNull(fields(Run.class).get("chromatography").getPossibleValues());
    }

    @Test
    public void onlySortableEnumsStoreAnOrdinal() {
        // A non-sortable enum is indexed by name only. Nothing ordinal dependent is persisted for it, so
        // constants can be added or reordered later without invalidating existing indices.
        List<IndexableField> notSortable =
                LuceneMappingUtils.getIndexedFieldsFromSimpleValue("q", DataQuality.GOOD, true, false, false, false);
        assertEquals(1, notSortable.size(), "a non-sortable enum should only be indexed as a term");
        assertEquals(DataQuality.GOOD.name(), notSortable.get(0).stringValue());
        assertTrue(notSortable.stream().noneMatch(f -> f.numericValue() != null),
                "a non-sortable enum must not store an ordinal");

        // A sortable enum additionally stores its ordinal as a numeric doc value, so it sorts by declaration
        // order rather than alphabetically.
        List<IndexableField> sortable =
                LuceneMappingUtils.getIndexedFieldsFromSimpleValue("q", DataQuality.GOOD, true, true, false, false);
        assertEquals(2, sortable.size());
        assertEquals(DataQuality.GOOD.ordinal(),
                sortable.stream().filter(f -> f.numericValue() != null).findFirst().orElseThrow()
                        .numericValue().intValue());
    }


}
