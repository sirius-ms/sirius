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

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import io.sirius.ms.sdk.model.SearchableField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What one row of the autocomplete list shows. Field rows carry two competing pieces of information
 * next to the name - the field's description and its fully-qualified path - and only one of them fits
 * behind the name: the <b>description</b> wins, because a path the user can neither read at a glance
 * nor act on says less than the sentence explaining what the field means. The path fills that spot
 * only when the field has no description.
 * <p>
 * The fully-qualified name is never lost: it is the first line of the row's tooltip, followed by the
 * description in full (the dimmed row text is truncated to one line, the tooltip is not).
 */
public final class SuggestionDisplay {

    private SuggestionDisplay() {
    }

    /**
     * One rendered row: the name as shown, the dimmed text behind it (null = nothing) and the hover
     * tooltip (null = none).
     */
    public record Row(@NotNull String display, @Nullable String dimmed, @Nullable String tooltip) {
    }

    /**
     * Composes the row for {@code suggestion}; field names are shortened per {@code mode} (using the
     * field's own {@code significantSuffixLength}, the backend's notion of how much tail is
     * significant).
     */
    public static Row of(@NotNull TokenInputModel.Suggestion suggestion, @NotNull FieldDisplay.Mode mode) {
        if (suggestion instanceof TokenInputModel.Suggestion.FieldSuggestion fieldSuggestion)
            return fieldRow(fieldSuggestion.field(), mode);

        String description = blankToNull(suggestion.description());
        return new Row(suggestion.display(), description, tooltip(description));
    }

    private static Row fieldRow(@NotNull SearchableField field, @NotNull FieldDisplay.Mode mode) {
        String name = field.getName();
        int suffixLength = field.getSignificantSuffixLength() != null ? field.getSignificantSuffixLength() : 1;
        String display = FieldDisplay.of(name, mode, suffixLength);
        String description = blankToNull(field.getDescription());
        // the path is the fallback, and only when it adds something the display does not already show
        String dimmed = description != null ? description : (display.equals(name) ? null : name);
        return new Row(display, dimmed, tooltip(name, description));
    }

    @Nullable
    private static String tooltip(@Nullable String... lines) {
        List<String> present = new ArrayList<>(lines.length);
        for (String line : lines)
            if (line != null)
                present.add(line);
        return present.isEmpty() ? null : GuiUtils.formatToolTip(present);
    }

    @Nullable
    private static String blankToNull(@Nullable String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
