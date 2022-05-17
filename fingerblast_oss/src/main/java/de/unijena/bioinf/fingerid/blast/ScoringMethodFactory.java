/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;

/**
 * Created by Marcus Ludwig on 28.02.17.
 */
public class ScoringMethodFactory {

    public static CSIFingerIdScoringMethod getCSIFingerIdScoringMethod(PredictionPerformance[] performances) {
        return new CSIFingerIdScoringMethod(performances);
    }

    public static ProbabilityEstimateScoringMethod getProbabilityEstimateScoringMethod(PredictionPerformance[] performances) {
        return new ProbabilityEstimateScoringMethod(performances);
    }

    public static SimpleMaximumLikelihoodScoringMethod getSimpleMaximumLikelihoodScoringMethod(PredictionPerformance[] performances) {
        return new SimpleMaximumLikelihoodScoringMethod(performances);
    }

    public static UnitScoringMethod getUnitScoring(PredictionPerformance[] performances) {
        return new UnitScoringMethod(performances);
    }

    public static AccuracyScoringMethod getAccuracyScoring(PredictionPerformance[] performances) {
        return new AccuracyScoringMethod(performances);
    }

    public static TanimotoScoringMethod getTanimotoScoring(PredictionPerformance[] performances) {
        return new TanimotoScoringMethod(performances);
    }

    public static ProbabilisticTanimotoScoringMethod getProbabilisticTanimotoScoring(PredictionPerformance[] performances) {
        return new ProbabilisticTanimotoScoringMethod(performances);
    }

    public static BayesnetScoringWithDynamicComputationScoringMethod getBayesnetScoringWIthDynamicComputation() {
        return new BayesnetScoringWithDynamicComputationScoringMethod();
    }

    public static BayesnetScoringWithDynamicComputationScoringPersistentMethod getBayesnetScoringWithDynamicComputationPersistent(BayesianNetworkScoringProvider bayesianNetworkScoringProvider) {
        return new BayesnetScoringWithDynamicComputationScoringPersistentMethod(bayesianNetworkScoringProvider);
    }

    public static class CSIFingerIdScoringMethod implements FingerblastScoringMethod<CSIFingerIdScoring> {
        private final PredictionPerformance[] performances;

        public CSIFingerIdScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public CSIFingerIdScoring getScoring() {
            return new CSIFingerIdScoring(performances);
        }

        public PredictionPerformance[] getPerformances() {
            return performances;
        }
    }

    public static class ProbabilityEstimateScoringMethod implements FingerblastScoringMethod<ProbabilityEstimateScoring> {
        private final PredictionPerformance[] performances;

        public ProbabilityEstimateScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public ProbabilityEstimateScoring getScoring() {
            return new ProbabilityEstimateScoring(performances);
        }

        public PredictionPerformance[] getPerformances() {
            return performances;
        }
    }

    public static class SimpleMaximumLikelihoodScoringMethod implements FingerblastScoringMethod<SimpleMaximumLikelihoodScoring> {
        private final PredictionPerformance[] performances;

        public SimpleMaximumLikelihoodScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public SimpleMaximumLikelihoodScoring getScoring() {
            return new SimpleMaximumLikelihoodScoring(performances);
        }

        public PredictionPerformance[] getPerformances() {
            return performances;
        }
    }

    public static class BayesnetScoringWithDynamicComputationScoringMethod implements FingerblastScoringMethod<BayesnetScoringWithDynamicComputation> {
        @Override
        public BayesnetScoringWithDynamicComputation getScoring() {
            return new BayesnetScoringWithDynamicComputation();
        }
    }


    public static class BayesnetScoringWithDynamicComputationScoringPersistentMethod implements FingerblastScoringMethod<BayesnetScoringWithDynamicComputationPersistent> {
        private final BayesianNetworkScoringProvider bayesianNetworkScoringProvider;

        public BayesnetScoringWithDynamicComputationScoringPersistentMethod(BayesianNetworkScoringProvider bayesianNetworkScoringProvider) {
            this.bayesianNetworkScoringProvider = bayesianNetworkScoringProvider;
        }
        @Override
        public BayesnetScoringWithDynamicComputationPersistent getScoring() {
            return new BayesnetScoringWithDynamicComputationPersistent(bayesianNetworkScoringProvider);
        }
    }

    // Legacy scorers

    protected abstract static class LegacyScorer implements FingerblastScoring<Object> {

        double threshold = 0.25;
        double minSamples = 25;
        PredictionPerformance[] performances;

        public LegacyScorer(PredictionPerformance[] performances) {
            this.performances = performances;
        }


        protected boolean isValid(int index) {
            return performances[index].getSmallerClassSize() >= minSamples && performances[index].getF() >= threshold;
        }

        @Override
        public Object extractParameters(ParameterStore store) {
            return null;
        }

        @Override
        public void prepare(Object genericInputParameter) {

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

        public PredictionPerformance[] getPerformances() {
            return performances;
        }
    }

    public static class UnitScoringMethod implements FingerblastScoringMethod<LegacyScorer> {
        private final PredictionPerformance[] performances;

        public UnitScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public LegacyScorer getScoring() {
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

        public PredictionPerformance[] getPerformances() {
            return performances;
        }
    }

    public static class AccuracyScoringMethod implements FingerblastScoringMethod<LegacyScorer> {
        private final PredictionPerformance[] performances;

        public AccuracyScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public LegacyScorer getScoring() {
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
                                score += Math.log(1d - performances[index].getAccuracy());
                            }
                        }
                        ++index;
                    }
                    return score;
                }
            };
        }

        public PredictionPerformance[] getPerformances() {
            return performances;
        }
    }

    public static class TanimotoScoringMethod implements FingerblastScoringMethod<LegacyScorer> {
        private final PredictionPerformance[] performances;

        public TanimotoScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public LegacyScorer getScoring() {
            return new LegacyScorer(performances) {
                @Override
                public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
                    return Tanimoto.nonProbabilisticTanimoto(fingerprint, databaseEntry);
                }
            };
        }

        public PredictionPerformance[] getPerformances() {
            return performances;
        }
    }

    public static class ProbabilisticTanimotoScoringMethod implements FingerblastScoringMethod<LegacyScorer> {
        private final PredictionPerformance[] performances;

        public ProbabilisticTanimotoScoringMethod(PredictionPerformance[] performances) {
            this.performances = performances;
        }

        @Override
        public LegacyScorer getScoring() {
            return new LegacyScorer(performances) {
                @Override
                public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
                    return Tanimoto.fastTanimoto(fingerprint, databaseEntry);//Tanimoto.probabilisticTanimoto(fingerprint, databaseEntry).expectationValue();
                }
            };
        }

        public PredictionPerformance[] getPerformances() {
            return performances;
        }
    }

}
