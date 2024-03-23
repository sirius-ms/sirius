package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;

public class ConfidenceScoreApproximate extends FormulaScore {

    public ConfidenceScoreApproximate(double score) {
        super(score);
    }

    @Override
    public ScoreType getScoreType() {
        return ScoreType.Probabilistic;
    }

    @Override
    public String name() {
        return "ConfidenceScoreApproximate";
    }

    @Override
    public String shortName() {
        return "Confidence (A)";
    }
}
