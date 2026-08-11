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

import lombok.Getter;

/**
 * Comparison operator of a numeric (or date/time) query clause. Declaration order is the order
 * offered in the operator dropdown, the inclusive range being the default like in the javascript
 * search bar.
 */
@Getter
public enum NumberOp {
    RANGE_INCLUSIVE("[a TO b] (inclusive)", "[ TO ]"),
    RANGE_EXCLUSIVE("{a TO b} (exclusive)", "{ TO }"),
    GTE(">=", ">="),
    GT(">", ">"),
    LTE("<=", "<="),
    LT("<", "<"),
    EQ("=", "=");

    /**
     * Explanatory text for the operator dropdown.
     */
    private final String label;
    /**
     * Terse form shown on committed chips and staged token fragments.
     */
    private final String symbol;

    NumberOp(String label, String symbol) {
        this.label = label;
        this.symbol = symbol;
    }

    /**
     * True for the two-valued range operators; all others take a single value.
     */
    public boolean isRange() {
        return this == RANGE_INCLUSIVE || this == RANGE_EXCLUSIVE;
    }

    @Override
    public String toString() {
        return label;
    }
}
