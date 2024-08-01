/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.model.lcms;

public enum Polarity {
    POSITIVE(1), NEGATIVE(-1), UNKNOWN(0);

    public final int charge;

    public static Polarity of(int charge) {
        switch (charge) {
            case 0: return UNKNOWN;
            case 1: return POSITIVE;
            case -1: return NEGATIVE;
        }
        throw new IllegalArgumentException("unknown polarity " + charge);

    }

    Polarity(int charge) {
        this.charge = charge;
    }

    public static Polarity fromCharge(int charge) {
        switch (charge) {
            case 1:
                return POSITIVE;
            case -1:
                return NEGATIVE;
            case 0:
                return UNKNOWN;
            default:
                throw new IllegalArgumentException("Multi charges are not Supported!");
        }
    }
}
