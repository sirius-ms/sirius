package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.GibbsSampling.model.LibraryHit;
import gnu.trove.list.array.TDoubleArrayList;
import java.util.HashMap;
import java.util.Map;

public class MFCandidate implements Comparable<MFCandidate> {
    protected final FTree tree;
    protected final Ms2Experiment experiment;
    protected final Map<Class, Object> annotations;
    protected MolecularFormula formula;
    protected PrecursorIonType ionType;
    public boolean isCorrect;
    public boolean inTrainingSet;
    public boolean inEvaluationSet;
    protected final double score;
    protected LibraryHit hit;
    private final TDoubleArrayList nodeScores;

    public MFCandidate(MolecularFormula molecularFormula, double score) {
        this.formula = molecularFormula;
        this.tree = null;
        this.experiment = null;
        this.annotations = new HashMap();
        this.score = score;
        this.nodeScores = new TDoubleArrayList(4);
    }

    public MFCandidate(FTree tree) {
        this(tree, (Ms2Experiment)null);
    }

    public MFCandidate(FTree tree, Ms2Experiment experiment) {
        this.tree = tree;
        this.experiment = experiment;
        this.annotations = new HashMap();
        this.formula = tree.getRoot().getFormula();
        this.ionType = (PrecursorIonType)tree.getAnnotationOrThrow(PrecursorIonType.class);
        this.score = ((TreeScoring)tree.getAnnotationOrThrow(TreeScoring.class)).getOverallScore();
        this.nodeScores = new TDoubleArrayList(4);
    }

    public FTree getTree() {
        return this.tree;
    }

    public Ms2Experiment getExperiment() {
        return this.experiment;
    }

    public Map<Class, Object> getAnnotations() {
        return this.annotations;
    }

    public MolecularFormula getFormula() {
        return this.formula;
    }

    public PrecursorIonType getIonType() {
        return this.ionType;
    }

    public double getScore() {
        return this.score;
    }

    public LibraryHit getLibraryHit() {
        return this.hit;
    }

    public void setLibraryHit(LibraryHit hit) {
        this.hit = hit;
    }

    public boolean hasLibraryHit() {
        return this.hit != null;
    }

    protected void addNodeProbabilityScore(double score) {
        this.nodeScores.add(Math.log(score));
    }

    protected double getNodeLogProb() {
        return this.nodeScores.sum();
    }

    public int compareTo(MFCandidate o) {
        return Double.compare(o.getScore(), this.score);
    }
}
