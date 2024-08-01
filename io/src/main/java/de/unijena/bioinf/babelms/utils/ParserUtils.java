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

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ParserUtils {
    public static void checkMolecularFormula(MutableMs2Experiment exp){
        if (exp.getMolecularFormula() == null) {
            try {
                if (exp.hasAnnotation(InChI.class)) {
                    exp.setMolecularFormula(exp.getAnnotationOrThrow(InChI.class).extractFormulaOrThrow());
                } else if (exp.hasAnnotation(Smiles.class)) {
                    exp.setMolecularFormula(SmilesUCdk.formulaFromSmiles(
                            exp.getAnnotationOrThrow(Smiles.class).smiles));
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(ParserUtils.class).error("Error when extracting molecular formula from smiles or inchi.", e);
            }
        }

        if (exp.getMolecularFormula() != null)
            exp.setAnnotation(Whiteset.class, Whiteset.ofNeutralizedFormulas(List.of(exp.getMolecularFormula()), ParserUtils.class));
    }
}
