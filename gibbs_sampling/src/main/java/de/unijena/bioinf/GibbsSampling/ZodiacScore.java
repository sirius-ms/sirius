package de.unijena.bioinf.GibbsSampling;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;

public class ZodiacScore extends FormulaScore {

    public ZodiacScore(double score) {
        super(score);
    }

    @Override
    public String name() {
        return "Zodiac_Score";
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Probabilistic;
    }
}
