/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils.search;

import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

public class SearchableFieldsProviderTest {

    private static final List<SearchableField> FIELDS =
            List.of(new SearchableField().name("ionMass").fieldType(SearchableFieldType.DOUBLE));

    @Test
    public void testFetchesOnceWhileFresh() {
        AtomicInteger calls = new AtomicInteger();
        AtomicLong clock = new AtomicLong(0);
        SearchableFieldsProvider provider = new SearchableFieldsProvider(
                () -> { calls.incrementAndGet(); return FIELDS; }, 30_000, clock::get);

        assertEquals(FIELDS, provider.refreshIfStale());
        clock.set(10_000); // still fresh
        assertEquals(FIELDS, provider.refreshIfStale());
        assertEquals(1, calls.get());
    }

    @Test
    public void testRefetchesWhenStale() {
        AtomicInteger calls = new AtomicInteger();
        AtomicLong clock = new AtomicLong(0);
        SearchableFieldsProvider provider = new SearchableFieldsProvider(
                () -> { calls.incrementAndGet(); return FIELDS; }, 30_000, clock::get);

        provider.refreshIfStale();
        clock.set(31_000);
        provider.refreshIfStale();
        assertEquals(2, calls.get());
    }

    @Test
    public void testFailedFetchKeepsLastResultAndStaysRetryable() {
        AtomicInteger calls = new AtomicInteger();
        AtomicLong clock = new AtomicLong(0);
        SearchableFieldsProvider provider = new SearchableFieldsProvider(
                () -> {
                    if (calls.incrementAndGet() > 1)
                        throw new IllegalStateException("server gone");
                    return FIELDS;
                }, 30_000, clock::get);

        assertEquals(FIELDS, provider.refreshIfStale());
        clock.set(31_000);
        // fetch fails -> last known fields are kept, no exception escapes to the UI
        assertEquals(FIELDS, provider.refreshIfStale());
        // and the failure did not mark the cache fresh: the next call retries
        provider.refreshIfStale();
        assertEquals(3, calls.get());
    }

    @Test
    public void testGetCachedNeverFetches() {
        AtomicInteger calls = new AtomicInteger();
        SearchableFieldsProvider provider = new SearchableFieldsProvider(
                () -> { calls.incrementAndGet(); return FIELDS; }, 30_000, () -> 0L);

        assertTrue(provider.getCached().isEmpty());
        assertEquals(0, calls.get());
    }
}
