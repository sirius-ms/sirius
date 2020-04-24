package de.unijena.bioinf.ChemistryBase.algorithm.scoring;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Supplier;

public abstract class FormulaScore extends Score.AbstDoubleScore<FormulaScore> {
    /**
     * This is the Score value we use to handle Missing Scores (e.g. ranking lists)
     */
    protected static final double MISSING_SCORE_VALUE = Double.NEGATIVE_INFINITY;
    private static final Map<Class<? extends FormulaScore>, FormulaScore> MISSINGS = new HashMap<>();


    public enum ScoreType {
        Probabilistic, Logarithmic
    }

    /**
     * ALL extending scores, have to implement this constructor
     *
     * @param score the score value
     */
    public FormulaScore(double score) {
        super(validateScoreValue(score));
    }

    protected static double validateScoreValue(double toValidate) {
        if (Double.isNaN(toValidate)) {
            LoggerFactory.getLogger(FormulaScore.class).warn("Score Value is: '" + toValidate + "', which is not supported for Ranking reasons. Changing it to: " + MISSING_SCORE_VALUE);
            return MISSING_SCORE_VALUE;
        }
        return toValidate;
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

    @Override
    public final String toString() {
        return isNa() ? NA() : String.valueOf(score());
    }

    public boolean isNa() {
        return Double.compare(MISSING_SCORE_VALUE, score()) == 0;
    }

    public double scoreIfNa(Supplier<Double> alternative) {
        return isNa() ? alternative.get() : score();
    }

    public double scoreIfNa(double alternative) {
        return isNa() ? alternative : score();
    }

    public synchronized static <T extends FormulaScore> T NA(@NotNull Class<T> scoreType) {
        return NA(scoreType, MISSING_SCORE_VALUE);
    }
}
