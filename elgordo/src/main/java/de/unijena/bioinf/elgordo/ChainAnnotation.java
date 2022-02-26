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
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public class ChainAnnotation extends LipidAnnotation{

    public ChainAnnotation(Target target, MolecularFormula chainFormula, MolecularFormula peakFormula, PrecursorIonType ionType, MolecularFormula modification, LipidChain chain) {
        super(target, chainFormula, peakFormula, ionType,modification);
        this.chain = chain;
    }

    public LipidChain getChain() {
        return chain;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && chain.equals(((ChainAnnotation)o).chain);
    }

    @Override
    public int hashCode() {
        return super.hashCode()*chain.hashCode();
    }

    private final LipidChain chain;



}
