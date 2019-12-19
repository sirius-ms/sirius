package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.fingerid.blast.*;
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public class ScoreFeaturesNonBio implements FeatureCreator {
    private final String[] names;
    private FingerblastScoring scoring;
    private PredictionPerformance[] statistics;
    Scored<FingerprintCandidate>[] rankedCandidates;
    Scored<FingerprintCandidate>[] rankedCandidates_filtered;
    public int weight_direction=1;


    public ScoreFeaturesNonBio(FingerblastScoring scoring, Scored<FingerprintCandidate>[] rankedCandidates,Scored<FingerprintCandidate>[] rankedCandidates_filtered){
        this.rankedCandidates=rankedCandidates;
        names = new String[]{scoring.toString()};
        this.scoring=scoring;
        this.rankedCandidates_filtered=rankedCandidates_filtered;
    }



    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics = statistics;
    }

    @Override
    public int weight_direction() {
        return weight_direction;
    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult) {

        assert  rankedCandidates[0].getScore()>=rankedCandidates[rankedCandidates.length-1].getScore();




        final FingerprintCandidate topHit = rankedCandidates_filtered[0].getCandidate();
        final FingerprintCandidate topHitpub = rankedCandidates[0].getCandidate();
        final double[] scores = new double[1];

        scoring.prepare(query);
        scores[0] = scoring.score(query, topHit.getFingerprint())-scoring.score(query,topHitpub.getFingerprint());


        return scores;
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(ProbabilityFingerprint query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return rankedCandidates.length>0;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 1;
    }

    @Override
    public String[] getFeatureNames() {


        return names;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //Nothing to do as long as ScoringMethods stay the same
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //Nothing to do as long as ScoringMethods stay the same
    }
}
