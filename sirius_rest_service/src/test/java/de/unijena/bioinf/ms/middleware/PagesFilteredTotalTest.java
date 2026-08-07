package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pages of filtered queries, e.g. the features of one aligned feature, must report the number of matching
 * objects as total. Counting the whole collection instead makes clients page through elements that do not
 * belong to the query.
 */
class PagesFilteredTotalTest {

    private static final Filter FILTER = Filter.where("alignedFeatureId").eq(42L);

    @Test
    void filteredPageCountsOnlyMatchingObjects() throws IOException {
        Database<?> storage = mock(Database.class);
        when(storage.count(any(Filter.class), any())).thenReturn(7L);

        Page<String> page = Pages.makePage(storage, PageRequest.of(0, 2), List.of("a", "b"), Feature.class, FILTER);

        assertEquals(7L, page.getTotalElements(), "total must refer to the objects matching the filter");
        verify(storage, never()).countAll(any(Class.class));
    }

    @Test
    void unpagedFilteredRequestNeedsNoCount() throws IOException {
        Database<?> storage = mock(Database.class);

        Page<String> page = Pages.makePage(storage, Pageable.unpaged(), List.of("a", "b", "c"), Feature.class, FILTER);

        assertEquals(3L, page.getTotalElements());
        verify(storage, never()).count(any(Filter.class), any());
    }

    @Test
    void emptyContentYieldsEmptyPage() throws IOException {
        Database<?> storage = mock(Database.class);

        Page<String> page = Pages.makePage(storage, PageRequest.of(3, 2), List.of(), Feature.class, FILTER);

        assertEquals(0L, page.getTotalElements());
        verify(storage, never()).count(any(Filter.class), any());
    }
}
