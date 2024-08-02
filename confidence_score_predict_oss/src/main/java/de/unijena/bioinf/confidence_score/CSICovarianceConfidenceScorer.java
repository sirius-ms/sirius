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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.svm.SVMPredict;
import de.unijena.bioinf.confidence_score.svm.SVMUtils;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.CombinatorialSubtree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by martin on 20.06.18.
 */
public class CSICovarianceConfidenceScorer<S extends FingerblastScoring<?>> implements ConfidenceScorer {
    public static final String NO_DISTANCE_ID = "Nodist";
    public static final String DISTANCE_2_5_ID = "dist2";
    public static final String DISTANCE_6_10_ID = "dist6";
    public static final String DISTANCE_ID = "dist";

    public static final String DB_ALL_ID = "All";
    public static final String DB_BIO_ID = "Bio";

    private final Map<String, TrainedSVM> trainedSVMs;
    private final FingerblastScoringMethod<S> covarianceScoringMethod;


    public CSICovarianceConfidenceScorer(@NotNull Map<String, TrainedSVM> trainedsvms,
                                         @NotNull FingerblastScoringMethod<S> covarianceScoringMethod
                                      ) {
        this.trainedSVMs = trainedsvms;
        this.covarianceScoringMethod = covarianceScoringMethod;

    }

    public double computeConfidence(final Ms2Experiment exp, List<Scored<FingerprintCandidate>> allDbCandidates , long dbFilterFlag, @NotNull ParameterStore parametersWithQuery, boolean structureSearchDBIsPubChem, @NotNull FTree[] ftrees,
                                    @NotNull CombinatorialSubtree[] combSubtrees,
                                    Map<Fragment, ArrayList<CombinatorialFragment>>[] mappings,@NotNull CanopusResult canopusResult,
                                    @NotNull CanopusResult canopusResultTopHit) {
        return computeConfidence(exp, allDbCandidates, parametersWithQuery, it -> (it.getBitset() & dbFilterFlag) != 0,structureSearchDBIsPubChem, ftrees,combSubtrees,mappings,canopusResult,canopusResultTopHit);
    }


    @Override
    public double computeConfidence(@NotNull Ms2Experiment exp, @NotNull List<Scored<FingerprintCandidate>> allDbCandidates, @NotNull ParameterStore parametersWithQuery, @Nullable Predicate<FingerprintCandidate> filter, @NotNull boolean structureSearchDBIsPubChem, @NotNull FTree[] ftrees,
                                    @NotNull CombinatorialSubtree[] combSubtrees,
                                    Map<Fragment, ArrayList<CombinatorialFragment>>[] mappings,@NotNull CanopusResult canopusResult,
                                    @NotNull CanopusResult canopusResultTopHit) {
        return computeConfidence(exp, allDbCandidates, parametersWithQuery, filter, structureSearchDBIsPubChem,ftrees,combSubtrees,mappings,canopusResult,canopusResultTopHit);

    }

    @Override
    public double computeConfidence(@NotNull Ms2Experiment exp, @NotNull List<Scored<FingerprintCandidate>> allDbCandidates, @NotNull List<Scored<FingerprintCandidate>> searchDBCandidates, @NotNull ParameterStore parametersWithQuery,@NotNull boolean structureSearchDBIsPubChem,@NotNull FTree[] ftrees,
                                    @NotNull CombinatorialSubtree[] combSubtrees,
                                    Map<Fragment, ArrayList<CombinatorialFragment>>[] mappings,@NotNull CanopusResult canopusResult,
                                    @NotNull CanopusResult canopusResultTopHit) {
        return computeConfidence(exp,
                allDbCandidates.toArray(new Scored[]{}),
                searchDBCandidates.toArray(new Scored[]{}),
                parametersWithQuery, covarianceScoringMethod.getScoring(),structureSearchDBIsPubChem,ftrees,combSubtrees,mappings,canopusResult,canopusResultTopHit);
    }

    public double computeConfidence(final Ms2Experiment exp,
                                    Scored<FingerprintCandidate>[] rankedPubchemCandidates,
                                    @Nullable Scored<FingerprintCandidate>[] rankedSearchDBCandidates,
                                    ParameterStore parametersWithQuery, S covarianceScoring, boolean structureSearchDBIsPubChem,@NotNull FTree[] ftrees,
                                    @NotNull CombinatorialSubtree[] combSubtrees,
                                    Map<Fragment, ArrayList<CombinatorialFragment>>[] mappings,@NotNull CanopusResult canopusResult,
                                    @NotNull CanopusResult canopusResultTopHit) {

        if (rankedPubchemCandidates.length <= 4) {
            LoggerFactory.getLogger(getClass()).debug("Cannot calculate confidence with less than 5 hits in \"PubChem\" database! Returning NaN. Instance: " + exp.getName() + "-" + exp.getMolecularFormula() + "-" + exp.getPrecursorIonType());
            return Double.NaN;
        } else if (rankedSearchDBCandidates != null && rankedSearchDBCandidates.length == 0) {
            LoggerFactory.getLogger(getClass()).debug("Cannot calculate confidence with NO hit in \"Search\" database! Returning NaN. Instance: " + exp.getName() + "-" + exp.getMolecularFormula() + "-" + exp.getPrecursorIonType());
            return Double.NaN;
        }

        final CombinedFeatureCreator comb;
        final String distanceType;
        final String dbType;



        ProbabilityFingerprint canopusFptPred = canopusResult.getCanopusFingerprint();
        ProbabilityFingerprint canopusFptTop = canopusResultTopHit.getCanopusFingerprint();
        CombinatorialSubtree[] epiTrees=combSubtrees;
        FTree[] fTrees = ftrees;
        Map<Fragment, ArrayList<CombinatorialFragment>>[] originalMappings =mappings;

        if (structureSearchDBIsPubChem) {

            if (rankedSearchDBCandidates.length>10) { //calculate score for pubChem lists
                comb = new CombinedFeatureCreatorALL( rankedPubchemCandidates, covarianceScoring, canopusFptPred, canopusFptTop, epiTrees, originalMappings, fTrees);
                distanceType = DISTANCE_ID;
                dbType = DB_ALL_ID;
            } else{
                comb = new CombinedFeatureCreatorALL6TO10(rankedPubchemCandidates, covarianceScoring, canopusFptPred, canopusFptTop, epiTrees, originalMappings, fTrees);
                distanceType = DISTANCE_6_10_ID;
                dbType = DB_ALL_ID;
            }
        }else {

            if (rankedSearchDBCandidates.length > 10) { //calculate score for filtered lists
                comb = new CombinedFeatureCreatorBIODISTANCE(rankedPubchemCandidates, rankedSearchDBCandidates, covarianceScoring, canopusFptPred, canopusFptTop, epiTrees, originalMappings, fTrees);
                distanceType = DISTANCE_ID;
                dbType = DB_BIO_ID;
            } else if (rankedSearchDBCandidates.length > 1 && rankedSearchDBCandidates.length < 6) {
                comb = new CombinedFeatureCreatorBIODISTANCE2TO5( rankedPubchemCandidates,  rankedSearchDBCandidates,  covarianceScoring, canopusFptPred, canopusFptTop, epiTrees, originalMappings, fTrees);
                distanceType = DISTANCE_2_5_ID;
                dbType = DB_BIO_ID;
            } else if (rankedSearchDBCandidates.length > 5 && rankedSearchDBCandidates.length < 11) {
                comb = new CombinedFeatureCreatorBIODISTANCE6TO10( rankedPubchemCandidates, rankedSearchDBCandidates,  covarianceScoring, canopusFptPred, canopusFptTop, epiTrees, originalMappings, fTrees);
                distanceType = DISTANCE_6_10_ID;
                dbType = DB_BIO_ID;
            } else {
                comb = new CombinedFeatureCreatorBIONODISTANCE( rankedPubchemCandidates,  rankedSearchDBCandidates,  covarianceScoring, canopusFptPred, canopusFptTop, epiTrees, originalMappings);
                distanceType = NO_DISTANCE_ID;
                dbType = DB_BIO_ID;
            }
        }
        final double[] features = comb.computeFeatures(parametersWithQuery);
        return calculateConfidence(features, dbType, distanceType);
    }


    private double calculateConfidence(double[] feature, @NotNull String dbType, @Nullable String distanceType) {
       /* Random r = new Random(); //TODO: BETTER DONT FORGET TO TAKE THIS OUT
        return r.nextDouble(1);*/

        final String id = distanceType != null ? dbType + distanceType + ".svm" : dbType + ".svm";
        final TrainedSVM svm = trainedSVMs.get(id);
        if (svm == null)
            throw new IllegalArgumentException("Could not find confidence svm with ID: \"" + id + "\"");
        final double[][] featureMatrix = new double[1][feature.length];
        featureMatrix[0] = feature;
        SVMUtils.standardize_features(featureMatrix, svm.scales);
        return new SVMPredict().predict_confidence(featureMatrix, svm)[0];
    }

}
