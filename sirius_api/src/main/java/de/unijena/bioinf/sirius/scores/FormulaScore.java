package de.unijena.bioinf.sirius.scores;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;

import java.util.Objects;

public abstract class FormulaScore extends Score.AbstDoubleScore<FormulaScore> {
    public enum ScoreType {
        Probabilistic, Logarithmic;
    }

    public FormulaScore(double score) {
        super(score);
    }

    public abstract ScoreType getScoreType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormulaScore that = (FormulaScore) o;
        return Double.compare(that.score(), score()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(score()) ^ getClass().hashCode();
    }
}
