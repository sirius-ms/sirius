package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.fingerid.blast.ProbabilityEstimateScoring;
import de.unijena.bioinf.fingerid.blast.SimpleMaximumLikelihoodScoring;

import java.util.Arrays;

/**
 * Created by Marcus Ludwig on 30.04.16.
 */
public class NormalizedToMedianMeanScores implements FeatureCreator {
    private final String[] names;
    private final FingerblastScoring[] scorers;
    private int[] positions;

    public NormalizedToMedianMeanScores(int... positions){
        names = new String[]{"CSIFingerIdScoringMedian", "SimpleMaximumLikelihoodScoringMedian", "ProbabilityEstimateScoringMedian",
                                "CSIFingerIdScoringAvg", "SimpleMaximumLikelihoodScoringAvg", "ProbabilityEstimateScoringAvg"};
        scorers = new FingerblastScoring[3];
        this.positions = positions;
    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        scorers[0] = new CSIFingerIdScoring(statistics);
        scorers[1] = new SimpleMaximumLikelihoodScoring(statistics);
        scorers[2] = new ProbabilityEstimateScoring(statistics);
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        final double[] scores = new double[scorers.length*2*positions.length];
        int scoresPos = 0;
        for (int i = 0; i < scorers.length; i++) {
            FingerblastScoring scorer = scorers[i];
            scorer.prepare(query.getFingerprint());

            double[] allScores = new double[rankedCandidates.length];
            for (int j = 0; j < rankedCandidates.length; j++) {
                allScores[j] = scorer.score(query.getFingerprint(), rankedCandidates[j].getFingerprint());
            }
            double median = median(allScores);
            double mean = mean(allScores);
            double diffToMedian = allScores[0]-median;
            double diffToMean = allScores[0]-mean;

            for (int j = 0; j < positions.length; j++) {
                int position = positions[j];
                double score = allScores[position];
                if (diffToMean==0){
                    if (Math.abs(score-mean)<1e-12)
                        scores[scoresPos++] = 1;
                    else {
                        throw new IllegalArgumentException("Unexpected mean scores");
//                        scores[scoresPos++] = (score-mean)/(Math.signum(allScores[0]-allScores[allScores.length-1])*0.001);
//                        System.err.println("Unexpected mean scores");
                    }
                } else {
                    scores[scoresPos++] = (score-mean)/diffToMean;
                }

                if (diffToMedian==0){
                    if (Math.abs(score-median)<1e-12)
                        scores[scoresPos++] = 1;
                    else{
                        throw new IllegalArgumentException("Unexpected median scores");
//                        scores[scoresPos++] = (score-median)/(Math.signum(allScores[0]-allScores[allScores.length-1])*0.001);
//                        System.err.println("Unexpected median scores");
                    }
                } else {
                    scores[scoresPos++] = (score-median)/diffToMedian;
                }


                //todo test
                if (Double.isNaN(scores[scoresPos-2]) || Double.isNaN(scores[scoresPos-1])){
                    throw new IllegalArgumentException("NaN");
                }
            }
        }
        return scores;
    }


    private double mean(double[] numbers){
        double sum = 0.0;
        for (double number : numbers) sum += number;
        return sum/numbers.length;
    }


    public double median(double[] numbers){
        double[] numbers2 = numbers.clone();
        Arrays.sort(numbers2);
        return numbers2[numbers2.length/2]; //todo ceil, floor?!?
    }


    @Override
    public int getFeatureSize() {
        return scorers.length*2*positions.length;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        if (rankedCandidates.length==0) return false;
        for (int i = 0; i < scorers.length; i++) {
            FingerblastScoring scorer = scorers[i];
            scorer.prepare(query.getFingerprint());

            double[] allScores = new double[rankedCandidates.length];
            for (int j = 0; j < rankedCandidates.length; j++) {
                allScores[j] = scorer.score(query.getFingerprint(), rankedCandidates[j].getFingerprint());
            }
            double median = median(allScores);
            double mean = mean(allScores);
            double diffToMedian = allScores[0]-median;
            double diffToMean = allScores[0]-mean;

            for (int j = 0; j < positions.length; j++) {
                int position = positions[j];
                double score = allScores[position];
                if (diffToMean==0){
                    if (!(Math.abs(score-mean)<1e-12)){
                        return false;
                    }
                }

                if (diffToMedian==0){
                    if (!(Math.abs(score-median)<1e-12)) {
                      return false;
                    }
                }

            }
        }
        return true;
    }

    @Override
    public String[] getFeatureNames() {
        String[] allNames = new String[names.length*positions.length];
        int pos = 0;
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            for (int j = 0; j < positions.length; j++) {
                int position = positions[j];
                allNames[pos++] = name+"_"+position;
            }
        }
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
