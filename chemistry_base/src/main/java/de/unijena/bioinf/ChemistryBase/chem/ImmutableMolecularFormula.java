
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

package de.unijena.bioinf.ChemistryBase.chem;

import java.util.Arrays;

public class ImmutableMolecularFormula extends MolecularFormula {

    private final short[] amounts;
    private final TableSelection selection;
    private final double mass;
    private final int hash;

    ImmutableMolecularFormula(MolecularFormula formula) {
        this(formula.getTableSelection(), formula.buffer(), formula.getMass(), formula.hashCode());
    }

    private ImmutableMolecularFormula(TableSelection selection, short[] buffer, double mass, int hash) {
        this.amounts = buffer.clone();
        this.selection = selection;
        this.mass = mass;
        this.hash = hash;
    }

    ImmutableMolecularFormula(TableSelection selection, short[] buffer) {
        int i = buffer.length - 1;
        while (i >= 0 && buffer[i] == 0) --i;
        this.amounts = Arrays.copyOf(buffer, i + 1);
        this.selection = selection;
        this.mass = calcMass();
        this.hash = super.hashCode();
    }
    /*
    protected int calculateHash() {
        final short[] buf = buffer();
        if (buf.length==0) return 0;
        int hash = 0;
        hash |= buf[0];
        if (buf.length == 1) return hash;
        hash |= buf[1]<<8;
        for (int i=1; i < buf.length; ++i) {
            hash |= buf[i]<<(8+i*3);
        }
        return hash;
    }
    */

    @Override
    public boolean equals(MolecularFormula formula) {
        if (hash != formula.hashCode()) return false;
        return super.equals(formula);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public int getIntMass() {
        return calcIntMass();
    }

    @Override
    public TableSelection getTableSelection() {
        return selection;
    }

    @Override
    protected short[] buffer() {
        return amounts;
    }

}
