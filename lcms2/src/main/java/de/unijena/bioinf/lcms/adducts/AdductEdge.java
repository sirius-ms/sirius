package de.unijena.bioinf.lcms.adducts;

import java.util.Arrays;
import java.util.Locale;

public class AdductEdge {

    protected final AdductNode left, right;
    protected final KnownMassDelta[] explanations;

    // scoring
    protected float ratioScore, correlationScore, representativeCorrelationScore, ms2score, pvalue;

    private boolean common;

    public AdductEdge(AdductNode left, AdductNode right, KnownMassDelta[] explanations) {
        this.left = left;
        this.right = right;
        this.explanations = explanations;
        this.pvalue = Float.NaN;
        this.ms2score = Float.NaN;
        if (explanations.length>1) {
            System.err.println("multiple explanations: " + Arrays.toString(explanations));
        }
        this.common = false;
        for (KnownMassDelta m : explanations) {
            if (m instanceof AdductRelationship) {
                AdductRelationship r = (AdductRelationship) m;
                if (r.left.hasNeitherAdductNorInsource() && r.right.hasNeitherAdductNorInsource()) {
                    this.common=true;
                }
            }
        }
    }

    public float getScore() {
        return -pvalue + (common ? Scorer.SCORE_BONUS_FOR_SIMPLE_EDGES : 0);
    }

    public AdductNode getOther(AdductNode u) {
        if (u==left) return right;
        else return left;
    }

    public AdductNode getLeft() {
        return left;
    }

    public AdductNode getRight() {
        return right;
    }

    public KnownMassDelta[] getExplanations() {
        return explanations;
    }

    public boolean isAdductEdge() {
        return Arrays.stream(explanations).anyMatch(x->x instanceof AdductRelationship);
    }

    @Override
    public String toString() {
        return getLeft().toString() + " -> " + getRight().toString() + String.format(Locale.US, " (σ = %.2f ) ", getScore()) + Arrays.toString(explanations);
    }
}
