package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Note: this class has a natural ordering that is inconsistent with equals
 */
public class ScoredMolecularFormula implements Comparable<ScoredMolecularFormula> {

    private final double score;
    private final MolecularFormula formula;

    public static List<ScoredMolecularFormula> merge(List<ScoredMolecularFormula> as, List<ScoredMolecularFormula> bs, double weightLeft, boolean normalize) {
        if (weightLeft >= 1 || weightLeft <= 0) throw new IllegalArgumentException("weight have to be a float value in (0,1)");
        if (as.isEmpty()) return bs;
        if (bs.isEmpty()) return as;
        double weightRight = 1-weightLeft;
        if(normalize) {
            weightLeft /= Collections.max(as).getScore();
            weightRight /= Collections.max(bs).getScore();
        }
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>(Math.max(as.size(), bs.size()) + 20);
        for (ScoredMolecularFormula a : as) map.put(a.formula, a.score*weightLeft);
        for (ScoredMolecularFormula b : bs) {
            final Double score = map.get(b.formula);
            if (score == null)
                map.put(b.formula, b.score*weightRight);
            else
                map.put(b.formula, b.score*weightRight + score);
        }
        final ArrayList<ScoredMolecularFormula> formulas = new ArrayList<ScoredMolecularFormula>(map.size());
        for (MolecularFormula a : map.keySet()) formulas.add(new ScoredMolecularFormula(a, map.get(a)));
        Collections.sort(formulas, Collections.reverseOrder());
        return formulas;
    }

    public ScoredMolecularFormula(MolecularFormula formula, double score) {
        if (formula == null) throw new NullPointerException("Expect non-null molecular formula");
        this.score = score;
        this.formula = formula;
    }

    public ScoredMolecularFormula(MolecularFormula formula, MolecularFormulaScorer scorer) {
        this(formula, scorer.score(formula));
    }

    public double getScore() {
        return score;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoredMolecularFormula that = (ScoredMolecularFormula) o;

        if (Double.compare(that.score, score) != 0) return false;
        if (!formula.equals(that.formula)) return false;

        return true;
    }

    @Override
    public String toString() {
        return String.format("%s (%.3f)", formula.toString(), score);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = score != +0.0d ? Double.doubleToLongBits(score) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + formula.hashCode();
        return result;
    }

    @Override
    public int compareTo(ScoredMolecularFormula o) {
        return new Double(score).compareTo(o.score);
    }
}
