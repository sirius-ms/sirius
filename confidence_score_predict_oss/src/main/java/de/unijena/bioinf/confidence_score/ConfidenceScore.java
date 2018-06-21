package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import de.unijena.bioinf.confidence_score.features.*;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.List;

/**
 * Created by martin on 20.06.18.
 */
public class ConfidenceScore {

    PredictionPerformance[] performance;
    CompoundWithAbstractFP<ProbabilityFingerprint> query;
    CompoundWithAbstractFP<Fingerprint>[] ranked_candidates;
    IdentificationResult idresult;



    public ConfidenceScore(CompoundWithAbstractFP<Fingerprint>[] ranked_candidates, PredictionPerformance[] performance, CompoundWithAbstractFP<ProbabilityFingerprint> query,IdentificationResult idresult){

        this.performance=performance;
        this.query=query;
        this.ranked_candidates=ranked_candidates;



    }


    public double[] compute_features(){

        CombinedFeatureCreator features_combined =  new CombinedFeatureCreator(new ScoreFeatures(), new PlattFeatures(), new DistanceFeatures()
        , new LogDistanceFeatures(), new PvalueFeatures(), new TreeFeatures());


        features_combined.prepare(performance);

        return features_combined.computeFeatures(query,ranked_candidates,idresult);





    }






}
