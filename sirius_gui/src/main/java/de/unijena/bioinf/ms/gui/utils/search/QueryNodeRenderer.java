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
import de.unijena.bioinf.ms.gui.utils.query.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Renders a {@link QueryNode} as a read-only, collapsed {@link ChipComponent} - one chip per node,
 * showing the clause in human-readable form ({@code field op value}, {@code "free text"}, groups
 * collapsed to their compiled lucene form) with the full compiled query as tooltip. Style-agnostic:
 * the caller picks {@link ChipComponent.Style#USER} for the user's own clauses or
 * {@link ChipComponent.Style#MODEL} for filter-panel-derived clauses.
 * <p>
 * This is the shared read-only rendering used by the collapsed bar (and, once the interactive editor
 * is refactored onto it, the overlay); it never mutates the query - editing lives in the overlay's
 * interactive builder.
 */
public final class QueryNodeRenderer {

    private QueryNodeRenderer() {
    }

    /** Convenience: extensive (fully-qualified) rendering, no per-field length lookup. */
    public static ChipComponent chip(@NotNull QueryNode node, @NotNull ChipComponent.Style style,
                                     @Nullable Runnable onClick) {
        return chip(node, style, onClick, FieldDisplay.Mode.EXTENSIVE, null);
    }

    /**
     * A read-only chip for one query node. The chip <b>label</b> shows field names per {@code mode}
     * (compact = the last {@code significantSuffixLength} segments, looked up via {@code suffixLengthByField});
     * the <b>tooltip</b> is always the fully-qualified compiled lucene (or the full-text hint).
     *
     * @param onClick             optional click action (e.g. open the query builder); null = inert
     * @param suffixLengthByField field name -> significant suffix length (nullable / may return null -> heuristic)
     */
    public static ChipComponent chip(@NotNull QueryNode node, @NotNull ChipComponent.Style style,
                                     @Nullable Runnable onClick, @NotNull FieldDisplay.Mode mode,
                                     @Nullable Function<String, Integer> suffixLengthByField) {
        String tooltip = node instanceof QueryClause clause && clause.isFreeText()
                ? "Full-text search in the default fields"
                : LuceneQueryCompiler.render(node);
        return new ChipComponent(label(node, mode, suffixLengthByField), tooltip, style, onClick, null);
    }

    /** The human-readable label of a node, with field names shown per {@code mode}. */
    static String label(@NotNull QueryNode node, @NotNull FieldDisplay.Mode mode,
                        @Nullable Function<String, Integer> suffixLengthByField) {
        if (node instanceof QueryClause clause) {
            if (clause.isFreeText())
                return (clause.negated() ? "NOT " : "") + "“" + clause.value1() + "”";
            return (clause.negated() ? "NOT " : "") + displayField(clause.field(), mode, suffixLengthByField)
                    + " " + SearchBarOverlay.clauseBody(clause);
        }
        QueryGroup group = (QueryGroup) node;
        StringBuilder body = new StringBuilder("(");
        for (int i = 0; i < group.items().size(); i++) {
            if (i > 0)
                body.append(' ').append(group.logics().get(i - 1)).append(' ');
            body.append(label(group.items().get(i), mode, suffixLengthByField));
        }
        body.append(')');
        return (group.negated() ? "NOT " : "") + body;
    }

    /** A single field name shown per {@code mode} (compact = last significant-suffix-length segments). */
    static String displayField(@NotNull String field, @NotNull FieldDisplay.Mode mode,
                               @Nullable Function<String, Integer> suffixLengthByField) {
        if (mode == FieldDisplay.Mode.EXTENSIVE || field.isEmpty())
            return field;
        Integer length = suffixLengthByField == null ? null : suffixLengthByField.apply(field);
        return length != null ? FieldDisplay.compact(field, length) : FieldDisplay.compact(field);
    }
}
