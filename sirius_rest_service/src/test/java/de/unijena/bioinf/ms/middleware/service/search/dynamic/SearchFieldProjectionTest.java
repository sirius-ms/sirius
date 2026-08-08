package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Reading single fields from the index must give the same values as reconstructing the whole object, otherwise a
 * caller that only needs a few properties would silently get something else than the object it refers to.
 */
class SearchFieldProjectionTest {

    private static final String PROJECT_ID = "test";

    @AutoClose
    private NitriteSirirusProject ps;
    private SearchService searchService;

    @BeforeEach
    void createTestProject() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        searchService = makeSearchService();
        //registers the project with the search service
        new NoSQLProjectImpl(PROJECT_ID, new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);

        searchService.addDocument(PROJECT_ID, AlignedFeature.builder()
                .alignedFeatureId("1")
                .name("caffeine")
                .ionMass(195.0876d)
                .charge(1)
                .build());
    }

    @SneakyThrows
    private static SearchService makeSearchService() {
        return new SearchServiceImpl(p -> {
            Map<String, ValueType> tagDefinitions = new HashMap<>();
            for (Object item : p.findTags()) {
                TagDefinition td = (TagDefinition) item;
                tagDefinitions.put(td.getTagName(), td.getValueType());
            }
            return new PerPojoSearchContext(null, tagDefinitions);
        });
    }

    private AlignedFeature indexedFeature() {
        return searchService.search(PROJECT_ID, null, Pageable.unpaged(), AlignedFeature.class).getContent().getFirst();
    }

    private <R> R projected(Set<String> fields, java.util.function.Function<de.unijena.bioinf.ms.middleware.service.search.IndexedFields, R> mapper) {
        return searchService.searchFields(PROJECT_ID, null, Pageable.unpaged(), AlignedFeature.class, fields, mapper)
                .getContent().getFirst();
    }

    @Test
    @DisplayName("a projected number is the number the object carries")
    void numbersMatchTheObject() {
        AlignedFeature feature = indexedFeature();

        assertEquals(feature.getIonMass(), projected(Set.of("ionMass"), f -> f.getDouble("ionMass")));
        assertEquals(Integer.valueOf(feature.getCharge()), projected(Set.of("charge"), f -> f.getInt("charge")));
    }

    @Test
    @DisplayName("a projected text is the text the object carries")
    void textMatchesTheObject() {
        AlignedFeature feature = indexedFeature();

        assertEquals(feature.getName(), projected(Set.of("name"), f -> f.getString("name")));
        assertEquals(feature.getAlignedFeatureId(),
                projected(Set.of("alignedFeatureId"), f -> f.getString("alignedFeatureId")));
    }

    @Test
    @DisplayName("fields that were not requested are not readable, even though the object has them")
    void unrequestedFieldsAreNotLoaded() {
        assertNull(projected(Set.of("alignedFeatureId"), f -> f.getString("name")));
    }
}
