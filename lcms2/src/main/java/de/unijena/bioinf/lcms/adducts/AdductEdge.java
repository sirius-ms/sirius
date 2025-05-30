package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractFeature;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.Arrays;
import java.util.Locale;

public class AdductEdge {

    public boolean isValid() {

        if (Double.isFinite(extraSampleCorrelation)) return true;
        // otherwise, check if both nodes have exact same set of samples
        if (left.getFeatures().getFeatures().isPresent() && right.getFeatures().getFeatures().isPresent()) {
            LongOpenHashSet longSet = new LongOpenHashSet(left.getFeatures().getFeatures().get().stream().mapToLong(AbstractFeature::getRunId).toArray());
            return longSet.equals(new LongOpenHashSet(right.getFeatures().getFeatures().get().stream().mapToLong(AbstractFeature::getRunId).toArray()));
        }
        return true;

    }

    private static enum EdgeType {ION(-1f), ADDUCT(-3f), MULTIMERE(-2), OTHER(-4f);
        private float scoreBonus;

        EdgeType(float scoreBonus) {
            this.scoreBonus = scoreBonus;
        }
    };

    protected final AdductNode left, right;
    protected final KnownMassDelta[] explanations;


    // correlations
    protected float extraSampleCorrelation, interSampleCorrelation, interSampleCorrelationRepresentative;
    protected short extraSampleCorrelationCount, interSampleCorrelationCount, interSampleCorrelationRepresentativeCount;
    // other scores
    protected float ms2score, rtScore;
    protected float pvalue;

    private EdgeType edgeType;

    public AdductEdge(AdductNode left, AdductNode right, KnownMassDelta[] explanations) {
        this.left = left;
        this.right = right;
        this.explanations = explanations;
        this.pvalue = Float.NaN;
        this.ms2score = Float.NaN;
        this.rtScore = Float.NaN;
        this.extraSampleCorrelation=Float.NaN;
        this.interSampleCorrelation = Float.NaN;
        this.interSampleCorrelationRepresentative = Float.NaN;
        if (explanations.length>1) {
            System.err.println("multiple explanations: " + Arrays.toString(explanations));
        }
        this.edgeType = EdgeType.OTHER;
        for (KnownMassDelta m : explanations) {
            if (m instanceof AdductRelationship) {
                AdductRelationship r = (AdductRelationship) m;
                if (r.left.getMultimereCount()!=r.right.getMultimereCount()) {
                    if (this.edgeType.ordinal()>EdgeType.MULTIMERE.ordinal()) this.edgeType = EdgeType.MULTIMERE;
                } else if (r.left.hasNeitherAdductNorInsource() && r.right.hasNeitherAdductNorInsource()) {
                    this.edgeType = EdgeType.ION;
                } else if (this.edgeType.ordinal()>EdgeType.ADDUCT.ordinal()) this.edgeType = EdgeType.ADDUCT;
            } else if (m instanceof LossRelationship) {
                if (((LossRelationship) m).coversPotentialAdduct()) {
                    if (this.edgeType.ordinal()>EdgeType.ADDUCT.ordinal()) this.edgeType = EdgeType.ADDUCT;
                }
            }
        }
    }

    public float getScore() {
        if (edgeType==EdgeType.OTHER) return 0f;
        else return -pvalue + edgeType.scoreBonus;
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
