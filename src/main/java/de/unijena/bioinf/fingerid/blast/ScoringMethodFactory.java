package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;

/**
 * Created by Marcus Ludwig on 28.02.17.
 */
public class ScoringMethodFactory {

    public static CSIFingerIdScoringMethod getCSIFingerIdScoringMethod(PredictionPerformance[] performances){
        return new CSIFingerIdScoringMethod(performances);
    }

    public static ProbabilityEstimateScoringMethod getProbabilityEstimateScoringMethod(PredictionPerformance[] performances){
        return new ProbabilityEstimateScoringMethod(performances);
    }

    public static SimpleMaximumLikelihoodScoringMethod getSimpleMaximumLikelihoodScoringMethod(PredictionPerformance[] performances){
        return new SimpleMaximumLikelihoodScoringMethod(performances);
    }


    public static class CSIFingerIdScoringMethod implements FingerblastScoringMethod {
        private final PredictionPerformance[] performances;

        public CSIFingerIdScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public FingerblastScoring getScoring() {
            return new CSIFingerIdScoring(performances);
        }
    }

    public static class ProbabilityEstimateScoringMethod implements FingerblastScoringMethod {
        private final PredictionPerformance[] performances;

        public ProbabilityEstimateScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public FingerblastScoring getScoring() {
            return new ProbabilityEstimateScoring(performances);
        }
    }

    public static class SimpleMaximumLikelihoodScoringMethod implements FingerblastScoringMethod {
        private final PredictionPerformance[] performances;

        public SimpleMaximumLikelihoodScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public FingerblastScoring getScoring() {
            return new SimpleMaximumLikelihoodScoring(performances);
        }
    }

}
