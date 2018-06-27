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
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.CovarianceScoring;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 20.06.18.
 */
public class ConfidenceScore {

    PredictionPerformance[] performance;
    CompoundWithAbstractFP<ProbabilityFingerprint> query;
    Scored<FingerprintCandidate>[] ranked_candidates;
    IdentificationResult idresult;
    Ms2Experiment exp;
    CSIFingerIdScoring csiscoring;
    CovarianceScoring covscore;
    int flags;



    //TODO: IdentificationResult is onyl for SIRIUS, not FingerID, so cant use it as tophit (needed at all?)

    public ConfidenceScore(Ms2Experiment exp, Scored<FingerprintCandidate>[] ranked_candidates, PredictionPerformance[] performance, CompoundWithAbstractFP<ProbabilityFingerprint> query, IdentificationResult idresult, CSIFingerIdScoring csiscoring, CovarianceScoring covscore, int flags){

        this.performance=performance;
        this.query=query;
        this.ranked_candidates=ranked_candidates;
        this.exp=exp;
        this.idresult=idresult;
        this.csiscoring=csiscoring;
        this.covscore=covscore;
        this.flags=flags;




    }



    public double[] compute_features(){

        CombinedFeatureCreator features_combined =  new CombinedFeatureCreator(new ScoreFeatures(csiscoring), new PlattFeatures(), new DistanceFeatures()
        , new LogDistanceFeatures(), new PvalueFeatures(), new TreeFeatures());


        features_combined.prepare(performance);

        return features_combined.computeFeatures(query,ranked_candidates,idresult,flags);





    }






}
