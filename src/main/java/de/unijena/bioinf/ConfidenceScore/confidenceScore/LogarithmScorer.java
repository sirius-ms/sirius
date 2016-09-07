package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public class LogarithmScorer implements FeatureCreator {
    private static final double EPSILON = 0.0001;
    private FeatureCreator[] featureCreators;
    private int featureCount;

    public LogarithmScorer(){}

    public LogarithmScorer(FeatureCreator... featureCreators){
        this.featureCreators = featureCreators;
        int count = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            count += featureCreator.getFeatureSize();
        }
        this.featureCount = count;
    }


    @Override
    public void prepare(PredictionPerformance[] statistics) {
        for (FeatureCreator featureCreator : featureCreators) {
            featureCreator.prepare(statistics);
        }
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        double[] scores = new double[getFeatureSize()];
        int pos = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            final double[] currentScores = featureCreator.computeFeatures(query, rankedCandidates);
            final double[] logs = log(currentScores);
            for (int i = 0; i < logs.length; i++) scores[pos++] = logs[i];
        }
        return scores;
    }

    @Override
    public int getFeatureSize() {
        return featureCount;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        for (FeatureCreator featureCreator : featureCreators) {
            if (!featureCreator.isCompatible(query, rankedCandidates)) return false;
        }
        return true;
    }

    @Override
    public int getRequiredCandidateSize() {
        int max = -1;
        for (FeatureCreator featureCreator : featureCreators) max = Math.max(max, featureCreator.getRequiredCandidateSize());
        return max;
    }

    @Override
    public String[] getFeatureNames() {
        String[] names = new String[getFeatureSize()];
        int pos = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            final String[] currentNames = featureCreator.getFeatureNames();
            for (int i = 0; i < currentNames.length; i++) names[pos++] = currentNames[i]+"Log";
        }
        return names;
    }

    private double[] log(double[] scores) {
        double[] logs = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            logs[i] = Math.log(Math.abs(scores[i])+EPSILON);
        }
        return logs;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        List<FeatureCreator> featureCreatorList = new ArrayList<>();
        fillList(featureCreatorList, helper, document, dictionary, "featureCreators");
        this.featureCreators = featureCreatorList.toArray(new FeatureCreator[0]);
        int count = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            count += featureCreator.getFeatureSize();
        }
        this.featureCount = count;
    }

    private <T, G, D, L> void fillList(List<T> list, ParameterHelper helper, DataDocument<G, D, L> document, D dictionary, String keyName) {
        if (!document.hasKeyInDictionary(dictionary, keyName)) return;
        Iterator<G> ls = document.iteratorOfList(document.getListFromDictionary(dictionary, keyName));
        while (ls.hasNext()) {
            final G l = ls.next();
            list.add((T) helper.unwrap(document, l));
        }
    }
    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (FeatureCreator featureCreator : featureCreators) document.addToList(list, helper.wrap(document, featureCreator));
        document.addListToDictionary(dictionary, "featureCreators", list);
    }
}
