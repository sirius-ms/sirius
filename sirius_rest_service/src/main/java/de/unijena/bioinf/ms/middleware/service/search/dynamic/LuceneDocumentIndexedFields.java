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

package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.service.search.IndexedFields;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.convertStoredValue;

/**
 * The fields of a lucene document. Values are converted with {@link LuceneMappingUtils#convertStoredValue}, the same
 * conversion that is used to reconstruct whole objects from the index.
 */
record LuceneDocumentIndexedFields(@NotNull Document document) implements IndexedFields {

    /**
     * Numbers are stored as numeric values, so they have to be read as such. Class literals of primitives are mapped
     * to their boxed type, since a converted value can only be cast to the latter.
     */
    private static final Map<Class<?>, Class<?>> BOXED_TYPES = Map.of(
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class,
            float.class, Float.class,
            boolean.class, Boolean.class);

    private static final List<Class<?>> SUPPORTED_TYPES = List.of(
            String.class, Integer.class, Long.class, Double.class, Float.class, Boolean.class, Date.class);

    /**
     * Numbers are stored as numeric values, unless the indexed property is a text that happens to hold a number,
     * e.g. an id. Those are stored as text and have to be parsed.
     */
    private static final List<Class<?>> NUMERIC_TYPES = List.of(Integer.class, Long.class, Double.class, Float.class);

    @Override
    public <V> @Nullable V get(@NotNull String field, @NotNull Class<V> type) {
        IndexableField value = document.getField(field);
        return value == null ? null : convert(value, field, type);
    }

    @Override
    public <V> @NotNull List<V> getAll(@NotNull String field, @NotNull Class<V> type) {
        return Arrays.stream(document.getFields(field))
                .map(value -> convert(value, field, type))
                .toList();
    }

    private <V> V convert(IndexableField value, String field, Class<V> type) {
        @SuppressWarnings("unchecked")
        Class<V> targetType = (Class<V>) BOXED_TYPES.getOrDefault(type, type);

        if (!targetType.isEnum() && !SUPPORTED_TYPES.contains(targetType))
            throw new IllegalArgumentException("Field '" + field + "' cannot be read as " + type.getName()
                    + ". Only text, numbers, booleans, dates and enums are supported. Fields that are reconstructed "
                    + "by a mapper, e.g. tags, are only available on the indexed object itself.");

        if (value.numericValue() == null) {
            if (NUMERIC_TYPES.contains(targetType))
                return targetType.cast(parseNumber(value.stringValue(), targetType, field));
            if (targetType.equals(Date.class))
                throw new IllegalArgumentException("Field '" + field + "' is stored as text and cannot be read as date.");
        }

        return targetType.cast(convertStoredValue(value, targetType));
    }

    private static @Nullable Object parseNumber(@Nullable String text, Class<?> type, String field) {
        if (text == null)
            return null;
        try {
            if (type.equals(Integer.class))
                return Integer.valueOf(text);
            if (type.equals(Long.class))
                return Long.valueOf(text);
            if (type.equals(Double.class))
                return Double.valueOf(text);
            return Float.valueOf(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Field '" + field + "' is not a " + type.getSimpleName()
                    + " but '" + text + "'.", e);
        }
    }
}
