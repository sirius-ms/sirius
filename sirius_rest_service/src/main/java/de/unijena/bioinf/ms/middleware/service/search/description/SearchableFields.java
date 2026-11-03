/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search.description;

import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneKind;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Turns what the index knows into what a client is told.
 */
public final class SearchableFields {

    private SearchableFields() {
    }

    /**
     * The type a client sees for a value of the given java type, or null if such a value cannot be searched.
     * <p>
     * Finer than the index's own notion: an enum and a boolean are both keywords to lucene, but a client can
     * only offer their values if it is told which is which.
     */
    @Nullable
    public static SearchableField.FieldType fieldTypeOf(@NotNull Class<?> type) {
        if (type.equals(String.class))
            return SearchableField.FieldType.TEXT;
        if (type.equals(int.class) || type.equals(Integer.class))
            return SearchableField.FieldType.INTEGER;
        if (type.equals(long.class) || type.equals(Long.class))
            return SearchableField.FieldType.LONG;
        if (type.equals(double.class) || type.equals(Double.class))
            return SearchableField.FieldType.DOUBLE;
        if (type.equals(float.class) || type.equals(Float.class))
            return SearchableField.FieldType.FLOAT;
        if (type.equals(boolean.class) || type.equals(Boolean.class))
            return SearchableField.FieldType.BOOLEAN;
        if (type.equals(Date.class))
            return SearchableField.FieldType.DATE;
        if (type.isEnum())
            return SearchableField.FieldType.ENUM;
        return null;
    }

    /**
     * The type a client sees for a field the index only knows as a kind - one a mapper contributed, where there
     * is no java type to be more precise about.
     */
    @NotNull
    public static SearchableField.FieldType fieldTypeOf(@NotNull LuceneKind kind) {
        return switch (kind) {
            case KEYWORD, TEXT -> SearchableField.FieldType.TEXT;
            case INTEGER -> SearchableField.FieldType.INTEGER;
            case LONG -> SearchableField.FieldType.LONG;
            case DOUBLE -> SearchableField.FieldType.DOUBLE;
            case FLOAT -> SearchableField.FieldType.FLOAT;
            case DATE -> SearchableField.FieldType.DATE;
            case TIME -> SearchableField.FieldType.TIME;
        };
    }

    /**
     * Maps a tag {@link ValueType} to the {@link SearchableField.FieldType} exposed to API users.
     * Consistent with how tag values are indexed by the TagMapper: NONE tags are presence flags queried as
     * {@code tags.<name>:true}, hence BOOLEAN.
     */
    public static SearchableField.FieldType fieldTypeOf(ValueType valueType) {
        return switch (valueType) {
            case TEXT -> SearchableField.FieldType.TEXT;
            case INTEGER -> SearchableField.FieldType.INTEGER;
            case REAL -> SearchableField.FieldType.DOUBLE;
            case DATE -> SearchableField.FieldType.DATE;
            case TIME -> SearchableField.FieldType.TIME;
            case BOOLEAN, NONE -> SearchableField.FieldType.BOOLEAN;
        };
    }

    /**
     * Describes the dynamic search field of a project tag ({@code tags.<tagName>}), consistent with how tag
     * values are indexed and queried.
     *
     * @param possibleValues the values the tag definition restricts this tag to (in query form), or null if it
     *                       accepts any value
     * @param description    what the tag definition says this tag means, or null if it says nothing - then the
     *                       field is described by naming the tag, which is all that is known about it
     */
    public static SearchableField toTagSearchableField(@NotNull String fieldName, @NotNull String tagName,
                                                       @NotNull ValueType valueType, @Nullable List<String> possibleValues,
                                                       @Nullable String description) {
        return SearchableField.builder()
                .name(fieldName)
                .fieldType(fieldTypeOf(valueType))
                .fullTextSearch(valueType == ValueType.TEXT)
                .significantSuffixLength(2) // "tags.<tagName>" - the tag field plus the tag key
                .possibleValues(tagPossibleValues(valueType, possibleValues))
                .description((description != null && !description.isBlank()
                        ? description : "Project tag '" + tagName + "'")
                        // what a definition cannot say for itself: absence of a value-less tag is the negation
                        + (valueType == ValueType.NONE ? "; presence flag, search for value 'true'" : ""))
                .build();
    }

    /**
     * Same precedence as for annotated fields: a declared vocabulary is the more specific statement and wins,
     * otherwise the values follow from the type. Neither a boolean nor a value-less tag can declare values (the
     * tag definition rejects that), and the two differ in what they hold: a boolean tag is written as true or
     * false, while a value-less tag is a presence flag written only as true - offering it false would offer a
     * value that matches nothing, whatever the tag. Absence is matched by negating, which needs a second clause
     * to negate against.
     */
    @Nullable
    private static List<String> tagPossibleValues(@NotNull ValueType valueType, @Nullable List<String> declared) {
        if (declared != null && !declared.isEmpty())
            return declared;
        return switch (valueType) {
            case BOOLEAN -> List.of("true", "false");
            case NONE -> List.of("true");
            default -> null;
        };
    }

    /**
     * Expands dynamic-key field templates into the keys that are actually present in the index.
     * <p>
     * Map-like index fields whose Lucene key ends in a dynamic segment (e.g.
     * {@code topAnnotations.matchedDatabases.*}, {@code qualities.*},
     * {@code ...molecularFormula.*}) are described with a trailing {@code .*}, which is not a usable
     * query token. Given the concrete field names present in the index, every such template is
     * replaced by one {@link SearchableField} per matching key - cloning the template's type, flags,
     * {@code possibleValues} and description - so the autocomplete offers real field names. Fields
     * without a {@code .*} terminal pass through unchanged. A template with no materialized key is
     * dropped (there is nothing concrete to query yet). Concrete fields are ordered by name.
     *
     * @param fields            the (static) searchable fields, some possibly {@code prefix.*} templates
     * @param indexedFieldNames the concrete field names present in the index (e.g. from the segment field infos)
     */
    public static List<SearchableField> expandDynamicKeyFields(@NotNull List<SearchableField> fields,
                                                               @NotNull Collection<String> indexedFieldNames) {
        List<SearchableField> result = new ArrayList<>(fields.size());
        for (SearchableField field : fields) {
            String name = field.getName();
            if (name == null || !name.endsWith(".*")) {
                result.add(field); // not a dynamic-key template - keep as is
                continue;
            }
            String prefix = name.substring(0, name.length() - 1); // strip only the '*', keep the trailing '.'
            indexedFieldNames.stream()
                    .filter(indexed -> indexed.length() > prefix.length() && indexed.startsWith(prefix))
                    .sorted()
                    .forEach(concrete -> {
                        // meaningful tail = the structural field (1) + however many segments the dynamic key spans
                        int keySegments = concrete.substring(prefix.length()).split("\\.").length;
                        result.add(SearchableField.builder()
                                .name(concrete)
                                .fieldType(field.getFieldType())
                                .fullTextSearch(field.isFullTextSearch())
                                .sortable(field.isSortable())
                                .defaultSearchField(field.isDefaultSearchField())
                                .possibleValues(field.getPossibleValues())
                                .description(field.getDescription())
                                .significantSuffixLength(1 + keySegments)
                                .build());
                    });
        }
        return result;
    }
}
