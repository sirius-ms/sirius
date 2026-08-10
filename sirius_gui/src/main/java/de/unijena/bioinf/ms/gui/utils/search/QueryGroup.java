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

import java.util.List;

/**
 * A parenthesized group of the query builder. {@code logics} joins consecutive items, so it always
 * holds {@code max(0, items.size() - 1)} operators. A group may be temporarily empty while it is
 * open for typing; empty groups compile to nothing.
 */
public record QueryGroup(@NotNull String id, boolean negated,
                         @NotNull List<QueryNode> items,
                         @NotNull List<LogicOp> logics) implements QueryNode {

    public QueryGroup {
        items = List.copyOf(items);
        logics = List.copyOf(logics);
    }

    public static QueryGroup empty(boolean negated) {
        return new QueryGroup(QueryNode.nextId("group"), negated, List.of(), List.of());
    }

    @Override
    public QueryGroup withNegated(boolean negated) {
        return new QueryGroup(id, negated, items, logics);
    }

    public QueryGroup withContent(@NotNull List<QueryNode> items, @NotNull List<LogicOp> logics) {
        return new QueryGroup(id, negated, items, logics);
    }
}
