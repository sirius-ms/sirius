package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexFieldWithMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-3 index/DB consistency for update-side paths.
 * <ul>
 *   <li>H4 — the bulk {@code updateDocumentsFields} must apply the same non-stored-field guard as the
 *       single-document variant, instead of silently dropping unstored fields.</li>
 *   <li>H5 — {@code removeTagValueType} must not resurrect logically-deleted documents.</li>
 * </ul>
 */
public class IndexUpdateConsistencyTest {

    private static final String PROJECT_ID = "index-update-project";

    @NoArgsConstructor
    @AllArgsConstructor
    public static class NonStoredPojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "secret", stored = false)
        public String secret;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaggablePojo implements Taggable {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "name", fullTextSearch = true, defaultSearchField = true)
        public String name;
        @IndexFieldWithMapper(mapper = TagMapper.class)
        public Map<String, Tag> tags;

        @Override
        public Map<String, Tag> getTags() {
            return tags;
        }

        @Override
        public void setTags(Map<String, Tag> tags) {
            this.tags = tags;
        }
    }

    private Project<?> mockProject;

    private SearchService newService(Map<String, ValueType> tagDefs) throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(PROJECT_ID);
        Mockito.when(mockProject.getSystemUID()).thenReturn("index-update-uid");
        Mockito.when(mockProject.findTags()).thenReturn(Collections.emptyList());

        SearchService service = new SearchServiceImpl(project -> new PerPojoSearchContext(null, tagDefs));
        service.openOrCreateProjectIndex(mockProject);
        return service;
    }

    @Test
    public void bulkUpdateFieldsRejectsNonStoredFields() throws IOException {
        SearchService service = newService(new HashMap<>());
        try {
            service.addDocument(PROJECT_ID, new NonStoredPojo("1", "classified"));

            assertThrows(UnsupportedOperationException.class,
                    () -> service.updateDocumentsFields(PROJECT_ID, java.util.List.of("1"),
                            (NonStoredPojo p) -> p.secret = "changed", NonStoredPojo.class),
                    "bulk updateDocumentsFields must reject POJOs with non-stored indexed fields (H4)");
        } finally {
            service.closeProjectIndex(mockProject, true);
        }
    }

    @Test
    public void removeTagValueTypeDoesNotResurrectDeletedDocs() throws IOException {
        Map<String, ValueType> tagDefs = new HashMap<>();
        tagDefs.put("color", ValueType.TEXT);
        SearchService service = newService(tagDefs);
        try {
            service.addDocument(PROJECT_ID, new TaggablePojo("1", "alpha", tagMap("color", "red")));
            service.addDocument(PROJECT_ID, new TaggablePojo("2", "beta", tagMap("color", "blue")));

            // Logically delete one document.
            service.removeDocumentById(PROJECT_ID, "2", TaggablePojo.class);
            assertEquals(1, service.search(PROJECT_ID, null, Pageable.unpaged(), TaggablePojo.class).getTotalElements());

            // Removing a tag value type re-indexes affected docs; the deleted doc must not come back.
            service.removeTagValueType(PROJECT_ID, "color");

            Page<TaggablePojo> remaining = service.search(PROJECT_ID, null, Pageable.unpaged(), TaggablePojo.class);
            assertEquals(1, remaining.getTotalElements(),
                    "removeTagValueType must not resurrect the logically-deleted document (H5)");
            assertEquals("1", remaining.getContent().getFirst().id);
        } finally {
            service.closeProjectIndex(mockProject, true);
        }
    }

    private static Map<String, Tag> tagMap(String name, String value) {
        Map<String, Tag> tags = new HashMap<>();
        tags.put(name, Tag.builder().tagName(name).value(value).build());
        return tags;
    }
}
