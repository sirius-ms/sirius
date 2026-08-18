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

import java.util.List;

/**
 * The fields an indexed type is held under, in the order the model declares them.
 * <p>
 * Produced while the index is configured, so it cannot disagree with the index it describes. This is everything
 * the index says about itself; turning it into something an API client can read happens elsewhere.
 *
 * @see FieldFacts
 */
public record IndexSchema(@NotNull List<FieldFacts> fields) {

    public static final IndexSchema EMPTY = new IndexSchema(List.of());

    public IndexSchema(@NotNull List<FieldFacts> fields) {
        this.fields = List.copyOf(fields);
    }
}
