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
import java.util.List;

/**
 * Created by martin on 20.06.18.
 */
public class ConfidenceScoreComputor {


    ArrayList<TrainedSVM> trainedSVMs;

    //TODO: IdentificationResult is onyl for SIRIUS, not FingerID, so cant use it as tophit (needed at all?)

    public ConfidenceScoreComputor(ArrayList<TrainedSVM> trainedsvms){

        this.trainedSVMs=trainedsvms;




    }



    public double compute_confidence(Ms2Experiment exp, Scored<FingerprintCandidate>[] ranked_candidates, PredictionPerformance[] performance, CompoundWithAbstractFP<ProbabilityFingerprint> query, IdentificationResult idresult, CSIFingerIdScoring csiscoring, CovarianceScoring covscore, long flags){


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

        Collections.sort(candlist);

        for(int i=0;i<candlist.size();i++){
            ranked_candidates_csiscore[i]=candlist.get(i);
        }




        CombinedFeatureCreator comb = new CombinedFeatureCreator();

        //TODO load this
        TrainedSVM svm = new TrainedSVM(null,null,null);


        int max_distance=1; //



        if(flags==0){

           comb= new CombinedFeatureCreatorBIODISTANCE(ranked_candidates_csiscore,ranked_candidates_covscore,performance,covscore);



        }

        if((flags&4294967292L)!=0 && flags!=2){

            if(ranked_candidates_covscore.length>1) {


                comb = new CombinedFeatureCreatorBIODISTANCE(ranked_candidates_csiscore,ranked_candidates_covscore,performance,covscore);

            }else {
                comb =  new CombinedFeatureCreatorBIONODISTANCE(ranked_candidates_csiscore,ranked_candidates_covscore,performance,covscore);

            }
        }



        comb.prepare(performance);



        double[] feature = comb.computeFeatures(query,idresult,flags);


        double[][]featureMatrix= new double[1][feature.length];

        featureMatrix[0]=feature;

        SVMPredict predict = new SVMPredict();



        SVMUtils utils = new SVMUtils();

        utils.standardize_features(featureMatrix,svm.scales);

        utils.normalize_features(featureMatrix,svm.scales);

        return predict.predict_confidence(featureMatrix,svm)[0];








    }









}
