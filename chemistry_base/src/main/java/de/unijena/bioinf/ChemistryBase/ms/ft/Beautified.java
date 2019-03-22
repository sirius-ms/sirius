package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This annotation is used when a tree is "beautiful" ;)
 * either it explains enough peaks or we already maxed out its tree size score
 *
 * The beautificationScoreAddition is the additional score a tree gains due to the beautification. It is
 * canceled in the scoring, such that the original score (without beautification) is preserved
 */
public final class Beautified implements TreeAnnotation  {


    private static Pattern BEAUTIFICATION_PATTERN = Pattern.compile("beautiful \\((\\S+)\\)");
    public static Beautified fromString(String s) {
        if (s.equals("nope")) return IS_UGGLY;
        else {
            final Matcher m = BEAUTIFICATION_PATTERN.matcher(s);
            if (!m.find()) throw new IllegalArgumentException("Malformed string: '" + s + "'");
            return new Beautified(true, Double.parseDouble(m.group(1)));
        }
    }

    public String toString() {
        if (!beautiful) return "nope";
        else return "beautiful";
    }

    public double getNodeBoost() {
        return nodeBoost;
    }


    public final static Beautified IS_UGGLY = new Beautified(false,0d);

    protected final boolean beautiful;
    protected final double nodeBoost;

    public static final String PENALTY_KEY = "BeautificationPenalty";

    private Beautified(boolean beautiful, double nodeBoost) {
        this.beautiful = beautiful;
        this.nodeBoost = nodeBoost;
    }

    public static Beautified ugly() {
        return IS_UGGLY;
    }

    public static Beautified beautified(double nodeBoost) {
        return new Beautified(true, nodeBoost);
    }

    public boolean isBeautiful() {
        return beautiful;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Beautified that = (Beautified) o;
        return beautiful == that.beautiful &&
                Double.compare(that.nodeBoost, nodeBoost) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(beautiful, nodeBoost);
    }
}
