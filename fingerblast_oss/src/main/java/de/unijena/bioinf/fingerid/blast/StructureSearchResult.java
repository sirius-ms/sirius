package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ms.annotations.ResultAnnotation;

public class StructureSearchResult implements ResultAnnotation {

    public double confidenceScoreExact;

    public double confidenceScoreApproximate;

    boolean isExpanded; //TODO mode instead of boolean

    int mcesIndex;

    public StructureSearchResult(double confidenceScoreExact, double confidenceScoreApproximate, int mcesIndex, boolean isExpanded) {
        this.confidenceScoreExact = confidenceScoreExact;
        this.confidenceScoreApproximate = confidenceScoreApproximate;
        this.isExpanded = isExpanded;
        this.mcesIndex=mcesIndex;
    }
}
