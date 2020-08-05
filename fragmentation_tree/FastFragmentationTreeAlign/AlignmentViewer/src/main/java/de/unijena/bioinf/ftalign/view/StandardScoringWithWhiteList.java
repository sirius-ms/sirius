
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
