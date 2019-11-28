package de.unijena.bioinf.sirius.scores;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;

public class SiriusScore extends FormulaScore {

    public SiriusScore(double score) {
        super(score);
    }

    @Override
    public String name() {
        return "TreeIsotope_Score";
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Logarithmic;
    }
}