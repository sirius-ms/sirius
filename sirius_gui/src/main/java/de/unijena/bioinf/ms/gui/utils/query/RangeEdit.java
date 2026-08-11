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

import java.util.function.BiConsumer;

/**
 * An inline-editable numeric range of a filter term: the current bounds, the absolute bounds they are
 * clamped to, and how to apply a new {@code [min, max]} to the backing model. Lets the query-builder
 * edit a range chip in place (m/z, RT, confidence, ...) without knowing the concrete model.
 *
 * @param setter applies a new {@code (min, max)} to the backing model (does not fire an update itself)
 */
public record RangeEdit(double currentMin, double currentMax, double lowerBound, double upperBound,
                        @NotNull BiConsumer<Double, Double> setter) {

    /** Whether the current upper bound is open (equal to the absolute maximum). */
    public boolean upperOpen() {
        return currentMax >= upperBound;
    }

    /** Whether the current lower bound is open (equal to the absolute minimum). */
    public boolean lowerOpen() {
        return currentMin <= lowerBound;
    }
}
