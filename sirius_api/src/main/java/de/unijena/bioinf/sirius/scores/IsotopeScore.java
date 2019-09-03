package de.unijena.bioinf.sirius.scores;

public final class IsotopeScore extends FormulaScore {

    public IsotopeScore(double score) {
        super(score);
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Logarithmic;
    }
}
