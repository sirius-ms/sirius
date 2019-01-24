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
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

public class IdentificationResult implements Cloneable, Comparable<IdentificationResult>, Annotated<DataAnnotation> {

    // TODO: we have to get rid of all these -_-
    protected FTree tree, beautifulTree, resolvedBeautifulTree;
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
        this.beautifulTree = ir.beautifulTree;
        this.formula = ir.formula;
        this.resolvedBeautifulTree = ir.resolvedBeautifulTree;
        this.score = ir.score;
    }

    public PrecursorIonType getPrecursorIonType() {
        return getResolvedTree().getAnnotationOrThrow(PrecursorIonType.class);
    }

    public static IdentificationResult withPrecursorIonType(IdentificationResult ir, PrecursorIonType ionType) {
        IdentificationResult r = new IdentificationResult(ir);
        r.resolvedBeautifulTree = new IonTreeUtils().treeToNeutralTree(ir.getBeautifulTree(), ionType);
        r.formula = ionType.measuredNeutralMoleculeToNeutralMolecule(ir.formula);
        return r;
    }

    public IdentificationResult(FTree tree, int rank) {
        this(tree, rank, false);
    }

    protected IdentificationResult(FTree tree, int rank, boolean isBeautiful) {
        this.tree = tree;
        this.score = tree == null ? 0d : tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
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
        if (isBeautiful)
            beautifulTree = tree;
    }

    @Deprecated
    public IdentificationResult transform(PrecursorIonType ionType) {
        final FTree tree = new FTree(getRawTree());
        final PrecursorIonType currentIonType = tree.getAnnotationOrThrow(PrecursorIonType.class);
        if (!currentIonType.hasNeitherAdductNorInsource() || ionType.getCharge() != currentIonType.getCharge()) {
            if (currentIonType.equals(ionType)) return this;
            else
                throw new RuntimeException("Tree is not compatible with precursor ion type " + ionType.toString() + ": " + tree.getRoot().getFormula() + " with " + currentIonType.toString());
        }
        if (!currentIonType.getIonization().equals(ionType.getIonization())) {
            final boolean invalidIonization;
            if (ionType.isIntrinsicalCharged()) {
                final PeriodicTable T = PeriodicTable.getInstance();
                if ((ionType.getCharge() > 0 && currentIonType.getIonization().equals(T.getProtonation())) || (ionType.getCharge() < 0 && currentIonType.getIonization().equals(T.getDeprotonation()))) {
                    invalidIonization = false;
                } else invalidIonization = true;

            } else invalidIonization = true;
            if (invalidIonization)
                throw new RuntimeException("Tree is not compatible with precursor ion type " + ionType.getIonization().toString() + ": " + tree.getRoot().getFormula() + " with " + currentIonType.getIonization().toString());
        }
        tree.setAnnotation(PrecursorIonType.class, ionType);
        return new IdentificationResult(new IonTreeUtils().treeToNeutralTree(tree), rank, true);
    }

    public int getRank() {
        return rank;
    }

    public MolecularFormula getMolecularFormula() {
        return formula;
    }

    public RecalibrationFunction getRecalibrationFunction() {
        final RecalibrationFunction f = (RecalibrationFunction) getRawTree().getAnnotations().get(RecalibrationFunction.class);
        if (f == null) return RecalibrationFunction.identity();
        else return f;
    }

    public double getScore() {
        return score;
    }

    /**
     * true if a beautiful (bigger, better explaining spectrum) tree is available
     *
     * @return
     */
    public boolean isBeautiful() {
        return beautifulTree != null;
    }

    public FTree getRawTree() {
        if (isBeautiful()) {
            return beautifulTree;
        } else {
            return tree;
        }
    }

    //this is also called neutralizedTree
    public FTree getResolvedTree() {
        if (resolvedBeautifulTree == null) {
            resolvedBeautifulTree = new IonTreeUtils().treeToNeutralTree(new FTree(getRawTree()));
        }
        return resolvedBeautifulTree;
    }

    public FTree getStandardTree() {
        return tree;
    }

    public FTree getBeautifulTree() {
        return beautifulTree;
    }

    public void setBeautifulTree(FTree beautifulTree) {
        this.resolvedBeautifulTree = null;
        this.beautifulTree = beautifulTree;
        TreeScoring beautifulScoring = this.beautifulTree.getAnnotationOrThrow(TreeScoring.class);
        TreeScoring treeScoring = this.tree.getAnnotationOrThrow(TreeScoring.class);
        beautifulScoring.setBeautificationPenalty(beautifulScoring.getOverallScore() - treeScoring.getOverallScore());
        beautifulScoring.setOverallScore(treeScoring.getOverallScore());

        copyAnnotations(tree, beautifulTree);
    }

    private void copyAnnotations(FTree tree, FTree beautifulTree) {
        //todo do this for all annotations?
        UnconsideredCandidatesUpperBound upperBound = tree.getAnnotationOrNull(UnconsideredCandidatesUpperBound.class);
        if (upperBound == null) return;
        //TODO always update as beautified trees are computed each separately!?
//        if (beautifulTree.getAnnotationOrNull(UnconsideredCandidatesUpperBound.class)==null){
        beautifulTree.removeAnnotation(UnconsideredCandidatesUpperBound.class);
        beautifulTree.addAnnotation(UnconsideredCandidatesUpperBound.class, upperBound);
//        }
    }

    public double getTreeScore() {
        final TreeScoring treeScore = tree.getAnnotationOrThrow(TreeScoring.class);
        return treeScore.getOverallScore() - treeScore.getIsotopeMs1Score();
    }


    public double getIsotopeScore() {
        final TreeScoring treeScore = tree.getAnnotationOrThrow(TreeScoring.class);
        return treeScore.getIsotopeMs1Score();
    }

    public IdentificationResult clone() {
        final IdentificationResult r = new IdentificationResult(new FTree(tree), rank);
        if (beautifulTree != null) r.beautifulTree = new FTree(beautifulTree);
        r.score = score;
        return r;
    }


    public String toString() {
        return formula + " with score " + getScore() + " at rank " + rank;
    }

    public double getExplainedPeaksRatio() {
        final TreeScoring treeScoring = getRawTree().getAnnotationOrNull(TreeScoring.class);
        if (treeScoring != null)
            return treeScoring.getRatioOfExplainedPeaks();
        else
            return Double.NaN;
    }

    public double getNumOfExplainedPeaks() {
        final FTree tree = getRawTree();
        if (tree != null)
            return tree.numberOfVertices();
        else
            return Double.NaN;
    }

    public double getExplainedIntensityRatio() {
        final TreeScoring treeScoring = getRawTree().getAnnotationOrNull(TreeScoring.class);
        if (treeScoring != null)
            return treeScoring.getExplainedIntensity();
        else
            return Double.NaN;

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
