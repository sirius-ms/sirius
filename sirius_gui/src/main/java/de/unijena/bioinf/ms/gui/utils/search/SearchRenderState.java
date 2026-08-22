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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * The shared, mutable rendering state of the search bar and its overlay: the field-name display
 * {@link FieldDisplay.Mode} (toggled together on both surfaces), the {@link QueryMode} the builder
 * offers, and a resolver from a field name to its {@code significantSuffixLength} (from the
 * searchable-fields metadata). One instance is shared so the collapsed bar and the overlay always
 * render field names the same way, and so the query mode holds for the whole search bar.
 * <p>
 * Scoped to the surface that owns it: the search bar has one, the filter dialog builds its own, so
 * both settings are chosen where they are used (as the display mode already was). Session state, not
 * a persisted setting - both start at their default every run.
 */
public final class SearchRenderState {

    private final SearchableFieldsProvider fieldsProvider;
    private FieldDisplay.Mode mode = FieldDisplay.Mode.COMPACT; // compact by default
    private QueryMode queryMode = QueryMode.SIMPLE; // the shape most filters have; advanced on request

    public SearchRenderState(@NotNull SearchableFieldsProvider fieldsProvider) {
        this.fieldsProvider = fieldsProvider;
    }

    @NotNull
    public FieldDisplay.Mode mode() {
        return mode;
    }

    /** Flips compact &lt;-&gt; extensive. */
    public void toggleMode() {
        mode = mode == FieldDisplay.Mode.COMPACT ? FieldDisplay.Mode.EXTENSIVE : FieldDisplay.Mode.COMPACT;
    }

    @NotNull
    public QueryMode queryMode() {
        return queryMode;
    }

    /** Flips simple &lt;-&gt; advanced. */
    public void toggleQueryMode() {
        queryMode = queryMode == QueryMode.SIMPLE ? QueryMode.ADVANCED : QueryMode.SIMPLE;
    }

    /** field name -> significant suffix length from the searchable-fields metadata (null if unknown). */
    @NotNull
    public Function<String, @Nullable Integer> suffixLengthResolver() {
        return field -> fieldsProvider.getCached().stream()
                .filter(f -> field.equals(f.getName()))
                .map(SearchableField::getSignificantSuffixLength)
                .findFirst().orElse(null);
    }
}
