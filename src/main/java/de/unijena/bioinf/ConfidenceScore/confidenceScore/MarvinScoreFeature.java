package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.*;

/**
 * Created by Marcus Ludwig on 21.04.16.
 */
public class MarvinScoreFeature implements FeatureCreator {
    private final ScoringMethod scoringMethod;
    private final String name;
    private Scorer scorer;
    private FingerprintStatistics statistics;

    public MarvinScoreFeature(){
        scoringMethod = new MarvinsScoring();
        name = scoringMethod.getClass().getSimpleName();
    }

    @Override
    public void prepare(FingerprintStatistics statistics) {
        this.statistics = statistics;
        scorer = scoringMethod.getScorer(statistics);
    }

    @Override
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates) {
        final Candidate topHit = rankedCandidates[0];
        final double[] scores = new double[1];
        scorer.preprocessQuery(query, statistics);
        scores[0] = scorer.score(query, topHit, statistics);
        return scores;
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(Query query, Candidate[] rankedCandidates) {
        return rankedCandidates.length>0;
    }

    @Override
    public String[] getFeatureNames() {
        return new String[]{name};
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
