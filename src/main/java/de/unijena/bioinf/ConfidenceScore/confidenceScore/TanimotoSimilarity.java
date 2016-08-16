package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;


/**
 * Created by Marcus Ludwig on 30.04.16.
 */
public class TanimotoSimilarity implements FeatureCreator{
    private PredictionPerformance[] statistics;
    private int[] positions;
    private int max;

    public TanimotoSimilarity(){
        this.positions = new int[]{};
        this.max = Integer.MIN_VALUE;
    }

    //todo use also diff between similarities !?
    public TanimotoSimilarity(int... positions){
        this.positions = positions;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < positions.length; i++) max = Math.max(max, positions[i]);
        this.max = max;
    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics = statistics;
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        Fingerprint topHitFp = rankedCandidates[0].getFingerprint();
        double[] scores = new double[positions.length];
        for (int i = 0; i < positions.length; i++) {
            int position = positions[i];
            scores[i] = topHitFp.tanimoto(rankedCandidates[position].getFingerprint());
        }
        return scores;
    }


    @Override
    public int getFeatureSize() {
        return positions.length;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
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
