package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.*;

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
    public static UnitScoringMethod getUnitScoring(PredictionPerformance[] performances){
        return new UnitScoringMethod(performances);
    }
    public static AccuracyScoringMethod getAccuracyScoring(PredictionPerformance[] performances){
        return new AccuracyScoringMethod(performances);
    }
    public static TanimotoScoringMethod getTanimotoScoring(PredictionPerformance[] performances){
        return new TanimotoScoringMethod(performances);
    }
    public static ProbabilisticTanimotoScoringMethod getProbabilisticTanimotoScoring(PredictionPerformance[] performances){
        return new ProbabilisticTanimotoScoringMethod(performances);
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


    // Legacy scorers

    protected abstract static class LegacyScorer implements FingerblastScoring {

        double threshold = 0.25;
        double minSamples = 25;
        PredictionPerformance[] performances;

        public LegacyScorer(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public void prepare(ProbabilityFingerprint fingerprint) {

        }

        protected boolean isValid(int index) {
            return performances[index].getSmallerClassSize() >= minSamples && performances[index].getF() >= threshold;
        }

        @Override
        public double getThreshold() {
            return threshold;
        }

        @Override
        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public double getMinSamples() {
            return minSamples;
        }

        @Override
        public void setMinSamples(double minSamples) {
            this.minSamples = minSamples;
        }
    }

    public static class UnitScoringMethod implements FingerblastScoringMethod {
        private final PredictionPerformance[] performances;
        public UnitScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public FingerblastScoring getScoring() {
            return new LegacyScorer(performances) {
                @Override
                public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
                    int index = 0;
                    double score = 0d;
                    for (FPIter2 fp : fingerprint.foreachPair(databaseEntry)) {
                        if (isValid(index)) {
                            if (fp.isLeftSet() == fp.isRightSet()) score += 1d;
                        }
                        ++index;
                    }
                    return score;
                }
            };
        }

    }
    public static class AccuracyScoringMethod implements FingerblastScoringMethod {
        private final PredictionPerformance[] performances;
        public AccuracyScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public FingerblastScoring getScoring() {
            return new LegacyScorer(performances) {
                @Override
                public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
                    int index = 0;
                    double score = 0d;
                    for (FPIter2 fp : fingerprint.foreachPair(databaseEntry)) {
                        if (isValid(index)) {
                            if (fp.isLeftSet() == fp.isRightSet()) {
                                score += Math.log(performances[index].getAccuracy());
                            } else {
                                score += Math.log(1d-performances[index].getAccuracy());
                            }
                        }
                        ++index;
                    }
                    return score;
                }
            };
        }
    }

    public static class TanimotoScoringMethod implements FingerblastScoringMethod {
        private final PredictionPerformance[] performances;
        public TanimotoScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public FingerblastScoring getScoring() {
            return new LegacyScorer(performances) {
                @Override
                public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
                    return Tanimoto.nonProbabilisticTanimoto(fingerprint,databaseEntry);
                }
            };
        }
    }
    public static class ProbabilisticTanimotoScoringMethod implements FingerblastScoringMethod {
        private final PredictionPerformance[] performances;
        public ProbabilisticTanimotoScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public FingerblastScoring getScoring() {
            return new LegacyScorer(performances) {
                @Override
                public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
                    return Tanimoto.probabilisticTanimoto(fingerprint,databaseEntry).expectationValue();
                }
            };
        }
    }

}
