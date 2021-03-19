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

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ge28quv on 16/05/17.
 */
public class FormulaFactory {
    private static FormulaFactory instance;

    private Map<String, MolecularFormula> formulaMap;


    public static FormulaFactory getInstance(){
        if (instance==null) instance = new FormulaFactory();
        return instance;
    }

    public FormulaFactory() {
        this.formulaMap = new HashMap<>();
    }

    public MolecularFormula getFormula(String formulaString){
        MolecularFormula mf = formulaMap.get(formulaString);
        if (mf ==null) {
            mf = MolecularFormula.parseOrThrow(formulaString);
            formulaMap.put(formulaString, mf);
        }
        return mf;
    }

}
