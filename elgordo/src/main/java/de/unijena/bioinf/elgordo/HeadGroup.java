/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.elgordo;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.Objects;

public class HeadGroup {

    public final static HeadGroup
            NoHeadGroup = new HeadGroup(""),
            Glycerol = new HeadGroup("C3H8O3"),
            Glyceroltrimethylhomoserin =  new HeadGroup("C10H21O5N"),
            Galactosylglycerol = new HeadGroup("C9H18O8"),
            Digalactosyldiacylglycerol = new HeadGroup("C15H28O13"),
            Sulfoquinovosylglycerols = new HeadGroup("C9H18O10S"),
            Glycerophosphocholines = new HeadGroup("C8H20O6PN"),
            Glycerophosphoethanolamines = new HeadGroup("C5H14O6PN"),
            Glycerophosphoserines = new HeadGroup("C6H14O8NP"),
            Glycerophosphoglycerols = new HeadGroup("C6H15O8P"),
            Glycerophosphoinositols = new HeadGroup("C9H19PO11"),
            Glycerophosphates = new HeadGroup("C3H9O6P"),
            Glycerophosphoglycerophosphoglycerols = new HeadGroup("C9H22O13P2"),
            Phosphocholin = new HeadGroup("C5H12NO3P"),
            Hexose = new HeadGroup("C6H10O5");
    ;

    protected MolecularFormula molecularFormula;
    public HeadGroup(MolecularFormula molecularFormula) {
        this.molecularFormula = molecularFormula;
    }
    private HeadGroup(String f) {
        this(MolecularFormula.parseOrThrow(f));
    }

    public MolecularFormula getMolecularFormula() {
        return molecularFormula;
    }

    public boolean isSphingolipid() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeadGroup headGroup = (HeadGroup) o;
        return molecularFormula.equals(headGroup.molecularFormula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(molecularFormula);
    }
}
