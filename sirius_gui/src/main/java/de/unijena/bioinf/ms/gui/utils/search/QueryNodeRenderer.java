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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /**
     * A read-only chip for one query node.
     *
     * @param onClick optional action when the chip is clicked (e.g. open the query builder); null = inert
     */
    public static ChipComponent     chip(@NotNull QueryNode node, @NotNull ChipComponent.Style style,
                                     @Nullable Runnable onClick) {
        if (node instanceof QueryClause clause && clause.isFreeText())
            return new ChipComponent((clause.negated() ? "NOT " : "") + "“" + clause.value1() + "”",
                    "Full-text search in the default fields", style, onClick, null);

        String text = node instanceof QueryClause clause
                ? (clause.negated() ? "NOT " : "") + clause.field() + " " + SearchBarOverlay.clauseBody(clause)
                : LuceneQueryCompiler.render(node); // groups collapse to their compiled form
        return new ChipComponent(text, LuceneQueryCompiler.render(node), style, onClick, null);
    }
}
