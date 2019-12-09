package de.unijena.bioinf.fingerid;


import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;

public class ConfidenceScore extends FormulaScore {
    public ConfidenceScore(double score) {
        super(score);
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Probabilistic;
    }

    @Override
    public String name() {
        return "Confidence_Score";
    }
}
