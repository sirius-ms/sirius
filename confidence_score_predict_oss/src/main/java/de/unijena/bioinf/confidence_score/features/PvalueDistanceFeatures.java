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
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by martin on 20.06.18.
 */


/**
 *
 *
 computes distance features, max distance is variable, so are scorers. Top scoring hit is FIXED at this point!


 */


public class PvalueDistanceFeatures implements FeatureCreator {
    private int[] distances;
    private int feature_size;
    private PredictionPerformance[] statistics;
    Scored<FingerprintCandidate>[] rankedCandidates;
    Scored<FingerprintCandidate>[] rankedCandidates_filtered;


    public PvalueDistanceFeatures(Scored<FingerprintCandidate>[] rankedCandidates,Scored<FingerprintCandidate>[] rankedCandidates_filtered,int... distances){

        this.distances=distances;
        feature_size=distances.length;
        this.rankedCandidates=rankedCandidates;
        this.rankedCandidates=rankedCandidates_filtered;

    }




    @Override
    public void prepare(PredictionPerformance[] statistics) {this.statistics=statistics;

    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult) {



        PvalueScoreUtils putils = new PvalueScoreUtils();


        double[] scores =  new double[feature_size];


        int pos = 0;


        for (int j = 0; j < distances.length; j++) {

            scores[pos++] = putils.computePvalueScore(rankedCandidates,rankedCandidates_filtered, rankedCandidates_filtered[0]) - putils.computePvalueScore(rankedCandidates,rankedCandidates_filtered,rankedCandidates_filtered[distances[j]]);
        }

        assert pos == scores.length;
        return scores;



    }

    @Override
    public int getFeatureSize() {
        return distances.length;
    }

    @Override
    public boolean isCompatible(ProbabilityFingerprint query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return false;
    }

    @Override
    public int getRequiredCandidateSize() {
        return distances.length;
    }

    @Override
    public String[] getFeatureNames()
    {
        String[] name=  new String[distances.length];
        for(int i=0;i<distances.length;i++){
            name[i]="pvaluedistance_"+distances[i];
        }
        return name;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
