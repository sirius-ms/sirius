package de.unijena.bioinf.sirius.scores;

public final class TreeScore extends FormulaScore {

    public TreeScore(double score) {
        super(score);
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Logarithmic;
    }
}
