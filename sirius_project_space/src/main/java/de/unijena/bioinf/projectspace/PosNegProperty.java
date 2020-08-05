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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import org.jetbrains.annotations.NotNull;

public abstract class PosNegProperty<Data> implements ProjectSpaceProperty {
    protected final Data positive;
    protected final Data negative;

    protected PosNegProperty(Data positive, Data negative) {
        this.positive = positive;
        this.negative = negative;
    }


    public Data getPositive() {
        return positive;
    }

    public Data getNegative() {
        return negative;
    }

    public Data getByIonType(@NotNull PrecursorIonType ionType) {
        return getByCharge(ionType.getCharge());
    }

    public Data getByCharge(int charge) {
        return charge < 0 ? negative : positive;
    }
}
