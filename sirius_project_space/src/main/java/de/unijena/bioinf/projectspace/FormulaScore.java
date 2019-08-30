package de.unijena.bioinf.projectspace;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class FormulaScore implements Comparable<FormulaScore> {

    public enum ScoreType {
        Probabilistic, Logarithmic;
    }

    public final double score;

    public FormulaScore(double score) {
        this.score = score;
    }

    public abstract ScoreType getScoreType();

    @Override
    public int compareTo(@NotNull FormulaScore o) {
        return Double.compare(score,o.score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormulaScore that = (FormulaScore) o;
        return Double.compare(that.score, score) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(score)^getClass().hashCode();
    }
}
