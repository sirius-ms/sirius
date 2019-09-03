package de.unijena.bioinf.ChemistryBase.algorithm.scoring;

public class Scored<T> extends SScored<T, Score.DoubleScore> {
    public Scored(T candidate, double score) {
        super(candidate, new Score.DoubleScore(score));
    }
}
