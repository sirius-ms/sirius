package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.RecalibrationFunction;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;

import java.io.File;
import java.io.IOException;

public class IdentificationResult {

    protected FTree tree;
    protected int rank;
    protected double score;

    public IdentificationResult(FTree tree, int rank) {
        this.tree = tree;
        this.score = tree==null ? 0d : tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public MolecularFormula getMolecularFormula() {
        return tree.getRoot().getFormula();
    }

    public RecalibrationFunction getRecalibrationFunction() {
        final RecalibrationFunction f = (RecalibrationFunction) tree.getAnnotations().get(RecalibrationFunction.class);
        if (f==null) return RecalibrationFunction.identity();
        else return f;
    }

    public double getScore() {
        return score;
    }

    public FTree getTree() {
        return tree;
    }

    public double getTreeScore() {
        final TreeScoring treeScore = tree.getAnnotationOrThrow(TreeScoring.class);
        return treeScore.getOverallScore() - treeScore.getAdditionalScore(Sirius.ISOTOPE_SCORE);
    }

    public void writeTreeToFile(File target) throws IOException {
        final String name = target.getName();
        if (name.endsWith(".dot")) {
            new FTDotWriter().writeTreeToFile(target, tree);
        } else new FTJsonWriter().writeTreeToFile(target, tree);
    }

    public void writeAnnotatedSpectrumToFile(File target) throws IOException {
        new AnnotatedSpectrumWriter().writeFile(target, tree);
    }

    public double getIsotopeScore() {
        final TreeScoring treeScore = tree.getAnnotationOrThrow(TreeScoring.class);
        return treeScore.getAdditionalScore(Sirius.ISOTOPE_SCORE);
    }
}
