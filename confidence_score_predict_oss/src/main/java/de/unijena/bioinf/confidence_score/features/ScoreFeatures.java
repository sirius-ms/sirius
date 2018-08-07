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
import de.unijena.bioinf.fingerid.blast.*;
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public class ScoreFeatures implements FeatureCreator {
    private final String[] names;
    private FingerblastScoring scoring;
    private PredictionPerformance[] statistics;
    private Utils utils;
    Scored<FingerprintCandidate>[] rankedCandidates;
    long flags;

    public ScoreFeatures(FingerblastScoring scoring, Scored<FingerprintCandidate>[] rankedCandidates){
        this.rankedCandidates=rankedCandidates;
        names = new String[]{scoring.toString()};
        this.scoring=scoring;
    }
    public ScoreFeatures(FingerblastScoring scoring, Scored<FingerprintCandidate>[] rankedCandidates, long flags){
        this.rankedCandidates=rankedCandidates;
        names = new String[]{scoring.toString()};
        this.scoring=scoring;
        this.flags=flags;
    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics = statistics;
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, IdentificationResult idresult,long flags) {

        utils= new Utils();
        if(this.flags==-1)this.flags=flags;


        rankedCandidates=utils.condense_candidates_by_flag(rankedCandidates,this.flags);


        final FingerprintCandidate topHit = rankedCandidates[0].getCandidate();
        final double[] scores = new double[1];

        scoring.prepare(query.getFingerprint());
        scores[0] = scoring.score(query.getFingerprint(), topHit.getFingerprint());

        return scores;
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
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
