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

package de.unijena.bioinf.ChemistryBase.fp;

public class SubstructureCountProperty extends SubstructureProperty {

    public SubstructureCountProperty(String smiles, int minimalCount) {
        super(smiles);
        this.minimalCount = minimalCount;
    }

    public SubstructureCountProperty(String smiles, String description, int minimalCount) {
        super(smiles, description);
        this.minimalCount = minimalCount;
    }

    public int minimalCount;

    public int getMinimalCount() {
        return minimalCount;
    }

    @Override
    public String toString() {
        return getSmarts() + " >= " + minimalCount;
    }

    @Override
    public String getDescription() {
        if (description!=null) {
            return toString() + " (" + description + ")";
        } else {
            return toString();
        }
    }
}
