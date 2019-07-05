package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.algorithm.Score;

public abstract class ResultScore implements Score, ResultAnnotation {

    public final double score;

    protected ResultScore(double score) {
        this.score = score;
    }

    public abstract boolean isLogarithmic();

    @Override
    public double score() {
        return score;
    }

    public abstract String name();
}
