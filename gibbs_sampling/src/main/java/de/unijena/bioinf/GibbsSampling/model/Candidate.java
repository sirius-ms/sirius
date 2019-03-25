package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import gnu.trove.list.array.TDoubleArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Candidate<T> implements Comparable<Candidate> {
    protected final T candidate;
    protected final Ms2Experiment experiment;
    protected final Map<Class<Object>, Object> annotations;
    protected final double score;
    private final TDoubleArrayList nodeScores;

    public Candidate(T candidate, double score) {
        this.candidate = candidate;
        this.experiment = null;
        this.annotations = new HashMap<Class<Object>, Object>();
        this.score = score;
        this.nodeScores = new TDoubleArrayList(4);
    }

    public Candidate(T t) {
        this(t, Double.NaN, (Ms2Experiment)null);
    }

    public Candidate(T candidate, double score, Ms2Experiment experiment) {
        this.candidate = candidate;
        this.experiment = experiment;
        this.annotations = new HashMap<Class<Object>, Object>();
        this.nodeScores = new TDoubleArrayList(4);
        this.score = score;
    }

    public T getCandidate() {
        return this.candidate;
    }

    public Ms2Experiment getExperiment() {
        return this.experiment;
    }

    public Map<Class<Object>, Object> getAnnotations() {
        return this.annotations;
    }



    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotation(Class<T> c) {
        final T ano = (T) annotations.get(c);
        if (ano == null) throw new NullPointerException("No annotation '" + c.getName() + "' for candidate");
        return ano;
    }


    public <T> T getAnnotationOrNull(Class<T> c) {
        final T ano = (T) annotations.get(c);
        return ano;
    }

    public <T> void addAnnotation(Class<T> klass, T annotation) {
        if (annotations.containsKey(klass))
            throw new RuntimeException("annotation '" + klass.getName() + "' is already present.");
        annotations.put((Class<Object>) klass, annotation);
    }



    public double getScore() {
        return this.score;
    }

    protected void addNodeProbabilityScore(double score) {
        this.nodeScores.add(Math.log(score));
    }

    protected void addNodeLogProbabilityScore(double score) {
        this.nodeScores.add(score);
    }

    protected void clearNodeScores(){
        nodeScores.clear();
    }

    protected double getNodeLogProb() {
        return this.nodeScores.sum();
    }

    public int compareTo(Candidate o) {
        return Double.compare(o.getScore(), this.score);
    }
}
