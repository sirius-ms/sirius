package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.svm.SVMPredict;
import de.unijena.bioinf.confidence_score.svm.SVMUtils;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.CovarianceScoring;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by martin on 20.06.18.
 */
public class CSICovarianceConfidenceScorer implements ConfidenceScorer {


    private final Map<String, TrainedSVM> trainedSVMs;
    private final CovarianceScoring covarianceScoring;
    private final CSIFingerIdScoring csiFingerIdScoring;


    //TODO: IdentificationResult is only for SIRIUS, not FingerID, so cant use it as tophit (needed at all?)

    public CSICovarianceConfidenceScorer(@NotNull Map<String, TrainedSVM> trainedsvms, @NotNull CovarianceScoring covarianceScoring, @NotNull CSIFingerIdScoring csiFingerIDScoring) {
        this.trainedSVMs=trainedsvms;
        this.covarianceScoring = covarianceScoring;
        this.csiFingerIdScoring = csiFingerIDScoring;
    }

    @Override
    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, Scored<FingerprintCandidate>[] filteredCandidates, ProbabilityFingerprint query, final long filterFlag) {
        String ce = "nothing";
        for(Ms2Spectrum spec : exp.getMs2Spectra()){
            if(ce.equals("nothing")) {
                ce = spec.getCollisionEnergy().toString();
            }else if (!ce.equals(spec.getCollisionEnergy().toString()) || spec.getCollisionEnergy().getMaxEnergy()!=spec.getCollisionEnergy().getMinEnergy()){
                ce= "ramp";
                break;
            }
        }

        //todo why do we need to re-score here, could we not just check if the scoring is already correct?
        //todo: we could use an annotation of  the identification result? like idResult.getAnnotation(FingerprintScorerType.class)

        //re-scoring the candidates?
        Scored<FingerprintCandidate>[] ranked_candidates_csiscore = new Scored[allCandidates.length];
        Scored<FingerprintCandidate>[] ranked_candidates_covscore = new Scored[allCandidates.length];

        csiFingerIdScoring.prepare(query);
        for (int i = 0; i < allCandidates.length; i++) {
            ranked_candidates_csiscore[i] = new Scored<>(allCandidates[i].getCandidate(), csiFingerIdScoring.score(query, allCandidates[i].getCandidate().getFingerprint()));
            ranked_candidates_covscore[i] = new Scored<>(allCandidates[i].getCandidate(), covarianceScoring.getScoring().score(query, allCandidates[i].getCandidate().getFingerprint()));
        }
        Arrays.sort(ranked_candidates_csiscore);
        Arrays.sort(ranked_candidates_covscore);


        //todo @Martin, does this make sense?
        CombinedFeatureCreator comb = new CombinedFeatureCreator();
        String distanceType="distance";
        String dbType="bio";

        if (filterFlag == 0) {
            comb = new CombinedFeatureCreatorALL(ranked_candidates_csiscore, ranked_candidates_covscore, csiFingerIdScoring.getPerfomances(), covarianceScoring);
            dbType = "all";
        }else if (DatasourceService.isBio(filterFlag) && filterFlag!=2) { //todo is != pubchem really needed.
            if(ranked_candidates_covscore.length>1) {
                //todo set missing values
//                comb = new CombinedFeatureCreatorBIODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, csiFingerIdScoring.getPerfomances(), covarianceScoring);
            }else {
//                comb = new CombinedFeatureCreatorBIONODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, csiFingerIdScoring.getPerfomances(), covarianceScoring);
                distanceType="noDistance";
            }
        }


        TrainedSVM svm = trainedSVMs.get(dbType + "" + distanceType + "" + ce); //todo there should be global variable or enums for these identifiers.

        comb.prepare(csiFingerIdScoring.getPerfomances());

        double[] feature = comb.computeFeatures(query, idResult, filterFlag);

        double[][]featureMatrix= new double[1][feature.length];

        featureMatrix[0]=feature;

        SVMPredict predict = new SVMPredict();

        SVMUtils utils = new SVMUtils();

        utils.standardize_features(featureMatrix,svm.scales);

        utils.normalize_features(featureMatrix,svm.scales);

        return predict.predict_confidence(featureMatrix,svm)[0];
    }
}
