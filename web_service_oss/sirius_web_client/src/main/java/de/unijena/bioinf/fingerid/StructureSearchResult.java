package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

public class StructureSearchResult implements ResultAnnotation {

    public double confidenceScoreExact;

    public double confidenceScoreApproximate;

    ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode;

    int mcesIndex;

    public StructureSearchResult(double confidenceScoreExact, double confidenceScoreApproximate, int mcesIndex, ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode) {
        this.confidenceScoreExact = confidenceScoreExact;
        this.confidenceScoreApproximate = confidenceScoreApproximate;
        this.expansiveSearchConfidenceMode=expansiveSearchConfidenceMode;
        this.mcesIndex=mcesIndex;
    }
}
