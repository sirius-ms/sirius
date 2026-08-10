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

package de.unijena.bioinf.ms.middleware.model.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Describes one field of the search index that can be used in lucene search queries (searchQuery parameter).
 * Use this information to build valid queries, e.g. which fields support range queries ([300 TO 400])
 * and which support word based (full text) search.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchableField {

    /**
     * Value type of a searchable field. Determines which query clauses are valid for the field:
     * TEXT supports terms, phrases, wildcards and regular expressions; numeric types (INTEGER, LONG, DOUBLE,
     * FLOAT), DATE (yyyy-MM-dd) and TIME (HH\:mm\:ss) additionally support comparisons and range queries;
     * BOOLEAN matches true/false; ENUM matches one of the possibleValues.
     */
    @Schema(name = "SearchableFieldType", enumAsRef = true)
    public enum FieldType {TEXT, INTEGER, LONG, DOUBLE, FLOAT, BOOLEAN, DATE, TIME, ENUM}

    /**
     * Name of the field to be used in search queries, e.g. {@code ionMass:[300 TO 400]}.
     * Nested fields are addressed with dot notation. A trailing {@code .*} means the prefix is followed
     * by a dynamic key, e.g. {@code tags.<tagName>} or an element symbol in {@code molecularFormula.<element>}.
     */
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected String name;

    /**
     * Value type of this field. Determines which query clauses are valid (see SearchableFieldType).
     */
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected FieldType fieldType;

    /**
     * If true, the field content is split into words and can be searched word by word (full text search).
     * If false, TEXT fields only match as exact terms (though wildcards and regex are still possible).
     */
    protected boolean fullTextSearch;

    /**
     * If true, search results can be sorted by this field.
     */
    protected boolean sortable;

    /**
     * If true, this field is searched when a query term does not specify a field name.
     */
    protected boolean defaultSearchField;

    /**
     * For ENUM fields: the values this field can take. Null otherwise.
     */
    @Nullable
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected List<String> possibleValues;

    /**
     * Optional human-readable description of the field content, as shown in the API documentation.
     * Plain text suitable for direct display (e.g. tooltips); deliberate paragraph breaks are newlines.
     */
    @Nullable
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String description;
}
