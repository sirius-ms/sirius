package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.confidence_score.Utils;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by martin on 20.06.18.
 */


/**
 *
 *
 computes distance features, max distance is variable, so are scorers. Top scoring hit is FIXED at this point!


 */


public class TanimotoToPredFeatures implements FeatureCreator {
    private int feature_size;
    private PredictionPerformance[] statistics;
    private Utils utils;
    Scored<FingerprintCandidate>[] rankedCandidates;
    long flags=-1;


    public TanimotoToPredFeatures(Scored<FingerprintCandidate>[] rankedCandidates){


        feature_size=1;
        this.rankedCandidates=rankedCandidates;

    }


    public TanimotoToPredFeatures(Scored<FingerprintCandidate>[] rankedCandidates, long flags){


        feature_size=1;
        this.rankedCandidates=rankedCandidates;
        this.flags=flags;

    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {this.statistics=statistics;

    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, IdentificationResult idresult, long flags) {


        utils= new Utils();

        if(this.flags==-1)this.flags=flags;

        rankedCandidates=utils.condense_candidates_by_flag(rankedCandidates,this.flags);



        double[] scores =  new double[feature_size];



        final double topHit = rankedCandidates[0].getScore();



            scores[0] = rankedCandidates[0].getCandidate().getFingerprint().tanimoto(query.getFingerprint().asDeterministic());


        return scores;



    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return false;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 1;
    }

    @Override
    public String[] getFeatureNames()
    {

        String[] names = new String[1];
       names[0]= "tanimotodistanceprediction";
    return names;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
