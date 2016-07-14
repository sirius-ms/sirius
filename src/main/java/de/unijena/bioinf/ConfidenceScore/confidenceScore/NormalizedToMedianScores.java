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
public class NormalizedToMedianScores implements FeatureCreator {
    private final String[] names;
    private final FingerblastScoring[] scorers;

    public NormalizedToMedianScores(){
        names = new String[]{"CSIFingerIdScoring", "SimpleMaximumLikelihoodScoring", "ProbabilityEstimateScoring"};
        scorers = new FingerblastScoring[3];
    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        scorers[0] = new CSIFingerIdScoring(statistics);
        scorers[1] = new SimpleMaximumLikelihoodScoring(statistics);
        scorers[2] = new ProbabilityEstimateScoring(statistics);
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        final double[] scores = new double[scorers.length];
        for (int i = 0; i < scorers.length; i++) {
            FingerblastScoring scorer = scorers[i];
            scorer.prepare(query.getFingerprint());

            double[] allScores = new double[rankedCandidates.length];
            for (int j = 0; j < rankedCandidates.length; j++) {
                allScores[j] = scorer.score(query.getFingerprint(), rankedCandidates[j].getFingerprint());
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
        return scorers.length;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
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
