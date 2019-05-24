package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.svm.SVMPredict;
import de.unijena.bioinf.confidence_score.svm.SVMUtils;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.CovarianceScoring;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by martin on 20.06.18.
 */
public class CSICovarianceConfidenceScorer implements ConfidenceScorer {

    private final Map<String, TrainedSVM> trainedSVMs;
    private final CovarianceScoring covarianceScoring;
    private final CSIFingerIdScoring csiFingerIdScoring;


    public CSICovarianceConfidenceScorer(@NotNull Map<String, TrainedSVM> trainedsvms, @NotNull CovarianceScoring covarianceScoring, @NotNull CSIFingerIdScoring csiFingerIDScoring) {
        this.trainedSVMs = trainedsvms;
        this.covarianceScoring = covarianceScoring;
        this.csiFingerIdScoring = csiFingerIDScoring;
    }

    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, Scored<FingerprintCandidate>[] filteredCandidates, Class<? extends FingerblastScoring> scoringMethod, ProbabilityFingerprint query) {
        return computeConfidence(exp, idResult, allCandidates, Arrays.stream(filteredCandidates).map(Scored::getCandidate).collect(Collectors.toSet()), scoringMethod, query);
    }


    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, long dbFilterFlag, Class<? extends FingerblastScoring> scoringMethod, ProbabilityFingerprint query) {
        return computeConfidence(exp, idResult, allCandidates, scoringMethod, query, it -> (it.getBitset() & dbFilterFlag) != 0);
    }

    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, Set<FingerprintCandidate> candidateFilter, Class<? extends FingerblastScoring> scoringMethod, ProbabilityFingerprint query) {
        return computeConfidence(exp, idResult, allCandidates, scoringMethod, query, candidateFilter::contains);
    }


    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, Class<? extends FingerblastScoring> scoringMethod, ProbabilityFingerprint query, @NotNull final Predicate<FingerprintCandidate> filter) {
        //re-scoring the candidates?
        final Scored<FingerprintCandidate>[] rankedCandidatesCSIscore;
        final Scored<FingerprintCandidate>[] rankedCandidatesCSIscoreFiltered;

        if (scoringMethod == CSIFingerIdScoring.class) { //set as csi covariance scoring
            rankedCandidatesCSIscore = allCandidates.clone();
        } else {
            csiFingerIdScoring.prepare(query);
            rankedCandidatesCSIscore = new Scored[allCandidates.length];
            for (int i = 0; i < allCandidates.length; i++)
                rankedCandidatesCSIscore[i] = new Scored<>(allCandidates[i].getCandidate(), csiFingerIdScoring.score(query, allCandidates[i].getCandidate().getFingerprint()));
        }
        Arrays.sort(rankedCandidatesCSIscore);
        rankedCandidatesCSIscoreFiltered = Arrays.stream(rankedCandidatesCSIscore).filter(it -> filter.test(it.getCandidate()))
                .toArray(Scored[]::new);


        final Scored<FingerprintCandidate>[] rankedCandidatesCovscore;
        final Scored<FingerprintCandidate>[] rankedCandidatesCovscoreFiltered;

        if (scoringMethod == CovarianceScoring.Scorer.class) { // set as covariance scoring
            rankedCandidatesCovscore = allCandidates.clone();
        } else { //no scoring given that is useful for the confidence computation, recalculate all.
            rankedCandidatesCovscore = new Scored[allCandidates.length];
            for (int i = 0; i < allCandidates.length; i++)
                rankedCandidatesCovscore[i] = new Scored<>(allCandidates[i].getCandidate(), covarianceScoring.getScoring().score(query, allCandidates[i].getCandidate().getFingerprint()));
        }
        Arrays.sort(rankedCandidatesCovscore);
        rankedCandidatesCovscoreFiltered = Arrays.stream(rankedCandidatesCovscore).filter(it -> filter.test(it.getCandidate()))
                .toArray(Scored[]::new);

        return computeConfidence(exp, idResult, rankedCandidatesCovscore, rankedCandidatesCSIscore, rankedCandidatesCovscoreFiltered, rankedCandidatesCSIscoreFiltered, query);
    }

    @Override
    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult,
                                    Scored<FingerprintCandidate>[] ranked_candidates_covscore, Scored<FingerprintCandidate>[] ranked_candidates_csiscore, Scored<FingerprintCandidate>[] ranked_candidates_covscore_filtered, Scored<FingerprintCandidate>[] ranked_candidates_csiscore_filtered, ProbabilityFingerprint query) {

        if (ranked_candidates_covscore.length != ranked_candidates_csiscore.length)
            throw new IllegalArgumentException("Covariance scored candidate list has different length from fingerid scored candidates list!");

        if (ranked_candidates_covscore.length <= 1) {
            LoggerFactory.getLogger(getClass()).warn("Cannot calculate confidence with only one candidate in PubChem");
            return Double.NaN;
        }


        //find collision energy in spectrum
        String ce = CE_NOTHING;
        for (Ms2Spectrum spec : exp.getMs2Spectra()) {
            if (ce.equals(CE_NOTHING)) {
                ce = spec.getCollisionEnergy().toString();
            } else if (!ce.equals(spec.getCollisionEnergy().toString()) || spec.getCollisionEnergy().getMaxEnergy() != spec.getCollisionEnergy().getMinEnergy()) {
                ce = CE_RAMP;
                break;
            }
        }

        //calculate score for pubChem lists
        final CombinedFeatureCreatorALL pubchemConfidence = new CombinedFeatureCreatorALL(ranked_candidates_csiscore, ranked_candidates_covscore, csiFingerIdScoring.getPerfomances(), covarianceScoring);
        pubchemConfidence.prepare(csiFingerIdScoring.getPerfomances());
        final double[] pubchemConfidenceFeatures = pubchemConfidence.computeFeatures(query, idResult);
        final boolean sameTopHit = ranked_candidates_covscore[0] == ranked_candidates_covscore_filtered[0];
        final double pubchemConf = calculateConfidence(pubchemConfidenceFeatures, DB_ALL_ID, "", ce);

        //calculate score for filtered lists
        final CombinedFeatureCreator comb;
        final String distanceType;
        if (ranked_candidates_covscore_filtered.length > 1) {
            comb = new CombinedFeatureCreatorBIODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, ranked_candidates_csiscore_filtered, ranked_candidates_covscore_filtered, csiFingerIdScoring.getPerfomances(), covarianceScoring, pubchemConf, sameTopHit);
            distanceType = NO_DISATANCE_ID;

        } else {
            comb = new CombinedFeatureCreatorBIONODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, ranked_candidates_csiscore_filtered, ranked_candidates_covscore_filtered, csiFingerIdScoring.getPerfomances(), covarianceScoring, pubchemConf, sameTopHit);
            distanceType = DISATANCE_ID;
        }

        comb.prepare(csiFingerIdScoring.getPerfomances());
        final double[] bioConfidenceFeatures = comb.computeFeatures(query, idResult);
        return calculateConfidence(bioConfidenceFeatures, DB_BIO_ID, distanceType, ce);
    }

    private double calculateConfidence(@NotNull double[] feature, @NotNull String dbType, @NotNull String distanceType, @NotNull String collisionEnergy) {
        final TrainedSVM svm = trainedSVMs.get(dbType + distanceType + collisionEnergy);
        final double[][] featureMatrix = new double[1][feature.length];
        featureMatrix[0] = feature;
        SVMUtils.standardize_features(featureMatrix, svm.scales);
        SVMUtils.normalize_features(featureMatrix, svm.scales);
        return new SVMPredict().predict_confidence(featureMatrix, svm)[0];
    }
}
