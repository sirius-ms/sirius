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
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Created by martin on 20.06.18.
 */
public class CSICovarianceConfidenceScorer implements ConfidenceScorer {


    private final Map<String, TrainedSVM> trainedSVMs;
    private final CovarianceScoring covarianceScoring;
    private final CSIFingerIdScoring csiFingerIdScoring;


    //TODO: IdentificationResult is onyl for SIRIUS, not FingerID, so cant use it as tophit (needed at all?)

    public CSICovarianceConfidenceScorer(@NotNull Map<String, TrainedSVM> trainedsvms, @NotNull CovarianceScoring covarianceScoring, CSIFingerIdScoring csiFingerIDScoring) {
        this.trainedSVMs=trainedsvms;
        this.covarianceScoring = covarianceScoring;
        this.csiFingerIdScoring = csiFingerIDScoring;
    }


    public double computeConfidence(Ms2Experiment exp, Scored<FingerprintCandidate>[] allCandidates, Scored<FingerprintCandidate>[] filteredCandidates, ProbabilityFingerprint query, IdentificationResult idResult) {
        String ce = "nothing";
        for(Ms2Spectrum spec : exp.getMs2Spectra()){

            if(ce.equals("nothing")) {
                ce = spec.getCollisionEnergy().toString();
            }else if (!ce.equals(spec.getCollisionEnergy().toString()) || spec.getCollisionEnergy().getMaxEnergy()!=spec.getCollisionEnergy().getMinEnergy()){
                ce= "ramp";
                break;
            }
        }

        //TODO: Is covariance scoring the one used already?
        //todo: we could you an annotation of  the identification result? like idResult.getAnnotation(FingerprintScorerType.class)

        Scored<FingerprintCandidate>[] ranked_candidates_covscore = allCandidates;


        Scored<FingerprintCandidate>[] ranked_candidates_csiscore = new Scored[allCandidates.length];
        ArrayList<Scored<FingerprintCandidate>> candlist = new ArrayList<>();

        csiFingerIdScoring.prepare(query);

        for (int i = 0; i < allCandidates.length; i++)
            candlist.add(new Scored<>(allCandidates[i].getCandidate(), csiFingerIdScoring.score(query, allCandidates[i].getCandidate().getFingerprint())));


        Collections.sort(candlist);

        for (int i = 0; i < candlist.size(); i++)
            ranked_candidates_csiscore[i]=candlist.get(i);


        CombinedFeatureCreator comb = new CombinedFeatureCreator();

        //TODO load this


        String distanceType="distance";
        String dbType="bio";


        long flags = 0L; //todo change to filtered list.
        System.out.println("################ Replace Flags with filtered list############");
        if(flags==0){
            comb = new CombinedFeatureCreatorALL(ranked_candidates_csiscore, ranked_candidates_covscore, csiFingerIdScoring.getPerfomances(), covarianceScoring);
            dbType="all";
        }

        if((flags&4294967292L)!=0 && flags!=2){
            if(ranked_candidates_covscore.length>1) {
                //todo @Martin Attention Attention!
//                comb = new CombinedFeatureCreatorBIODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, csiFingerIdScoring.getPerfomances(), covarianceScoring);
            }else {
//                comb = new CombinedFeatureCreatorBIONODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, csiFingerIdScoring.getPerfomances(), covarianceScoring);
                distanceType="noDistance";
            }
        }

        TrainedSVM svm = trainedSVMs.get(dbType + "" + distanceType + "" + ce); //todo there should be global variable or enums for these identifiers.

        comb.prepare(csiFingerIdScoring.getPerfomances());

        double[] feature = comb.computeFeatures(query, idResult, flags);

        double[][]featureMatrix= new double[1][feature.length];

        featureMatrix[0]=feature;

        SVMPredict predict = new SVMPredict();

        SVMUtils utils = new SVMUtils();

        utils.standardize_features(featureMatrix,svm.scales);

        utils.normalize_features(featureMatrix,svm.scales);

        return predict.predict_confidence(featureMatrix,svm)[0];
    }









}
