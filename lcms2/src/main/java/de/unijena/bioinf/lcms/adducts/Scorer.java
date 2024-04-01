package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Filter;

import java.util.Optional;

public class Scorer {

    private final static double DEFAULT_SCORE = 1;

    public Scorer() {
    }

    public double computeScore(ProjectSpaceTraceProvider provider, AdductEdge edge) {
        AlignedFeatures left = edge.left.getFeature();
        AlignedFeatures right = edge.right.features;
        if (left.getTraceRef().isPresent() && right.getTraceRef().isPresent()) {
            return computeScoreFromCorrelation(provider, left, right)
        } else {
            return computeScoreWithoutCorrelation(left, right);
        }
    }

    private double computeScoreWithoutCorrelation(AlignedFeatures left, AlignedFeatures right) {
        if (left.getRetentionTime().compareTo(right.getRetentionTime())==0) {
            return DEFAULT_SCORE;
        } else return Double.NEGATIVE_INFINITY;
    }

    public double computeScoreFromCorrelation(ProjectSpaceTraceProvider provider, AlignedFeatures left, AlignedFeatures right) {
        // there are two types of correlation we can use:

        // 1.) we correlate the ratios between each feature across all samples
        correlateAcrossSamples(provider.getIntensities(left), provider.getIntensities(right));
        // 2.) we correlate the merged traces with each other
        correlateTraces(provider.getMergeTrace(left), provider.getMergeTrace(right));

    }

    private void correlateTraces(Optional<MergedTrace> mergeTrace, Optional<MergedTrace> mergeTrace1) {

    }
}
