package de.unijena.bioinf.sirius.scores;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;

public final class TreeScore extends FormulaScore {

    public TreeScore(double score) {
        super(score);
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Logarithmic;
    }

    @Override
    public String name() {
        return "Tree_Score";
    }
}
