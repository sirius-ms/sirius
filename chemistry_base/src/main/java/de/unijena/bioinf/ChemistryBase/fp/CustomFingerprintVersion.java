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

import java.util.List;

public class CustomFingerprintVersion extends FingerprintVersion{

    protected final String name;
    protected final List<MolecularProperty> properties;
    protected final int size;

    public CustomFingerprintVersion(String name, int size) {
        this.name = name;
        this.size = size;
        this.properties = null;
    }

    public CustomFingerprintVersion(String name, List<MolecularProperty> properties) {
        this.name = name;
        this.properties = properties;
        this.size = properties.size();
    }

    @Override
    public MolecularProperty getMolecularProperty(int index) {
        return properties==null ? new SpecialMolecularProperty("?") : properties.get(index);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
        if (fingerprintVersion instanceof CustomFingerprintVersion) {
            return ((CustomFingerprintVersion) fingerprintVersion).name == this.name;
        } else return false;
    }
}
