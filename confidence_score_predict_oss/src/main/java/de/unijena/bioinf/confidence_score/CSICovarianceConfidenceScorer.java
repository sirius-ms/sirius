package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
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
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.CovarianceScoringMethod;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.fingerid.blast.ScoringMethodFactory;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by martin on 20.06.18.
 */
public class CSICovarianceConfidenceScorer implements ConfidenceScorer {
    public static final String NO_DISTANCE_ID = "Nodist";
    public static final String DISTANCE_ID = "dist";

    public static final String DB_ALL_ID = "All";
    public static final String DB_BIO_ID = "Bio";

    public static final String CE_LOW = "feLow";
    public static final String CE_MED = "feMed";
    public static final String CE_HIGH = "feHi";
    public static final String CE_RAMP = "feRAMP";


    private final Map<String, TrainedSVM> trainedSVMs;
    private final CovarianceScoringMethod covarianceScoringMethod;
    private final ScoringMethodFactory.CSIFingerIdScoringMethod csiFingerIdScoringMethod;
    private Class<? extends FingerblastScoringMethod> scoringOfInput;


    /*public CSICovarianceConfidenceScorer(@NotNull Map<String, TrainedSVM> trainedsvms, @NotNull CovarianceScoring covarianceScoring, @NotNull CSIFingerIdScoring csiFingerIDScoring) {

    }*/

    public CSICovarianceConfidenceScorer(@NotNull Map<String, TrainedSVM> trainedsvms, @NotNull CovarianceScoringMethod covarianceScoringMethod, @NotNull ScoringMethodFactory.CSIFingerIdScoringMethod csiFingerIDScoringMethod, Class<? extends FingerblastScoringMethod> scoringOfInput) {
        this.trainedSVMs = trainedsvms;
        this.covarianceScoringMethod = covarianceScoringMethod;
        this.csiFingerIdScoringMethod = csiFingerIDScoringMethod;
        this.scoringOfInput = scoringOfInput;
    }

    public Class<? extends FingerblastScoringMethod> getScoringOfInput() {
        return scoringOfInput;
    }

    public void setScoringOfInput(Class<? extends FingerblastScoringMethod> scoringOfInput) {
        this.scoringOfInput = scoringOfInput;
    }

    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, Scored<FingerprintCandidate>[] filteredCandidates, ProbabilityFingerprint query) {
        return computeConfidence(exp, idResult, allCandidates, Arrays.stream(filteredCandidates).map(Scored::getCandidate).collect(Collectors.toSet()), query);
    }


    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, long dbFilterFlag, ProbabilityFingerprint query) {
        return computeConfidence(exp, idResult, allCandidates, query, it -> (it.getBitset() & dbFilterFlag) != 0);
    }

    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, Set<FingerprintCandidate> candidateFilter, ProbabilityFingerprint query) {
        return computeConfidence(exp, idResult, allCandidates, query, candidateFilter::contains);
    }

    @Override
    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, ProbabilityFingerprint query, @NotNull final Predicate<FingerprintCandidate> filter) {
        return computeConfidence(exp, idResult, allCandidates, scoringOfInput, query, filter);

    }

    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, Class<? extends FingerblastScoringMethod> scoringMethod, ProbabilityFingerprint query, @NotNull final Predicate<FingerprintCandidate> filter) {
        //re-scoring the candidates?
        final Scored<FingerprintCandidate>[] rankedCandidatesCSIscore;
        final Scored<FingerprintCandidate>[] rankedCandidatesCSIscoreFiltered;

        if (scoringMethod == ScoringMethodFactory.CSIFingerIdScoringMethod.class) { //set as csi covariance scoring
            rankedCandidatesCSIscore = allCandidates.clone();
        } else {
            final CSIFingerIdScoring csiFingerIdScoring = csiFingerIdScoringMethod.getScoring();
            csiFingerIdScoring.prepare(query);
            rankedCandidatesCSIscore = new Scored[allCandidates.length];
            for (int i = 0; i < allCandidates.length; i++)
                rankedCandidatesCSIscore[i] = new Scored<>(allCandidates[i].getCandidate(), csiFingerIdScoring.score(query, allCandidates[i].getCandidate().getFingerprint()));
        }
        Arrays.sort(rankedCandidatesCSIscore, Scored.desc());
        rankedCandidatesCSIscoreFiltered = Arrays.stream(rankedCandidatesCSIscore).filter(it -> filter.test(it.getCandidate()))
                .toArray(Scored[]::new);


        final Scored<FingerprintCandidate>[] rankedCandidatesCovscore;
        final Scored<FingerprintCandidate>[] rankedCandidatesCovscoreFiltered;

        final CovarianceScoringMethod.Scoring covarianceScoring = covarianceScoringMethod.getScoring();
        covarianceScoring.prepare(query);
        if (scoringMethod == CovarianceScoringMethod.class) { // set as covariance scoring
            rankedCandidatesCovscore = allCandidates.clone();
        } else { //no scoring given that is useful for the confidence computation, recalculate all.
            rankedCandidatesCovscore = new Scored[allCandidates.length];
            for (int i = 0; i < allCandidates.length; i++)
                rankedCandidatesCovscore[i] = new Scored<>(allCandidates[i].getCandidate(), covarianceScoring.score(query, allCandidates[i].getCandidate().getFingerprint()));
        }
        Arrays.sort(rankedCandidatesCovscore, Scored.desc());
        rankedCandidatesCovscoreFiltered = Arrays.stream(rankedCandidatesCovscore).filter(it -> filter.test(it.getCandidate()))
                .toArray(Scored[]::new);

        return computeConfidence(exp, idResult, rankedCandidatesCovscore, rankedCandidatesCSIscore, rankedCandidatesCovscoreFiltered, rankedCandidatesCSIscoreFiltered, query, covarianceScoring, csiFingerIdScoringMethod.getPerformances());
    }


    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult,
                                    Scored<FingerprintCandidate>[] ranked_candidates_covscore, Scored<FingerprintCandidate>[] ranked_candidates_csiscore,
                                    Scored<FingerprintCandidate>[] ranked_candidates_covscore_filtered, Scored<FingerprintCandidate>[] ranked_candidates_csiscore_filtered,
                                    ProbabilityFingerprint query, CovarianceScoringMethod.Scoring covarianceScoring, PredictionPerformance[] csiPerformances) {

        if (ranked_candidates_covscore.length != ranked_candidates_csiscore.length)
            throw new IllegalArgumentException("Covariance scored candidate list has different length from fingerid scored candidates list!");

        if (ranked_candidates_covscore.length <= 1 || ranked_candidates_covscore_filtered.length <= 1) {
            LoggerFactory.getLogger(getClass()).warn("Cannot calculate confidence with only one hit in structure database! Returning NaN.");
            return Double.NaN;
        }

        final String ce = makeCeString(exp.getMs2Spectra());

        //calculate score for pubChem lists
        final CombinedFeatureCreatorALL pubchemConfidence = new CombinedFeatureCreatorALL(ranked_candidates_csiscore, ranked_candidates_covscore, csiPerformances, covarianceScoring);
        pubchemConfidence.prepare(csiPerformances);
        final double[] pubchemConfidenceFeatures = pubchemConfidence.computeFeatures(query, idResult);
        final boolean sameTopHit = ranked_candidates_covscore[0] == ranked_candidates_covscore_filtered[0];
        final double pubchemConf = calculateConfidence(pubchemConfidenceFeatures, DB_ALL_ID, "", ce);

        //calculate score for filtered lists
        final CombinedFeatureCreator comb;
        final String distanceType;
        if (ranked_candidates_covscore_filtered.length > 1) {
            comb = new CombinedFeatureCreatorBIODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, ranked_candidates_csiscore_filtered, ranked_candidates_covscore_filtered, csiPerformances, covarianceScoring, pubchemConf, sameTopHit);
            distanceType = DISTANCE_ID;

        } else {
            comb = new CombinedFeatureCreatorBIONODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, ranked_candidates_csiscore_filtered, ranked_candidates_covscore_filtered, csiPerformances, covarianceScoring, pubchemConf, sameTopHit);
            distanceType = NO_DISTANCE_ID;
        }

        comb.prepare(csiPerformances);
        final double[] bioConfidenceFeatures = comb.computeFeatures(query, idResult);
        return calculateConfidence(bioConfidenceFeatures, DB_BIO_ID, distanceType, ce);
    }

    private double calculateConfidence(@NotNull double[] feature, @NotNull String dbType, @NotNull String distanceType, @NotNull String collisionEnergy) {
        final String id = collisionEnergy + "_" + dbType + distanceType + ".svm";
        final TrainedSVM svm = trainedSVMs.get(id);
        if (svm == null)
            throw new IllegalArgumentException("Could not found confidence svm with ID: \"" + id + "\"");
        final double[][] featureMatrix = new double[1][feature.length];
        featureMatrix[0] = feature;
        SVMUtils.standardize_features(featureMatrix, svm.scales);
        SVMUtils.normalize_features(featureMatrix, svm.scales);
        return new SVMPredict().predict_confidence(featureMatrix, svm)[0];
    }

    public static String makeCeString(@NotNull final List<Ms2Spectrum<Peak>> spectra) {
        //find collision energy in spectrum
        double ceMin = Double.MAX_VALUE;
        double ceMax = Double.MIN_VALUE;
        for (Ms2Spectrum spec : spectra) {
            CollisionEnergy ce = spec.getCollisionEnergy();
            if (ce == null) return CE_RAMP;
            ceMax = Math.max(ceMax, ce.getMaxEnergy());
            ceMin = Math.min(ceMin, ce.getMinEnergy());
            if (ceMin != ceMax) return CE_RAMP;
        }

        if (ceMin <= 15) //10
            return CE_LOW;
        else if (ceMin <= 30) //20
            return CE_MED;
        else
            return CE_HIGH; //40
    }
}
