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
 * A single field clause of the query builder, e.g. {@code ionMass:[300 TO 400]} or
 * {@code name:"caffeine"}. Carries a {@link NumberOp} exactly when the field is numeric
 * (incl. date/time); text-like clauses (text, enum, boolean) have none and render as
 * {@code field:value}.
 *
 * @param value2 upper bound for the two-valued range operators, null/empty otherwise
 */
public record QueryClause(@NotNull String id, @NotNull String field, @Nullable NumberOp op,
                          @NotNull String value1, @Nullable String value2,
                          boolean negated) implements QueryNode {

    public static QueryClause text(@NotNull String field, @NotNull String value, boolean negated) {
        return new QueryClause(QueryNode.nextId("clause"), field, null, value, null, negated);
    }

    public static QueryClause numeric(@NotNull String field, @NotNull NumberOp op,
                                      @NotNull String value1, @Nullable String value2, boolean negated) {
        return new QueryClause(QueryNode.nextId("clause"), field, op, value1, value2, negated);
    }

    @Override
    public QueryClause withNegated(boolean negated) {
        return new QueryClause(id, field, op, value1, value2, negated);
    }
}
