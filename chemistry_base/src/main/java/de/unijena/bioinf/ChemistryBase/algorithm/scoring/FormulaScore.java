package de.unijena.bioinf.ChemistryBase.algorithm.scoring;

import org.apache.commons.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class FormulaScore extends Score.AbstDoubleScore<FormulaScore> {
    public static final FormulaScore NaN = new FormulaScore(Double.NaN) {
        @Override
        public ScoreType getScoreType() {
            return ScoreType.NaN;
        }
    };

    public enum ScoreType {
        Probabilistic, Logarithmic, NaN;
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
