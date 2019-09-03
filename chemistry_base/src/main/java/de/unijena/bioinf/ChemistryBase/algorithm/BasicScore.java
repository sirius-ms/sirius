package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;

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
