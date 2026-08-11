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

import java.util.concurrent.atomic.AtomicLong;

/**
 * One node of the search-bar query tree: either a single {@link QueryClause} or a parenthesized
 * {@link QueryGroup}. Nodes are immutable; tree operations ({@link QueryTreeOps}) return new trees.
 * The id identifies a node across those rebuilds (UI chips reference nodes by id).
 */
public sealed interface QueryNode permits QueryClause, QueryGroup {

    AtomicLong ID_SOURCE = new AtomicLong();

    static String nextId(String prefix) {
        return prefix + "-" + ID_SOURCE.incrementAndGet();
    }

    String id();

    boolean negated();

    QueryNode withNegated(boolean negated);
}
