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
        else return "beautiful (" + beautificationScoreAddition + ")";
    }

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

    /**
     * Automatically calculates the beautification bonus
     * @param tree which is beautified
     * @param increaseInTreeSize which was artificially added to the default tree size to beautify the tree
     */
    public static Beautified beautifiedFrom(FTree tree, double increaseInTreeSize) {
        return beautified(tree.numberOfEdges() * increaseInTreeSize);
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
