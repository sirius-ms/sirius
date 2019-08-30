package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.projectspace.FormulaScore;

public final class IsotopeScore extends FormulaScore {

    public IsotopeScore(double score) {
        super(score);
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Logarithmic;
    }
}
