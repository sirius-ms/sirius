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
public class LogDistanceFeatures implements FeatureCreator {
    private Utils utils;
    private int[] distances;
    private FingerblastScoring[] scorers;
    private int feature_size;
    private PredictionPerformance[] statistics;



    public LogDistanceFeatures(int... distances){

        this.distances=distances;
        feature_size=distances.length;

    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, Scored<FingerprintCandidate>[] rankedCandidates, IdentificationResult idresult,long flags) {
        scorers[0] = new CSIFingerIdScoring(statistics);

        rankedCandidates=utils.condense_candidates_by_flag(rankedCandidates,flags);


        double[] scores =  new double[feature_size*scorers.length];



        final FingerprintCandidate topHit = rankedCandidates[0].getCandidate();
        int pos = 0;
        for (int i = 0; i < scorers.length; i++) {
            scorers[i].prepare(query.getFingerprint());
            final double topScore = scorers[i].score(query.getFingerprint(), topHit.getFingerprint());
            for (int j = 0; j < distances.length; j++) {
                scores[pos++] = Math.log(topScore - scorers[i].score(query.getFingerprint(), rankedCandidates[distances[j]].getCandidate().getFingerprint()));
            }
        }
        assert pos == scores.length;
        return scores;
    }

    @Override
    public int getFeatureSize() {
        return 0;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return false;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 0;
    }

    @Override
    public String[] getFeatureNames() {
        return new String[0];
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
