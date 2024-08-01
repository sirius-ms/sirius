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

package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.HashMap;

/**
 * pool of {@link MolecularFormula}. Enables usage of one single {@link MolecularFormula} instance per formula. This may reduce memory and improve speed of comparisons.
 */
public class MolecularFormulaPool {
    private static MolecularFormulaPool instance;
    private HashMap<MolecularFormula, MolecularFormula> formulaMap;

    public MolecularFormulaPool() {
        formulaMap = new HashMap<>();
    }

    public static MolecularFormulaPool getInstance(){
        if (instance==null) instance = new MolecularFormulaPool();
        return instance;
    }

    public MolecularFormula get(MolecularFormula mf) {
        MolecularFormula representative = formulaMap.get(mf);
        if (representative!=null) return representative;
        formulaMap.put(mf, mf);
        return mf;
    }

    public boolean contains(MolecularFormula mf) {
        return formulaMap.containsKey(mf);
    }
}
