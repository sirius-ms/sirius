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

package de.unijena.bioinf.ms.gui.utils.query;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The item + logic lists of one nesting level of the query tree: the top level of the builder, or
 * the content of a {@link QueryGroup}. Same invariant as the group:
 * {@code logics.size() == max(0, items.size() - 1)}.
 */
public record QueryContainer(@NotNull List<QueryNode> items, @NotNull List<LogicOp> logics) {

    public QueryContainer {
        items = List.copyOf(items);
        logics = List.copyOf(logics);
    }

    public static QueryContainer empty() {
        return new QueryContainer(List.of(), List.of());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
