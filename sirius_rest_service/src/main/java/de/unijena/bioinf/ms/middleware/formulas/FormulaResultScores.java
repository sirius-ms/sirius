package de.unijena.bioinf.ms.middleware.formulas;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.TopFingerblastScore;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.sirius.scores.SiriusScore;

public class FormulaResultScores {
    private Double siriusScore;
    private Double zodiacScore;
    private Double topFingerblastScore;
    private Double confidenceScore;

    private Boolean canopusResult = null;

    public FormulaResultScores(FormulaScoring scoring) {
        this.siriusScore = scoring.getAnnotation(SiriusScore.class).map(FormulaScore::score).orElse(null);
        this.zodiacScore = scoring.getAnnotation(ZodiacScore.class).map(FormulaScore::score).orElse(null);
        this.topFingerblastScore = scoring.getAnnotation(TopFingerblastScore.class).map(FormulaScore::score).orElse(null);
        this.confidenceScore = scoring.getAnnotation(ConfidenceScore.class).map(FormulaScore::score).orElse(null);
    }

    public Double getSiriusScore() {
        return siriusScore;
    }

    public Double getZodiacScore() {
        return zodiacScore;
    }

    public Double getTopFingerblastScore() {
        return topFingerblastScore;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public Boolean getCanopusResult() {
        return canopusResult;
    }

    public void setCanopusResult(Boolean canopusResult) {
        this.canopusResult = canopusResult;
    }
}
