package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase-1 (H12): operations targeting a project whose index has not been opened (or failed to open)
 * must fail with a clear exception rather than a {@link NullPointerException} raised deep inside the
 * context helpers.
 */
public class SearchServiceContextTest {

    private SearchService serviceWithNoOpenProjects() throws IOException {
        return new SearchServiceImpl(project -> new PerPojoSearchContext(null, new HashMap<>()));
    }

    @Test
    public void searchOnUnopenedProjectThrowsClearException() throws IOException {
        SearchService service = serviceWithNoOpenProjects();
        assertThrows(IllegalStateException.class,
                () -> service.search("never-opened", "anything", Pageable.unpaged(), Object.class),
                "searching a project without an open index must throw a clear IllegalStateException, not an NPE (H12)");
    }

    @Test
    public void addDocumentToUnopenedProjectThrowsClearException() throws IOException {
        SearchService service = serviceWithNoOpenProjects();
        assertThrows(IllegalStateException.class,
                () -> service.addDocument("never-opened", new Object()),
                "indexing into a project without an open index must throw a clear IllegalStateException, not an NPE (H12)");
    }
}
