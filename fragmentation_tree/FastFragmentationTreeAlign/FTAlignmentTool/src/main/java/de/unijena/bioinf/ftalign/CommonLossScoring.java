
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ftalign;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.HashSet;

/**
 * Created by kaidu on 11.08.14.
 */
public class CommonLossScoring extends StandardScoring {
    public final static String[] LOSSES = new String[]{
            "H2O", "C2H2", "HO3P", "CH3", "H3O4P", "C2H4", "H3N", "CHN", "CS", "C2H6",
            "HS", "H2S", "CO", "CH3N", "C2H2O", "C4H4", "C3H2O", "C4H6", "CH4", "C3H4O",
            "OS", "O2S", "HF", "CH2S", "CH2", "C3H6", "C2H2S", "C2H7N", "C5H10", "CH2O2",
            "CH2O", "C2O2", "C4H2", "C4H8", "C5H8", "H2", "C2H4O", "C2H5N", "Br", "HBr", "Cl",
            "HCl", "CO2", "C6H6", "CHNO", "C2H6O", "S", "I", "HI", "CH4S", "CHNS", "CHClO", "C2H3Cl"
    };
    private HashSet<MolecularFormula> commonLosses;

    public CommonLossScoring(boolean useFragment) {
        super(useFragment);
        this.commonLosses = new HashSet<MolecularFormula>();
        for (String l : LOSSES) commonLosses.add(MolecularFormula.parseOrThrow(l));
    }

    @Override
    public float scoreFormulas(MolecularFormula left, MolecularFormula right, boolean isLoss) {
        if (!isLoss) {
            if (left.equals(right)) return super.scoreFormulas(left, right, isLoss);
            if (left.isSubtractable(right)) {
                final MolecularFormula diff = left.subtract(right);
                if (commonLosses.contains(diff)) return matchScore;
                else return super.scoreFormulas(left, right, isLoss);
            } else if (right.isSubtractable(left)) {
                final MolecularFormula diff = right.subtract(left);
                if (commonLosses.contains(diff)) return matchScore;
                else return super.scoreFormulas(left, right, isLoss);
            } else {
                return super.scoreFormulas(left, right, isLoss);
            }
        } else return super.scoreFormulas(left, right, isLoss);
    }
}
