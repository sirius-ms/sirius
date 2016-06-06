package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.Candidate;
import de.unijena.bioinf.fingerid.FingerprintStatistics;
import de.unijena.bioinf.fingerid.Query;

/**
 * Created by Marcus Ludwig on 30.04.16.
 */
public class TanimotoSimilarity implements FeatureCreator{
    private FingerprintStatistics statistics;
    private int[] positions;
    private int max;

    //todo use also diff between similarities !?
    public TanimotoSimilarity(int... positions){
        this.positions = positions;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < positions.length; i++) max = Math.max(max, positions[i]);
        this.max = max;
    }

    @Override
    public void prepare(FingerprintStatistics statistics) {
        this.statistics = statistics;
    }

    @Override
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates) {
        boolean[] topHitFp = rankedCandidates[0].fingerprint;
        double[] scores = new double[positions.length];
        for (int i = 0; i < positions.length; i++) {
            int position = positions[i];
            scores[i] = tanimoto(topHitFp, rankedCandidates[position].fingerprint);
        }
        return scores;
    }

    private double tanimoto(boolean[] fp1, boolean[] fp2){
        int samePos = 0;
//        int sameNeg = 0;
        int diff = 0;
        for (int i = 0; i < fp1.length; i++) {
            if (statistics.isBiased(i)) continue;
            //todo just mol probs used for scoring?
            if (fp1[i]){
                if (fp2[i]){
                    samePos++;
                } else {
                    diff++;
                }
            } else {
                if (fp2[i]){
                    diff++;
                } else {
//                    sameNeg++;
                }
            }
        }
        return 1.0*samePos/(samePos+diff);
    }

    @Override
    public int getFeatureSize() {
        return positions.length;
    }

    @Override
    public boolean isCompatible(Query query, Candidate[] rankedCandidates) {
        return (rankedCandidates.length>max);
    }

    @Override
    public String[] getFeatureNames() {
        String[] names = new String[getFeatureSize()];
        for (int j = 0; j < positions.length; j++) {
            int position = positions[j];
            names[j] = "TanimotoDiffTo"+position;
        }
        return names;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> document, D dictionary) {
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
    public <G, D, L> void exportParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (int position : positions) document.addToList(list, position);
        document.addListToDictionary(dictionary, "positions", list);
    }
}
