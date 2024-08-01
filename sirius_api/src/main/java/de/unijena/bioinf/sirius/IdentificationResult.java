
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

package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;

//this is basically just a scored tree
public final class IdentificationResult extends SScored<FTree, FormulaScore> implements Cloneable {

    public static IdentificationResult withPrecursorIonType(IdentificationResult ir, PrecursorIonType ionType, boolean preserveOriginalTreeScore) {

        PrecursorIonType adduct = ir.getTree().getAnnotationOrNull(PrecursorIonType.class);
        if (adduct==null || !adduct.equals(ionType)) {
            FTree newTree = new IonTreeUtils().treeToNeutralTree(ir.getCandidate(), ionType, preserveOriginalTreeScore);
            FormulaScore scoredObject = ir.getScoreObject();
            FormulaScore s;
            if (!preserveOriginalTreeScore){
                if (scoredObject instanceof SiriusScore) {
                    s = new SiriusScore(FTreeMetricsHelper.getSiriusScore(newTree));
                } else if (scoredObject instanceof TreeScore) {
                    s = new TreeScore(new FTreeMetricsHelper(newTree).getTreeScore());
                } else {
                    s = scoredObject;
                }
            } else s = scoredObject;
            return new IdentificationResult(newTree, s);
        }

        else return ir;
    }

    public IdentificationResult(IdentificationResult ir) {
        this(ir.getCandidate(), ir.getScoreObject());
    }

    public IdentificationResult(FTree tree, FormulaScore score) {
        super(tree, score);
    }

    public MolecularFormula getMolecularFormula() {
        return getTree().getRoot().getFormula();
    }

    public PrecursorIonType getPrecursorIonType() {
        return getResolvedTree().getAnnotationOrThrow(PrecursorIonType.class);
    }

    public FTree getTree() {
        return getCandidate();
    }

    @Deprecated
    public FTree getRawTree() {
        return getTree();
    }

    //this is also called neutralizedTree
    @Deprecated
    public FTree getResolvedTree() {
        return getTree();
    }

    @Deprecated
    public FTree getStandardTree() {
        return getTree();
    }

    @Deprecated
    public FTree getBeautifulTree() {
        return getTree();
    }

    /**
     * true if a beautiful (bigger, better explaining spectrum) tree is available
     */
    @Deprecated
    public boolean isBeautiful() {
        return true;
    }

    public IdentificationResult clone() {
        return new IdentificationResult(new FTree(getTree()), getScoreObject());
    }

    public String toString() {
        return getMolecularFormula() + " with score " + getScore();
    }

    public FTreeMetricsHelper newMetricsHelper() {
        return new FTreeMetricsHelper(getTree());
    }
}
