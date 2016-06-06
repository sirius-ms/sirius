package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.Candidate;
import de.unijena.bioinf.fingerid.FingerprintStatistics;
import de.unijena.bioinf.fingerid.Query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public class CombinedFeatureCreator implements FeatureCreator{
    FeatureCreator[] featureCreators;
    private int featureCount;

    public CombinedFeatureCreator(){}

    public CombinedFeatureCreator(FeatureCreator... featureCreators){
        this.featureCreators = featureCreators;
        int count = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            count += featureCreator.getFeatureSize();
        }
        this.featureCount = count;
    }


    @Override
    public void prepare(FingerprintStatistics statistics) {
        for (FeatureCreator featureCreator : featureCreators) {
            featureCreator.prepare(statistics);
        }
    }

    @Override
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates) {
        double[] scores = new double[getFeatureSize()];
        int pos = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            final double[] currentScores = featureCreator.computeFeatures(query, rankedCandidates);
            for (int i = 0; i < currentScores.length; i++) scores[pos++] = currentScores[i];
        }
        return scores;
    }

    @Override
    public int getFeatureSize() {
        return featureCount;
    }

    @Override
    public boolean isCompatible(Query query, Candidate[] rankedCandidates) {
        for (FeatureCreator featureCreator : featureCreators) {
            if (!featureCreator.isCompatible(query, rankedCandidates)) return false;
        }
        return true;
    }

    @Override
    public String[] getFeatureNames() {
        String[] names = new String[getFeatureSize()];
        int pos = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            final String[] currentNames = featureCreator.getFeatureNames();
            for (int i = 0; i < currentNames.length; i++) names[pos++] = currentNames[i];
        }
        return names;
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
