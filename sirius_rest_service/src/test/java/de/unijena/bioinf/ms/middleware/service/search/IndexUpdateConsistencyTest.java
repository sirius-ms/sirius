package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase-3 index/DB consistency for update-side paths.
 * <ul>
 *   <li>H4 — the bulk {@code updateDocumentsFields} must apply the same non-stored-field guard as the
 *       single-document variant, instead of silently dropping unstored fields.</li>
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

    private Project<?> mockProject;

    private SearchService newService() throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(PROJECT_ID);
        Mockito.when(mockProject.getSystemUID()).thenReturn("index-update-uid");
        Mockito.when(mockProject.findTags()).thenReturn(Collections.emptyList());

        SearchService service = new SearchServiceImpl(project -> new PerPojoSearchContext(null, new HashMap<>()));
        service.openOrCreateProjectIndex(mockProject);
        return service;
    }

    @Test
    public void bulkUpdateFieldsRejectsNonStoredFields_H4() throws IOException {
        SearchService service = newService();
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
}
