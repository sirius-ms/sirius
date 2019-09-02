package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.algorithm.Score;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;

public abstract class FTScore implements Score, TreeAnnotation {

    public final double score;

    protected FTScore(double score) {
        this.score = score;
    }

    public abstract boolean isLogarithmic();

    @Override
    public double score() {
        return score;
    }

    public abstract String name();
}
