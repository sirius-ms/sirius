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

/**
 * One editable unit of the combined query, over which the pojo-agnostic renderer/editor engine works
 * (see GUI-SEARCHBAR-PLAN.md). A term is provenance-tagged, compiles to a {@link QueryNode} for
 * rendering, and knows how to remove itself and open its full editor - without the engine knowing
 * anything about the concrete backing model.
 * <p>
 * This is the P1 surface: rendering + removal + open-editor. Inline value editing (working value,
 * commit/revert) is added in P2, extending this interface.
 */
public interface FilterTerm {

    /** Stable identity within an editing session (used to stage/track this term). */
    @NotNull
    String id();

    @NotNull
    Provenance provenance();

    /** The query node this term contributes, used for rendering (and, for PANEL terms, faithful to the executed query). */
    @NotNull
    QueryNode toQueryNode();

    /**
     * Removes this term from the query by resetting the backing state it represents. The caller is
     * responsible for firing the resulting model-update event once (so a batch of removals fires once).
     */
    void remove();

    /** Opens the full editor for this term (for facets that are not edited inline). */
    void openEditor(@NotNull FilterEditorHost host);
}
