/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.middleware.service.search;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

/**
 * The fields of a single indexed object, as requested when searching for field values.
 * <p>
 * Values are converted the same way they are when a whole object is reconstructed from the index, so reading a field
 * here gives what the corresponding property of the object would hold. Simple types are supported: text, numbers,
 * booleans, dates and enums. Fields that are reconstructed by a dedicated mapper, such as tags or fold changes, are
 * not available, they need the object itself.
 * <p>
 * Only the fields that have been requested are available, all others are null even if the object has them.
 */
public interface IndexedFields {

    /**
     * @param type type the value shall be converted to, a boxed type for numbers
     * @return value of the given field or null if the object does not have it or it has not been requested
     * @throws IllegalArgumentException if the value cannot be converted to the given type
     */
    <V> @Nullable V get(@NotNull String field, @NotNull Class<V> type);

    /**
     * @return all values of the given field, for fields that hold a collection
     */
    <V> @NotNull List<V> getAll(@NotNull String field, @NotNull Class<V> type);

    default @Nullable String getString(@NotNull String field) {
        return get(field, String.class);
    }

    default @Nullable Long getLong(@NotNull String field) {
        return get(field, Long.class);
    }

    default @Nullable Integer getInt(@NotNull String field) {
        return get(field, Integer.class);
    }

    default @Nullable Double getDouble(@NotNull String field) {
        return get(field, Double.class);
    }

    default @Nullable Float getFloat(@NotNull String field) {
        return get(field, Float.class);
    }

    default @Nullable Boolean getBoolean(@NotNull String field) {
        return get(field, Boolean.class);
    }

    default @Nullable Date getDate(@NotNull String field) {
        return get(field, Date.class);
    }

    default <E extends Enum<E>> @Nullable E getEnum(@NotNull String field, @NotNull Class<E> type) {
        return get(field, type);
    }
}
