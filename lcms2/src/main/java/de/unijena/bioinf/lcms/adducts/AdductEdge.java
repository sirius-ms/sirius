package de.unijena.bioinf.lcms.adducts;

public class AdductEdge {

    protected final AdductNode left, right;
    protected final KnownMassDelta[] explanations;

    // scoring
    protected float ratioScore, correlationScore, representativeCorrelationScore, score;

    public AdductEdge(AdductNode left, AdductNode right, KnownMassDelta[] explanations) {
        this.left = left;
        this.right = right;
        this.explanations = explanations;
    }
}
