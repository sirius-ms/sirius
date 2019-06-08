package de.unijena.bioinf.ChemistryBase.algorithm;

public abstract class BasicScore implements Score {

    public final double score;

    public BasicScore(double score) {
        this.score = score;
    }

    @Override
    public double score() {
        return score;
    }

}
