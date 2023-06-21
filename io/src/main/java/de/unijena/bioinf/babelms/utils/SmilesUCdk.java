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

package de.unijena.bioinf.babelms.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.SmilesU;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SmilesUCdk extends SmilesU {
    public static MolecularFormula formulaFromSmiles(String smiles) throws InvalidSmilesException, UnknownElementException {
        SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer iAtomContainer = smilesParser.parseSmiles(smiles);
        if (iAtomContainer == null) return null;
        String s = MolecularFormulaManipulator.getString(MolecularFormulaManipulator.getMolecularFormula(iAtomContainer));
        if (s == null) return null;
        int formalCharge = getFormalChargeFromSmiles(smiles);
        MolecularFormula formula = MolecularFormula.parse(s);
        if (formalCharge == 0) return formula;
        else if (formalCharge < 0) {
            return formula.add(MolecularFormula.parse(String.valueOf(Math.abs(formalCharge) + "H")));
        } else {
            return formula.subtract(MolecularFormula.parse(String.valueOf(formalCharge + "H")));
        }
    }
}
