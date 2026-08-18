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

/**
 * How a field is held in the lucene index, as the index itself sees it.
 * <p>
 * Deliberately coarser than what an API client is told: the index does not know an enum from any other keyword,
 * nor a boolean from either. Refining this into something a user can act on is the job of whoever describes the
 * index, not of the index.
 */
public enum LuceneKind {
    /** An unanalyzed term - matched as a whole or not at all. */
    KEYWORD,
    /** Analyzed into words. */
    TEXT,
    INTEGER,
    LONG,
    DOUBLE,
    FLOAT,
    /** A point with a date format, queried as yyyy-MM-dd. */
    DATE,
    /** A point with a time format, queried as HH:mm:ss. */
    TIME
}
