package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A searchQuery is written by the client, so a query the parser cannot make sense of is a client error.
 * These used to escape as HTTP 500 (#967).
 */
public class SearchQueryErrorTest {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPojo {
        @IndexField(name = "id", documentId = true)
        public String id;

        @IndexField(name = "name", defaultSearchField = true)
        public String name;

        @IndexField(name = "precursorMz")
        public double precursorMz;
    }

    private SearchService searchService;
    private Project<?> mockProject;
    private final String projectId = "test-project";

    @BeforeEach
    public void setup() throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(projectId);
        Mockito.when(mockProject.getSystemUID()).thenReturn("test-system-uid");
        Mockito.when(mockProject.findTags()).thenReturn(Collections.emptyList());

        searchService = new SearchServiceImpl(project -> {
            Map<String, ValueType> tagDefinitions = new HashMap<>();
            for (Object item : project.findTags()) {
                TagDefinition td = (TagDefinition) item;
                tagDefinitions.put(td.getTagName(), td.getValueType());
            }
            return new PerPojoSearchContext(null, tagDefinitions);
        });
        searchService.openOrCreateProjectIndex(mockProject);
        searchService.addDocument(projectId, new TestPojo("1", "Apigenin", 271.0601));
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(mockProject, true);
    }

    private ResponseStatusException searchExpectingFailure(String query) {
        return assertThrows(ResponseStatusException.class,
                () -> searchService.search(projectId, query, PageRequest.of(0, 10), TestPojo.class),
                "a query the parser rejects must not escape as a server error");
    }

    @Test
    public void unbalancedParenthesisIsABadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, searchExpectingFailure("name:(Apigenin").getStatusCode());
    }

    @Test
    public void danglingOperatorIsABadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, searchExpectingFailure("name:Apigenin AND").getStatusCode());
    }

    /**
     * Not a parse error - the query is well formed, but the bound cannot be coerced for a numeric field.
     */
    @Test
    public void nonNumericRangeBoundIsABadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, searchExpectingFailure("precursorMz:[abc TO def]").getStatusCode());
    }

    @Test
    public void aValidQueryStillWorks() {
        assertEquals(1, searchService.search(projectId, "name:Apigenin", PageRequest.of(0, 10), TestPojo.class)
                .getTotalElements());
    }

    @Test
    public void aValidRangeQueryStillWorks() {
        assertEquals(1, searchService.search(projectId, "precursorMz:[271 TO 272]", PageRequest.of(0, 10), TestPojo.class)
                .getTotalElements());
    }
}
