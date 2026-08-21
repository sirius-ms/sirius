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
 * How the values in an index are written, as a number a persisted index can be checked against.
 * <p>
 * The companion of {@link IndexSchema#fingerprint()}, which answers the other half of "does this stored index
 * still fit the code that reads it": the fingerprint sees the SHAPE of the index - which fields exist, how they
 * are held - and notices a rename or an added field on its own. What it cannot see is a change in what is
 * written INTO an unchanged field: a mapper formatting an adduct differently, an analyzer splitting words
 * differently, a value normalised where it was not before. Those leave a stored index whose documents are shaped
 * correctly and hold the wrong terms, which no automatic comparison catches.
 * <p>
 * <b>Bump this when a change makes previously written documents wrong</b>, whatever the shape says. Getting the
 * decision wrong in one direction costs everyone one rebuild; in the other it leaves stale terms in place and
 * queries quietly missing features, so err towards bumping.
 */
public final class IndexFormat {

    /**
     * Version 1 - the first version that is checked at all. Every index written before this existed reports no
     * version and is rebuilt once, which is what makes the check retroactive.
     */
    public static final int VERSION = 1;

    private IndexFormat() {
    }
}
