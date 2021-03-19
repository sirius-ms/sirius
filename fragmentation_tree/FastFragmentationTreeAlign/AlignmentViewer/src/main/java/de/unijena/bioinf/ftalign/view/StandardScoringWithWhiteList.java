
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

package de.unijena.bioinf.ftalign.view;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ftalign.StandardScoring;

import java.util.ArrayList;

public class StandardScoringWithWhiteList extends StandardScoring {

    private ArrayList<MolecularFormula> whiteList;

    public StandardScoringWithWhiteList(boolean useFragment) {
        super(true);
        this.whiteList = new ArrayList<MolecularFormula>();
    }

    @Override
    public float scoreFormulas(MolecularFormula left, MolecularFormula right, boolean isLoss) {
        final MolecularFormula xx;
        if (left.isSubtractable(right)) {
            xx = left.subtract(right);
        } else if (right.isSubtractable(left)) {
            xx = right.subtract(left);
        } else {
            final int diff = left.numberOfDifferenceHeteroAtoms(right);
            return (isLoss ? missmatchPenalty + (diff) * penaltyForEachNonHydrogen
                    : lossMissmatchPenalty + (diff) * lossPenaltyForEachNonHydrogen);
        }
        final int diff = (xx.atomCount() - xx.numberOfHydrogens());
        if (diff == 0) {
            return (isLoss ? matchScore + (left.atomCount() - left.numberOfHydrogens()) * scoreForEachNonHydrogen
                    : lossMatchScore + (left.atomCount() - left.numberOfHydrogens()) * lossScoreForEachNonHydrogen);
        } else {
            return (isLoss ? matchScore + (diff) * penaltyForEachNonHydrogen
                    : lossMatchScore + (diff) * lossPenaltyForEachNonHydrogen);
        }
    }

    @Override
    public float scoreJoinFormulas(MolecularFormula left, MolecularFormula right) {
        return scoreFormulas(left, right, true);
    }
}
