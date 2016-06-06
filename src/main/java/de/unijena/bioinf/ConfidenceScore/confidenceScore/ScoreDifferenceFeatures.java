package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.*;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public class ScoreDifferenceFeatures implements FeatureCreator {
    private final ScoringMethod[] scoringMethods;
    private final Scorer[] scorers;
    private FingerprintStatistics statistics;
    private int[] positions;
    private int max;

    public ScoreDifferenceFeatures(){
        this(new int[]{});
    }

    /**
     *
     * @param positions the positions
     */
    public ScoreDifferenceFeatures(int... positions){
        scoringMethods = new ScoringMethod[3];
        scoringMethods[0] = new MarvinsScoring();
        scoringMethods[1] = new MaximumLikelihoodScoring();
        scoringMethods[2] = new ProbabilityEstimateScoring();
        scorers = new Scorer[scoringMethods.length];

        this.positions = positions;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < positions.length; i++) max = Math.max(max, positions[i]);
        this.max = max;
    }

    @Override
    public void prepare(FingerprintStatistics statistics) {
        this.statistics = statistics;
        for (int i = 0; i < scoringMethods.length; i++) {
            scorers[i] = scoringMethods[i].getScorer(statistics);
        }
    }

    @Override
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates) {
        double[] scores = new double[scoringMethods.length * positions.length];
        final Candidate topHit = rankedCandidates[0];
        int pos = 0;
        for (int i = 0; i < scorers.length; i++) {
            scorers[i].preprocessQuery(query, statistics);
            final double topScore = scorers[i].score(query, topHit, statistics);
            for (int j = 0; j < positions.length; j++) {
                scores[pos++] = topScore - scorers[i].score(query, rankedCandidates[positions[j]], statistics);
            }
        }
        assert pos == scores.length;
        return scores;
    }

    @Override
    public int getFeatureSize() {
        return scoringMethods.length*positions.length;
    }

    @Override
    public boolean isCompatible(Query query, Candidate[] rankedCandidates) {
        return (rankedCandidates.length>max);
    }

    @Override
    public String[] getFeatureNames() {
        String[] names = new String[getFeatureSize()];
        int pos = 0;
        for (int i = 0; i < scoringMethods.length; i++) {
            String scoringMethod = scoringMethods[i].getClass().getSimpleName();
            for (int j = 0; j < positions.length; j++) {
                int position = positions[j];
                names[pos++] = scoringMethod+"DiffTo"+position;
            }
        }
        return names;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L positionsList = document.getListFromDictionary(dictionary, "positions");
        int size = document.sizeOfList(positionsList);
        int[] positions = new int[size];
        for (int i = 0; i < size; i++) positions[i] = (int)document.getIntFromList(positionsList, i);

        this.positions = positions;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < positions.length; i++) max = Math.max(max, positions[i]);
        this.max = max;
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (int position : positions) document.addToList(list, position);
        document.addListToDictionary(dictionary, "positions", list);
    }
}
