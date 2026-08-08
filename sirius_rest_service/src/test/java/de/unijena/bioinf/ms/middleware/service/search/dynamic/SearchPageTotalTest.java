package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The total of a page is what a client uses to decide whether to ask for more, so it has to be the real number of
 * matches. Lucene only counts exactly up to a threshold and reports a lower bound beyond it, which would make a client
 * stop early and silently miss the rest of its results.
 */
class SearchPageTotalTest {

    /** above Lucene's default total hits threshold of 1000, where the reported count starts to be a lower bound */
    private static final int MATCHES = 2175;

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Feature {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "quality", fullTextSearch = true, defaultSearchField = true)
        public String quality;
    }

    private static SinglePojoLuceneIndexManager<Feature> indexOfMatchingFeatures() {
        SinglePojoLuceneIndexManager<Feature> index = new SinglePojoLuceneIndexManager<>(
                new ByteBuffersDirectory(), Feature.class, null, name -> ValueType.TEXT);
        for (int i = 0; i < MATCHES; i++)
            index.addDocument(new Feature(Integer.toString(i), "GOOD"));
        //features that must not be counted, so a wrong total cannot accidentally look right
        for (int i = 0; i < 500; i++)
            index.addDocument(new Feature(Integer.toString(MATCHES + i), "BAD"));
        return index;
    }

    @Test
    @DisplayName("a small page still reports how many matches there are in total")
    void totalIsExactForASmallPage() throws java.io.IOException {
        try (SinglePojoLuceneIndexManager<Feature> index = indexOfMatchingFeatures()) {
            Page<Feature> page = index.search("quality:GOOD", PageRequest.of(0, 10));

            assertEquals(10, page.getContent().size(), "the page itself holds one page of results");
            assertEquals(MATCHES, page.getTotalElements(), "the total must be the real number of matches");
        }
    }

    @Test
    @DisplayName("the total does not depend on the requested page size")
    void totalDoesNotDependOnPageSize() throws java.io.IOException {
        try (SinglePojoLuceneIndexManager<Feature> index = indexOfMatchingFeatures()) {
            for (int size : new int[]{1, 500, 1500, 3000})
                assertEquals(MATCHES, index.search("quality:GOOD", PageRequest.of(0, size)).getTotalElements(),
                        "total differs when asking for " + size + " results per page");
        }
    }

    @Test
    @DisplayName("ids are counted the same way as whole objects")
    void idSearchReportsTheSameTotal() throws java.io.IOException {
        try (SinglePojoLuceneIndexManager<Feature> index = indexOfMatchingFeatures()) {
            assertEquals(MATCHES, index.searchIds("quality:GOOD", PageRequest.of(0, 10)).getTotalElements());
        }
    }

    @Test
    @DisplayName("an unpaged search reports its total as well")
    void unpagedSearchReportsTheTotal() throws java.io.IOException {
        try (SinglePojoLuceneIndexManager<Feature> index = indexOfMatchingFeatures()) {
            Page<Feature> page = index.search("quality:GOOD", Pageable.unpaged());

            assertEquals(MATCHES, page.getContent().size());
            assertEquals(MATCHES, page.getTotalElements());
        }
    }

    @Test
    @DisplayName("a page past the end reports the total, not what is left")
    void pageBeyondTheEndReportsTheTotal() throws java.io.IOException {
        try (SinglePojoLuceneIndexManager<Feature> index = indexOfMatchingFeatures()) {
            Page<Feature> page = index.search("quality:GOOD", PageRequest.of(10_000, 10));

            assertEquals(0, page.getContent().size());
            assertEquals(MATCHES, page.getTotalElements());
        }
    }
}
