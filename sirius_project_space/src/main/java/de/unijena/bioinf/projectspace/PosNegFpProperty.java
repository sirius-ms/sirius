/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;

import java.util.Optional;

public abstract class PosNegFpProperty<F extends FingerprintVersion, D extends FingerprintData<F>> extends PosNegProperty<D> {
    protected PosNegFpProperty(D positive, D negative) {
        super(positive, negative);
    }

    public boolean compatible(PosNegFpProperty<F, D> other) {
        return compatible(other.getPositive(), other.getNegative());
    }

    public boolean compatible(D positive, D negative) {
        // check if all compatible -> return true for null comparisons
        return (positive != null ? Optional.ofNullable(getPositive()).map(d -> d.identical(positive)).orElse(true) : true)
            && (negative != null ? Optional.ofNullable(getNegative()).map(d -> d.identical(negative)).orElse(true) : true);
    }
}