package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.*;

import java.util.Arrays;

/**
 * Created by Marcus Ludwig on 30.04.16.
 */
public class NormalizedToMedianScores implements FeatureCreator {
    private final ScoringMethod[] scoringMethods;
    private final String[] names;
    private final Scorer[] scorers;
    private FingerprintStatistics statistics;

    public NormalizedToMedianScores(){
        scoringMethods = new ScoringMethod[3];
        scoringMethods[0] = new MarvinsScoring();
        scoringMethods[1] = new MaximumLikelihoodScoring();
        scoringMethods[2] = new ProbabilityEstimateScoring();
        names = new String[scoringMethods.length];
        for (int i = 0; i < scoringMethods.length; i++) {
            names[i] = scoringMethods[i].getClass().getSimpleName();
        }

        scorers = new Scorer[scoringMethods.length];
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
//        final Candidate topHit = rankedCandidates[0];
        final double[] scores = new double[scoringMethods.length];
        for (int i = 0; i < scorers.length; i++) {
            Scorer scorer = scorers[i];
            scorer.preprocessQuery(query, statistics);

            double[] allScores = new double[rankedCandidates.length];
            for (int j = 0; j < rankedCandidates.length; j++) {
                allScores[j] = scorer.score(query, rankedCandidates[j], statistics);
            }
            double median = median(allScores);
            scores[i] = allScores[0]-median;
        }
        return scores;
    }


    private double mean(double[] numbers){
        double sum = 0.0;
        for (double number : numbers) sum += number;
        return sum/numbers.length;
    }

    /*
    sorts array!!!
     */
    public double median(double[] numbers){
        Arrays.sort(numbers);
        return numbers[numbers.length/2]; //todo ceil, floor?!?
    }


    @Override
    public int getFeatureSize() {
        return scoringMethods.length;
    }

    @Override
    public boolean isCompatible(Query query, Candidate[] rankedCandidates) {
        return rankedCandidates.length>0;
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
