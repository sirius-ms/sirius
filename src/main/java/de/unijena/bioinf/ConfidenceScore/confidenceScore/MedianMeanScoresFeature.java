package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;

import java.util.Arrays;

/**
 * Created by Marcus Ludwig on 29.04.16.
 */
public class MedianMeanScoresFeature implements FeatureCreator {
    private final String name;
    private FingerblastScoring scorer;
    private PredictionPerformance[] statistics;

    public MedianMeanScoresFeature(){
        //todo do for all scorings?
        name = "CSIFingerIdScoring";
    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics = statistics;
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        scorer = new CSIFingerIdScoring(statistics);

        scorer.prepare(query.getFingerprint());
        double[] scores = new double[rankedCandidates.length];
        for (int i = 0; i < rankedCandidates.length; i++) {
            scores[i] = scorer.score(query.getFingerprint(), rankedCandidates[i].getFingerprint());
        };
        return new double[]{mean(scores), median(scores)};
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
        return 2;
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
        return new String[]{"mean_"+name, "median_"+name};
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
