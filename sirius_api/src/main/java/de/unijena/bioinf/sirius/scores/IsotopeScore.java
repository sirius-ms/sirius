package de.unijena.bioinf.sirius.scores;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;

public final class IsotopeScore extends FormulaScore {

    public IsotopeScore(double score) {
        super(score);
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Logarithmic;
    }

    @Override
    public String name() {
        return "Isotope_Score";
    }
}
