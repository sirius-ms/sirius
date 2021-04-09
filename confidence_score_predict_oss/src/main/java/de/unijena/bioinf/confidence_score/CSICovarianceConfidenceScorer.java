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

package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.svm.SVMPredict;
import de.unijena.bioinf.confidence_score.svm.SVMUtils;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.*;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by martin on 20.06.18.
 */
public class CSICovarianceConfidenceScorer<S extends FingerblastScoring<?>> implements ConfidenceScorer {
    public static final String NO_DISTANCE_ID = "Nodist";
    public static final String DISTANCE_ID = "dist";

    public static final String DB_ALL_ID = "All";
    public static final String DB_BIO_ID = "Bio";

    public static final String CE_LOW = "feLow";
    public static final String CE_MED = "feMed";
    public static final String CE_HIGH = "feHi";
    public static final String CE_RAMP = "feRAMP";


    private final Map<String, TrainedSVM> trainedSVMs;
    private final FingerblastScoringMethod<S> covarianceScoringMethod;
    private final ScoringMethodFactory.CSIFingerIdScoringMethod csiFingerIdScoringMethod;
    private Class<? extends FingerblastScoringMethod<?>> scoringOfInput;


    public CSICovarianceConfidenceScorer(@NotNull Map<String, TrainedSVM> trainedsvms, @NotNull FingerblastScoringMethod<S> covarianceScoringMethod, @NotNull ScoringMethodFactory.CSIFingerIdScoringMethod csiFingerIDScoringMethod, Class<? extends FingerblastScoringMethod<?>> scoringOfInput) {
        this.trainedSVMs = trainedsvms;
        this.covarianceScoringMethod = covarianceScoringMethod;
        this.csiFingerIdScoringMethod = csiFingerIDScoringMethod;
        this.scoringOfInput = scoringOfInput;
    }

    public Class<? extends FingerblastScoringMethod<?>> getScoringOfInput() {
        return scoringOfInput;
    }

    public void setScoringOfInput(Class<? extends FingerblastScoringMethod<?>> scoringOfInput) {
        this.scoringOfInput = scoringOfInput;
    }

    public double computeConfidence(final Ms2Experiment exp, List<Scored<FingerprintCandidate>> pubchemCandidates, long dbFilterFlag, @NotNull ParameterStore parametersWithQuery) {
        return computeConfidence(exp, pubchemCandidates, parametersWithQuery, it -> (it.getBitset() & dbFilterFlag) != 0);
    }

    @Override
    public double computeConfidence(@NotNull final Ms2Experiment exp, @NotNull List<Scored<FingerprintCandidate>> allDbCandidates, @NotNull ParameterStore parametersWithQuery, @Nullable final Predicate<FingerprintCandidate> filter) {
        return computeConfidence(exp, allDbCandidates, scoringOfInput, parametersWithQuery, filter);

    }

    @Override
    public double computeConfidence(@NotNull final Ms2Experiment exp, @NotNull List<Scored<FingerprintCandidate>> allDbCandidates, @NotNull List<Scored<FingerprintCandidate>> searchDBCandidates, @NotNull ParameterStore parametersWithQuery) {
        return computeConfidence(exp, allDbCandidates, searchDBCandidates, scoringOfInput, parametersWithQuery);
    }

    public double computeConfidence(@NotNull final Ms2Experiment exp,
                                    @NotNull List<Scored<FingerprintCandidate>> pubchemCandidates, @NotNull List<Scored<FingerprintCandidate>> searchDBCandidates,
                                    @NotNull Class<? extends FingerblastScoringMethod<?>> scoringMethod, @NotNull ParameterStore parametersWithQuery) {
        //re-scoring the candidates?
        final List<Scored<FingerprintCandidate>> rankedPubchemCandidatesCSIscore;
        final List<Scored<FingerprintCandidate>> rankedSearchDBCandidatesCSIscore;
        if (scoringMethod.equals(ScoringMethodFactory.CSIFingerIdScoringMethod.class)) {
            rankedPubchemCandidatesCSIscore = pubchemCandidates;
            rankedSearchDBCandidatesCSIscore = searchDBCandidates;
        } else {
            final CSIFingerIdScoring csiFingerIdScoring = csiFingerIdScoringMethod.getScoring();
            rankedPubchemCandidatesCSIscore = calculateCSIScores(pubchemCandidates, csiFingerIdScoring, parametersWithQuery);
            rankedSearchDBCandidatesCSIscore = calculateCSIScores(searchDBCandidates, csiFingerIdScoring, parametersWithQuery);
        }

        final List<Scored<FingerprintCandidate>> rankedPubchemCandidatesCovscore;
        final List<Scored<FingerprintCandidate>> rankedSearchDBCandidatesCovscore;
        final S covarianceScoring = covarianceScoringMethod.getScoring();
        covarianceScoring.prepare(parametersWithQuery);
        if (scoringMethod.equals(covarianceScoringMethod.getClass())) {
            rankedPubchemCandidatesCovscore = pubchemCandidates;
            rankedSearchDBCandidatesCovscore = searchDBCandidates;
        } else { //no scoring given that is useful for the confidence computation, recalculate all.
            rankedPubchemCandidatesCovscore = calculateCovarianceScores(pubchemCandidates, covarianceScoring, parametersWithQuery);
            rankedSearchDBCandidatesCovscore = calculateCovarianceScores(searchDBCandidates, covarianceScoring, parametersWithQuery);
        }

        return computeConfidence(exp,
                rankedPubchemCandidatesCovscore.toArray(Scored[]::new),
                rankedPubchemCandidatesCSIscore.toArray(Scored[]::new),
                rankedSearchDBCandidatesCovscore.toArray(Scored[]::new),
                rankedSearchDBCandidatesCSIscore.toArray(Scored[]::new),
                parametersWithQuery, covarianceScoring, csiFingerIdScoringMethod.getPerformances());

    }

    public double computeConfidence(final Ms2Experiment exp,List<Scored<FingerprintCandidate>> pubchemCandidates,
                                    Class<? extends FingerblastScoringMethod<?>> scoringMethod,
                                    ParameterStore parametersWithQuery, @Nullable final Predicate<FingerprintCandidate> filter) {
        //re-scoring the candidates?
        final List<Scored<FingerprintCandidate>> rankedCandidatesCSIscore;
        if (scoringMethod == ScoringMethodFactory.CSIFingerIdScoringMethod.class) { //set as csi covariance scoring
            rankedCandidatesCSIscore = pubchemCandidates;
        } else {
            final CSIFingerIdScoring csiFingerIdScoring = csiFingerIdScoringMethod.getScoring();
            rankedCandidatesCSIscore = calculateCSIScores(pubchemCandidates, csiFingerIdScoring, parametersWithQuery);
        }

        final List<Scored<FingerprintCandidate>> rankedCandidatesCovscore;
        final S covarianceScoring = covarianceScoringMethod.getScoring();
        covarianceScoring.prepare(parametersWithQuery);
        if (scoringMethod.equals(covarianceScoringMethod.getClass())) { // set as covariance scoring
            rankedCandidatesCovscore = pubchemCandidates;
        } else { //no scoring given that is useful for the confidence computation, recalculate all.
            rankedCandidatesCovscore = calculateCovarianceScores(pubchemCandidates, covarianceScoring, parametersWithQuery);
        }

        return computeConfidence(exp,
                rankedCandidatesCovscore.toArray(Scored[]::new),
                rankedCandidatesCSIscore.toArray(Scored[]::new),
                filter != null ? rankedCandidatesCSIscore.stream().filter(s -> filter.test(s.getCandidate())).toArray(Scored[]::new) : null,
                filter != null ? rankedCandidatesCovscore.stream().filter(s -> filter.test(s.getCandidate())).toArray(Scored[]::new) : null,
                parametersWithQuery, covarianceScoring, csiFingerIdScoringMethod.getPerformances());
    }


    public double computeConfidence(final Ms2Experiment exp, Scored<FingerprintCandidate>[] rankedPubchemCandidatesCov, Scored<FingerprintCandidate>[] rankedPubchemCandidatesCSI,
                                    @Nullable Scored<FingerprintCandidate>[] rankedSearchDBCandidatesCov, @Nullable Scored<FingerprintCandidate>[] rankedSearchDBCandidatesCSI,
                                    ParameterStore parametersWithQuery, S covarianceScoring, PredictionPerformance[] csiPerformances) {

        if (rankedPubchemCandidatesCov.length != rankedPubchemCandidatesCSI.length)
            throw new IllegalArgumentException("Covariance scored candidate list has different length from fingerid scored candidates list!");

        if (rankedPubchemCandidatesCov.length <= 4) {
            LoggerFactory.getLogger(getClass()).debug("Cannot calculate confidence with less than 5 hits in \"PubChem\" database! Returning NaN. Instance: " + exp.getName() + "-" + exp.getMolecularFormula() + "-" + exp.getPrecursorIonType());
            return Double.NaN;
        } else if (rankedSearchDBCandidatesCov != null && rankedSearchDBCandidatesCov.length == 0) {
            LoggerFactory.getLogger(getClass()).debug("Cannot calculate confidence with NO hit in \"Search\" database! Returning NaN. Instance: " + exp.getName() + "-" + exp.getMolecularFormula() + "-" + exp.getPrecursorIonType());
            return Double.NaN;
        }

        final String ce = makeCeString(exp.getMs2Spectra());
        final CombinedFeatureCreator comb;
        final String distanceType;
        final String dbType;


        if (rankedSearchDBCandidatesCov == null || rankedSearchDBCandidatesCSI == null) { //calculate score for pubChem lists
            comb = new CombinedFeatureCreatorALL(rankedPubchemCandidatesCSI, rankedPubchemCandidatesCov, csiPerformances, covarianceScoring);
            distanceType = null;
            dbType = DB_ALL_ID;
        } else if (moreThanOneUniqueFPs(rankedSearchDBCandidatesCov)) { //calculate score for filtered lists
            comb = new CombinedFeatureCreatorBIODISTANCE(rankedPubchemCandidatesCSI, rankedPubchemCandidatesCov, rankedSearchDBCandidatesCSI, rankedSearchDBCandidatesCov, csiPerformances, covarianceScoring);
            distanceType = DISTANCE_ID;
            dbType = DB_BIO_ID;
        } else {
            comb = new CombinedFeatureCreatorBIONODISTANCE(rankedPubchemCandidatesCSI, rankedPubchemCandidatesCov, rankedSearchDBCandidatesCSI, rankedSearchDBCandidatesCov, csiPerformances, covarianceScoring);
            distanceType = NO_DISTANCE_ID;
            dbType = DB_BIO_ID;
        }
        final double[] features = comb.computeFeatures(parametersWithQuery);
        return calculateConfidence(features, dbType, distanceType, ce);
    }

    private boolean moreThanOneUniqueFPs(Scored<FingerprintCandidate>[] candidates) {
        if (candidates.length < 2)
            return false;
        short[] first = candidates[0].getCandidate().getFingerprint().toIndizesArray();
        for (int i = 1; i < candidates.length; i++)
            if (!Arrays.equals(first, candidates[i].getCandidate().getFingerprint().toIndizesArray()))
                return true;
        return false;
    }


    private double calculateConfidence(@NotNull double[] feature, @NotNull String dbType, @Nullable String distanceType, @NotNull String collisionEnergy) {
        final String id = distanceType != null ? collisionEnergy + "_" + dbType + distanceType + ".svm" : collisionEnergy + "_" + dbType + ".svm";
        final TrainedSVM svm = trainedSVMs.get(id);
        if (svm == null)
            throw new IllegalArgumentException("Could not found confidence svm with ID: \"" + id + "\"");
        final double[][] featureMatrix = new double[1][feature.length];
        featureMatrix[0] = feature;
        SVMUtils.standardize_features(featureMatrix, svm.scales);
        return new SVMPredict().predict_confidence(featureMatrix, svm)[0];
    }


    private static List<Scored<FingerprintCandidate>> calculateCSIScores(List<Scored<FingerprintCandidate>> candidates, CSIFingerIdScoring csiFingerIdScoring, ParameterStore parametersWithQuery) {
        ProbabilityFingerprint query = parametersWithQuery.getFP().orElseThrow();
        csiFingerIdScoring.prepare(query);
        return candidates.stream().map(SScored::getCandidate).map(c -> new Scored<>(c, csiFingerIdScoring.score(query, c.getFingerprint())))
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    private static List<Scored<FingerprintCandidate>> calculateCovarianceScores(List<Scored<FingerprintCandidate>> candidates, FingerblastScoring<?> covarianceScoring, ParameterStore parametersWithQuery) {
        ProbabilityFingerprint query = parametersWithQuery.getFP().orElseThrow();
        return candidates.stream().map(SScored::getCandidate).map(c -> new Scored<>(c, covarianceScoring.score(query, c.getFingerprint())))
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    public static String makeCeString(@NotNull final List<Ms2Spectrum<Peak>> spectra) {
        //find collision energy in spectrum
        double ceMin = Double.MAX_VALUE;
        double ceMax = Double.MIN_VALUE;
        for (Ms2Spectrum<?> spec : spectra) {
            CollisionEnergy ce = spec.getCollisionEnergy();
            if (ce == null) return CE_RAMP;
            ceMax = Math.max(ceMax, ce.getMaxEnergy());
            ceMin = Math.min(ceMin, ce.getMinEnergy());
            if (ceMin != ceMax) return CE_RAMP;
        }

        if (ceMin <= 15) //10
            return CE_LOW;
        else if (ceMin < 30) //20
            return CE_MED;
        else
            return CE_HIGH; //40
    }
}
