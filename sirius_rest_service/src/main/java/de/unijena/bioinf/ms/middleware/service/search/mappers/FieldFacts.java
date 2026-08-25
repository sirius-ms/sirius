/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search.mappers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * What the index knows about one of its fields, recorded while the index is configured.
 * <p>
 * These are facts, not descriptions: how the field is held, whether it can be sorted or searched without naming
 * it, and where it came from. Everything a client is eventually told - its API type, its documentation, the
 * values it can take - is derived from this by the describer and is no concern of the index.
 *
 * @param name               the field name a query uses; a map-like field ends in {@code .*} until its keys
 *                           are known
 * @param kind               how the field is held in the index
 * @param analyzed           whether the field is split into words (false for keyword and numeric fields)
 * @param sortable           whether the index carries doc values to sort by this field
 * @param defaultSearchField whether a query term without a field name searches this field
 * @param javaType           the type the value is read from, for a field mapped straight from a java field;
 *                           null for a field a {@link FieldMapper} contributes on its own terms. The index only
 *                           needs the kind, but the java type is what tells an enum from any other keyword, so
 *                           it is kept for whoever asks.
 * @param declaredBy         the java field whose annotation put this field in the index - the field itself when
 *                           it is mapped directly, and the field carrying the mapper when a mapper contributed
 *                           it. Carries the annotations a describer reads, so describing the index takes no
 *                           second walk over the model that could drift from this one.
 */
public record FieldFacts(
        @NotNull String name,
        @NotNull LuceneKind kind,
        boolean analyzed,
        boolean sortable,
        boolean defaultSearchField,
        @Nullable Class<?> javaType,
        @NotNull Field declaredBy
) {
}
