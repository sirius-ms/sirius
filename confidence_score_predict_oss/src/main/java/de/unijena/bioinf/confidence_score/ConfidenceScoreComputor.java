package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by martin on 20.06.18.
 */
public class ConfidenceScoreComputor {

    PredictionPerformance[] performance;
    CompoundWithAbstractFP<ProbabilityFingerprint> query;
    Scored<FingerprintCandidate>[] ranked_candidates;
    IdentificationResult idresult;
    Ms2Experiment exp;
    CSIFingerIdScoring csiscoring;
    CovarianceScoring covscore;
    int flags;



    //TODO: IdentificationResult is onyl for SIRIUS, not FingerID, so cant use it as tophit (needed at all?)

    public ConfidenceScoreComputor(Ms2Experiment exp, Scored<FingerprintCandidate>[] ranked_candidates, PredictionPerformance[] performance, CompoundWithAbstractFP<ProbabilityFingerprint> query, IdentificationResult idresult, CSIFingerIdScoring csiscoring, CovarianceScoring covscore, int flags){

        this.performance=performance;
        this.query=query;
        this.ranked_candidates=ranked_candidates;
        this.exp=exp;
        this.idresult=idresult;
        this.csiscoring=csiscoring;
        this.covscore=covscore;
        this.flags=flags;




    }



    public double compute_confidence(){

        //score list with cov scoring TODO: Here or take it as argument?


        Scored<FingerprintCandidate>[] ranked_candidates_covscore = new Scored[ranked_candidates.length];
        ArrayList<Scored<FingerprintCandidate>> candlist = new ArrayList<>();

        covscore.getScoring().prepare(query.getFingerprint());

        for(int i=0;i<ranked_candidates.length;i++){


            candlist.add(new Scored<>(ranked_candidates[i].getCandidate(),covscore.getScoring().score(query.getFingerprint(),ranked_candidates[i].getCandidate().getFingerprint())));




        }

        Collections.sort(candlist);

        for(int i=0;i<candlist.size();i++){
            ranked_candidates_covscore[i]=candlist.get(i);
        }




        CombinedFeatureCreator comb = new CombinedFeatureCreator();
        TrainedSVM svm = new TrainedSVM(null,null,null);

        int max_distance=1; //



        if(flags==2){

           // comb = getAllCreator();
            svm.import_parameters(new File(" all"));

        }

        if((flags&4294967292L)!=0 && flags!=2){

            if(ranked_candidates.length>1) {

                comb = new CombinedFeatureCreatorBIODISTANCE(ranked_candidates,ranked_candidates_covscore,performance,covscore);
                svm.import_parameters(new File(" bioDistance"));
            }else {
                comb =  new CombinedFeatureCreatorBIONODISTANCE(ranked_candidates,ranked_candidates_covscore,performance,covscore);
                svm.import_parameters(new File("bioNoDistance"));
            }
        }



        comb.prepare(this.performance);



        double[] feature = comb.computeFeatures(this.query,this.idresult,this.flags);


        double[][]featureMatrix= new double[1][feature.length];

        featureMatrix[0]=feature;

        SVMPredict predict = new SVMPredict();



        SVMUtils utils = new SVMUtils();

        utils.standardize_features(featureMatrix,svm.scales);

        utils.normalize_features(featureMatrix,svm.scales);

        return predict.predict_confidence(featureMatrix,svm)[0];








    }









}
