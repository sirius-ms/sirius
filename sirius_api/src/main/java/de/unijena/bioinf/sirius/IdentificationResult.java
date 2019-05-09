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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeStatistics;
import de.unijena.bioinf.ChemistryBase.ms.ft.UnconsideredCandidatesUpperBound;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

public final class IdentificationResult implements Cloneable, Comparable<IdentificationResult>, Annotated<DataAnnotation> {

    protected FTree tree;
    protected MolecularFormula formula;
    protected int rank;
    protected double score;
    private final Annotated.Annotations<DataAnnotation> annotations = new Annotated.Annotations<>();

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    public IdentificationResult(IdentificationResult ir) {
        setAnnotationsFrom(ir);
        this.rank = ir.rank;
        this.tree = ir.tree;
        this.formula = ir.formula;
        this.score = ir.score;
    }

    public PrecursorIonType getPrecursorIonType() {
        return getResolvedTree().getAnnotationOrThrow(PrecursorIonType.class);
    }

    public static IdentificationResult withPrecursorIonType(IdentificationResult ir, PrecursorIonType ionType) {
        IdentificationResult r = new IdentificationResult(ir);
        r.tree = new IonTreeUtils().treeToNeutralTree(ir.tree, ionType);
        r.formula = ionType.measuredNeutralMoleculeToNeutralMolecule(ir.formula);
        return r;
    }

    public IdentificationResult(FTree tree, int rank) {
        this.tree = tree;
        this.score = tree == null ? 0d : tree.getTreeWeight();
        this.rank = rank;

        if (tree != null) {
            tree.normalizeStructure();
            this.formula = tree.getRoot().getFormula();

            final IonTreeUtils.Type type = tree.getAnnotationOrNull(IonTreeUtils.Type.class);
            if (type == IonTreeUtils.Type.RESOLVED) {
                this.formula = tree.getRoot().getFormula();
            } else if (type == IonTreeUtils.Type.IONIZED) {
                this.formula = tree.getAnnotationOrThrow(PrecursorIonType.class).precursorIonToNeutralMolecule(tree.getRoot().getFormula());
            } else {
                this.formula = tree.getAnnotationOrThrow(PrecursorIonType.class).measuredNeutralMoleculeToNeutralMolecule(tree.getRoot().getFormula());
            }
        }
    }

    public int getRank() {
        return rank;
    }

    public MolecularFormula getMolecularFormula() {
        return formula;
    }

    public double getScore() {
        return score;
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

    public FTree getTree() {
        return tree;
    }

    @Deprecated
    public FTree getRawTree() {
        return tree;
    }

    //this is also called neutralizedTree
    @Deprecated
    public FTree getResolvedTree() {
        return tree;
    }

    @Deprecated
    public FTree getStandardTree() {
        return tree;
    }

    @Deprecated
    public FTree getBeautifulTree() {
        return tree;
    }

    private void copyAnnotations(FTree tree, FTree beautifulTree) {
        //todo do this for all annotations?
        UnconsideredCandidatesUpperBound upperBound = tree.getAnnotationOrNull(UnconsideredCandidatesUpperBound.class);
        if (upperBound == null) return;
        //TODO always update as beautified trees are computed each separately!?
//        if (beautifulTree.getAnnotationOrNull(UnconsideredCandidatesUpperBound.class)==null){
        beautifulTree.clearAnnotation(UnconsideredCandidatesUpperBound.class);
        beautifulTree.setAnnotation(UnconsideredCandidatesUpperBound.class, upperBound);
//        }
    }

    public double getTreeScore() {
        return new ScoringHelper(tree).getTreeScore();
    }


    public double getIsotopeScore() {
        return new ScoringHelper(tree).getIsotopeMs1Score();
    }

    public IdentificationResult clone() {
        final IdentificationResult r = new IdentificationResult(new FTree(tree), rank);
        r.score = score;
        return r;
    }


    public String toString() {
        return formula + " with score " + getScore() + " at rank " + rank;
    }

    public double getExplainedPeaksRatio() {
        return tree.getAnnotation(TreeStatistics.class).getRatioOfExplainedPeaks();
    }

    public double getNumOfExplainedPeaks() {
        return tree.numberOfVertices();
    }

    public double getExplainedIntensityRatio() {
        return tree.getAnnotation(TreeStatistics.class).getExplainedIntensity();

    }

    public double getNumberOfExplainablePeaks() {
        return getNumOfExplainedPeaks() / getExplainedPeaksRatio();
    }

    @Override
    public int compareTo(IdentificationResult o) {
        if (rank == o.rank) return Double.compare(o.score, score);
        else return Integer.compare(rank, o.rank);
    }
}
