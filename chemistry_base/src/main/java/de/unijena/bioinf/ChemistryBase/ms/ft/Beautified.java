package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

import java.util.Objects;

/**
 * This annotation is used when a tree is "beautiful" ;)
 * either it explains enough peaks or we already maxed out its tree size score
 *
 * The beautificationScoreAddition is the additional score a tree gains due to the beautification. It is
 * canceled in the scoring, such that the original score (without beautification) is preserved
 */
public final class Beautified implements TreeAnnotation  {

    public final static Beautified IS_UGGLY = new Beautified(false,0d);

    protected final boolean beautiful;
    protected final double beautificationScoreAddition;

    public static final String PENALTY_KEY = "BeautificationPenalty";

    private Beautified(boolean beautiful, double beautificationBonus) {
        this.beautiful = beautiful;
        this.beautificationScoreAddition = beautificationBonus;
    }

    public static Beautified ugly() {
        return IS_UGGLY;
    }

    public static Beautified beautified(double score) {
        return new Beautified(true, score);
    }

    public boolean isBeautiful() {
        return beautiful;
    }

    public double getBeautificationScoreAddition() {
        return beautificationScoreAddition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Beautified that = (Beautified) o;
        return beautiful == that.beautiful &&
                Double.compare(that.beautificationScoreAddition, beautificationScoreAddition) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(beautiful, beautificationScoreAddition);
    }
}
