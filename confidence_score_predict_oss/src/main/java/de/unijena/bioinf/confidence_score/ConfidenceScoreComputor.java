package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.features.*;
import de.unijena.bioinf.confidence_score.svm.SVMPredict;
import de.unijena.bioinf.confidence_score.svm.SVMUtils;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.CovarianceScoring;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.ScoringMethodFactory;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by martin on 20.06.18.
 */
public class ConfidenceScoreComputor {


    HashMap<String,TrainedSVM> trainedSVMs;
    PredictionPerformance[] performances;

    //TODO: IdentificationResult is onyl for SIRIUS, not FingerID, so cant use it as tophit (needed at all?)

    public ConfidenceScoreComputor(HashMap<String,TrainedSVM> trainedsvms, PredictionPerformance[] performances){

        this.trainedSVMs=trainedsvms;
        this.performances=performances;




    }

   public PredictionPerformance[] getPerformances(){
        return performances;
   }

   public double compute_fast_confidence(Ms2Experiment exp, Scored<FingerprintCandidate>[] ranked_candidates,CompoundWithAbstractFP<ProbabilityFingerprint> query, IdentificationResult idresult,CSIFingerIdScoring csiscoring, CovarianceScoring covscore,long flags){


       return 0;
   }



    public double compute_confidence(Ms2Experiment exp, Scored<FingerprintCandidate>[] ranked_candidates,  CompoundWithAbstractFP<ProbabilityFingerprint> query, IdentificationResult idresult, CSIFingerIdScoring csiscoring, CovarianceScoring covscore, long flags){


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

        Scored<FingerprintCandidate>[] ranked_candidates_covscore = ranked_candidates;


        Scored<FingerprintCandidate>[] ranked_candidates_csiscore = new Scored[ranked_candidates.length];
        ArrayList<Scored<FingerprintCandidate>> candlist = new ArrayList<>();

        csiscoring.prepare(query.getFingerprint());

        for(int i=0;i<ranked_candidates.length;i++){


            candlist.add(new Scored<>(ranked_candidates[i].getCandidate(),csiscoring.score(query.getFingerprint(),ranked_candidates[i].getCandidate().getFingerprint())));




        }

        Collections.sort(candlist,Collections.reverseOrder());

        for(int i=0;i<candlist.size();i++){
            ranked_candidates_csiscore[i]=candlist.get(i);
        }




        CombinedFeatureCreator comb = new CombinedFeatureCreator();

        //TODO load this


        String distanceType;
        String dbType;



        //TODO: output both bio and pubchem confidence?

        TrainedSVM svm = trainedSVMs.get("fe30_bio_distance.svm");
        Utils utils = new Utils();


        if(flags==0){

           comb= new CombinedFeatureCreatorALL(ranked_candidates_csiscore,ranked_candidates_covscore,performances,covscore);
            dbType="all";
            distanceType="distance";
            svm= trainedSVMs.get("fe"+ce+"_"+dbType+"_"+distanceType+".svm");

        }

        if((flags&4294967292L)!=0 && flags!=2){
            dbType="bio";

            //TODO: change distance stuff back, reenable BIOr

            if(utils.condense_candidates_by_flag(ranked_candidates_covscore,flags).length>1) {
                distanceType="distance";
//                comb = new CombinedFeatureCreatorBIODISTANCE(ranked_candidates_csiscore,ranked_candidates_covscore,performances,covscore);
                svm= trainedSVMs.get("fe"+ce+"_"+dbType+"_"+distanceType+".svm");


            }else {
                if(utils.condense_candidates_by_flag(ranked_candidates_covscore,flags).length>0) {
                  //  comb = new CombinedFeatureCreatorBIONODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, performances, covscore);
                    distanceType = "noDistance";

                    svm= trainedSVMs.get("fe"+ce+"_"+dbType+"_"+distanceType+".svm");
                }else {
                    return 0;
                }
            }
        }
        //TODO: testing only



        comb.prepare(performances);



        double[] feature = comb.computeFeatures(query,idresult,flags);

        String featurestring="";
        for(int i=0;i<feature.length;i++){
            featurestring+=feature[i]+" ";
        }
        System.out.println(featurestring);


        double[][]featureMatrix= new double[1][feature.length];

        featureMatrix[0]=feature;

        SVMPredict predict = new SVMPredict();



        SVMUtils svmutils = new SVMUtils();

        svmutils.standardize_features(featureMatrix,svm.scales);

        svmutils.normalize_features(featureMatrix,svm.scales);

        return predict.predict_confidence(featureMatrix,svm)[0];








    }









}
