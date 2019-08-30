package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.projectspace.FormulaScore;

public final class TreeScore extends FormulaScore {

    public TreeScore(double score) {
        super(score);
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Logarithmic;
    }
}
