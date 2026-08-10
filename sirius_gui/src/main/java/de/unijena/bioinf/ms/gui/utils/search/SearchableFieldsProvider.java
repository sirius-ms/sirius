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

import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.model.SearchableField;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Provides the searchable fields of the feature index for the search-bar autocompletion, cached per
 * project. Tag fields are dynamic (tag definitions can be added/removed at any time), so the cache
 * goes stale after a short time and is refreshed on demand - typically when the search overlay
 * opens, off the EDT ({@link #refreshIfStale()} performs a blocking API call when stale).
 * A failed fetch keeps the last known fields and stays retryable.
 */
@Slf4j
public class SearchableFieldsProvider {

    private static final long DEFAULT_MAX_AGE_MILLIS = 30_000;

    private final Supplier<List<SearchableField>> fetcher;
    private final long maxAgeMillis;
    private final LongSupplier clock;

    private volatile List<SearchableField> cached = List.of();
    private volatile long fetchedAtMillis = Long.MIN_VALUE;

    public SearchableFieldsProvider(@NotNull SiriusClient siriusClient, @NotNull String projectId) {
        this(() -> siriusClient.features().getAlignedFeaturesSearchableFields(projectId),
                DEFAULT_MAX_AGE_MILLIS, System::currentTimeMillis);
    }

    SearchableFieldsProvider(@NotNull Supplier<List<SearchableField>> fetcher, long maxAgeMillis,
                             @NotNull LongSupplier clock) {
        this.fetcher = fetcher;
        this.maxAgeMillis = maxAgeMillis;
        this.clock = clock;
    }

    /**
     * The last fetched fields without triggering any fetch; empty before the first fetch.
     */
    public List<SearchableField> getCached() {
        return cached;
    }

    /**
     * The current fields, fetching them when the cache is stale. Blocking - call off the EDT.
     * On fetch failure the last known fields are returned and the cache stays stale (retryable).
     */
    public synchronized List<SearchableField> refreshIfStale() {
        if (fetchedAtMillis != Long.MIN_VALUE && clock.getAsLong() - fetchedAtMillis < maxAgeMillis)
            return cached;
        try {
            List<SearchableField> fields = fetcher.get();
            cached = fields == null ? List.of() : List.copyOf(fields);
            fetchedAtMillis = clock.getAsLong();
        } catch (Exception e) {
            log.warn("Could not fetch searchable fields; keeping the last known {}.", cached.size(), e);
        }
        return cached;
    }
}
