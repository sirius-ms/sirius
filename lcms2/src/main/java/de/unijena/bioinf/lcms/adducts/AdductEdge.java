package de.unijena.bioinf.lcms.adducts;

public class AdductEdge {

    AdductNode left, right;
    KnownMassDelta[] explanations;

    double correlation;
    double score;

    public AdductEdge(AdductNode left, AdductNode right, KnownMassDelta[] explanations) {
        this.left = left;
        this.right = right;
        this.explanations = explanations;
    }
}
