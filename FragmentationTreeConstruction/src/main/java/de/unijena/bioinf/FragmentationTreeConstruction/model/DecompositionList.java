/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;

import java.util.ArrayList;
import java.util.List;

public class DecompositionList {

    private final List<ScoredMolecularFormula> decompositions;

    public static DecompositionList fromFormulas(List<MolecularFormula> formulas) {
        final ArrayList<ScoredMolecularFormula> decompositions = new ArrayList<ScoredMolecularFormula>(formulas.size());
        for (MolecularFormula f : formulas) decompositions.add(new ScoredMolecularFormula(f,0d));
        return new DecompositionList(decompositions);

    }

    public DecompositionList(List<ScoredMolecularFormula> decompositions) {
        this.decompositions = decompositions;
    }

    public List<ScoredMolecularFormula> getDecompositions() {
        return decompositions;
    }
}
