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
 * <p>
 * Rows that carry the connector joining them to the query ({@link TokenInputModel.Suggestion.Joined})
 * report it separately from the name, so the renderer can set it apart while the row still reads as one
 * pick rather than two.
 */
public final class SuggestionDisplay {

    private SuggestionDisplay() {
    }

    /**
     * One rendered row: the connector in front of it (null = none), the action label after that (null =
     * none), the name as shown, the dimmed text behind it (null = nothing) and the hover tooltip (null =
     * none).
     * <p>
     * The connector is kept apart from the name rather than pasted in front of it, so the renderer can
     * set it apart the way the chips zone does - small, italic, dimmed - and the name still reads as the
     * one thing the row is about. The label is kept apart for the opposite reason: on the rows that carry
     * one, the name is the typed text, the same on each of them, and the label is what the row is about.
     */
    public record Row(@Nullable String connector, @Nullable String label, @NotNull String display,
                      @Nullable String dimmed, @Nullable String tooltip) {
        public Row(@Nullable String connector, @NotNull String display, @Nullable String dimmed,
                   @Nullable String tooltip) {
            this(connector, null, display, dimmed, tooltip);
        }
    }

    /**
     * How much of the typed text a labelled row shows. The label must stay in view, and the row is one
     * line: a pasted filter runs to hundreds of characters, so the text is shortened in the middle -
     * both ends stay, which is what tells one pasted filter from another - and kept in full in the tooltip.
     */
    static final int TYPED_TEXT_LIMIT = 64;

    /**
     * Composes the row for {@code suggestion}; field names are shortened per {@code mode} (using the
     * field's own {@code significantSuffixLength}, the backend's notion of how much tail is
     * significant).
     */
    public static Row of(@NotNull TokenInputModel.Suggestion suggestion, @NotNull FieldDisplay.Mode mode) {
        // a joined row is its target with the connector in front; everything else about it - the
        // shortened field name, the description, the tooltip - is the target's
        if (suggestion instanceof TokenInputModel.Suggestion.Joined joined) {
            Row target = of(joined.target(), mode);
            // the joint between the two blocks opens the user's block, so the row shows that brace: it
            // says the connector is the big one, not a connector inside the query
            return new Row(joined.logic() + (joined.blockJoin() ? " (" : ""), target.label(),
                    target.display(), target.dimmed(), target.tooltip());
        }
        if (suggestion instanceof TokenInputModel.Suggestion.FieldSuggestion fieldSuggestion)
            return fieldRow(fieldSuggestion.field(), mode);

        String description = blankToNull(suggestion.description());
        String label = suggestion.actionLabel();
        if (label != null) {
            // the text is the typed text, shortened to leave the label room; the tooltip keeps it whole
            String text = suggestion.display();
            return new Row(null, label, shortenMiddle(text, TYPED_TEXT_LIMIT), description,
                    tooltip(text, description));
        }
        return new Row(null, suggestion.display(), description, tooltip(description));
    }

    /** {@code text} cut to {@code limit} characters by an ellipsis in the middle; unchanged when it fits. */
    static String shortenMiddle(@NotNull String text, int limit) {
        if (text.length() <= limit)
            return text;
        int head = (limit - 1) / 2;
        int tail = limit - 1 - head;
        return text.substring(0, head) + "\u2026" + text.substring(text.length() - tail);
    }

    private static Row fieldRow(@NotNull SearchableField field, @NotNull FieldDisplay.Mode mode) {
        String name = field.getName();
        int suffixLength = field.getSignificantSuffixLength() != null ? field.getSignificantSuffixLength() : 1;
        String display = FieldDisplay.of(name, mode, suffixLength);
        String description = blankToNull(field.getDescription());
        // the path is the fallback, and only when it adds something the display does not already show
        String dimmed = description != null ? description : (display.equals(name) ? null : name);
        return new Row(null, display, dimmed, tooltip(name, description));
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
