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
package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;

//this is basically just a scored tree
//todo if the rank is used pretty offen, we can add it again
public final class IdentificationResult<S extends FormulaScore> extends SScored<FTree, S> implements Cloneable {

//    protected FTree tree;
//    protected int rank = -1;

    public static <S extends FormulaScore> IdentificationResult<S> withPrecursorIonType(IdentificationResult<S> ir, PrecursorIonType ionType) {
        return new IdentificationResult<>(new IonTreeUtils().treeToNeutralTree(ir.getCandidate(), ionType), ir.getScoreObject());
    }

    public IdentificationResult(IdentificationResult<S> ir) {
        this(ir.getCandidate(), ir.getScoreObject());
    }

    public IdentificationResult(FTree tree, S score) {
        super(tree, score);
    }

    public MolecularFormula getMolecularFormula() {
        return getTree().getRoot().getFormula();
    }

    public PrecursorIonType getPrecursorIonType() {
        return getResolvedTree().getAnnotationOrThrow(PrecursorIonType.class);
    }

    /*public int getRank() {
        return rank;
    }*/

    /*public double getScore() {
        return FTreeMetricsHelper.getSiriusScore(tree);
    }*/

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
     *
     * @return
     */
    @Deprecated
    public boolean isBeautiful() {
        return true;
    }

    /*private void copyAnnotations(FTree tree, FTree beautifulTree) {
        //todo do this for all annotations?
        UnconsideredCandidatesUpperBound upperBound = tree.getAnnotationOrNull(UnconsideredCandidatesUpperBound.class);
        if (upperBound == null) return;
        //TODO always update as beautified trees are computed each separately!?
//        if (beautifulTree.getAnnotationOrNull(UnconsideredCandidatesUpperBound.class)==null){
        beautifulTree.clearAnnotation(UnconsideredCandidatesUpperBound.class);
        beautifulTree.setAnnotation(UnconsideredCandidatesUpperBound.class, upperBound);
//        }
    }*/

    public IdentificationResult<S> clone() {
        return new IdentificationResult<>(new FTree(getTree()), getScoreObject());
    }


    public String toString() {
        return getMolecularFormula() + " with score " + getScore();
    }

    public FTreeMetricsHelper newMetricsHelper() {
        return new FTreeMetricsHelper(getTree());
    }

    /*@Override
    public int compareTo(IdentificationResult o) {
//        if (rank == o.rank)
            return Double.compare(o.getScore(), getScore());

//        else return Integer.compare(rank, o.rank);
    }*/
}
