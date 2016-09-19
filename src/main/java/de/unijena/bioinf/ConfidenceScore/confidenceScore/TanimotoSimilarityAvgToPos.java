package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

import java.util.Arrays;


/**
 * Created by Marcus Ludwig on 30.04.16.
 */
public class TanimotoSimilarityAvgToPos implements FeatureCreator{
    private PredictionPerformance[] statistics;
    private int[] positions;

    public TanimotoSimilarityAvgToPos(){
        this.positions = new int[0];
    }

    public TanimotoSimilarityAvgToPos(int... followingPositions){
        this.positions = followingPositions;
        Arrays.sort(this.positions);
    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics = statistics;
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        Fingerprint topHitFp = rankedCandidates[0].getFingerprint();
        double sum = 0;
        int pPos = 0;
        double[] sim = new double[positions.length];
        for (int i = 1; i < rankedCandidates.length; i++) {
            CompoundWithAbstractFP<Fingerprint> rankedCandidate = rankedCandidates[i];
            sum += topHitFp.tanimoto(rankedCandidate.getFingerprint());
            while (pPos<positions.length && positions[pPos]<=i) {
                sim[pPos] = sum/i;
                pPos++;
            }
        }
        return sim;
    }


    @Override
    public int getFeatureSize() {
        return positions.length;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return (rankedCandidates.length>1);
    }

    @Override
    public int getRequiredCandidateSize() {
        return 2;
    }

    @Override
    public String[] getFeatureNames() {
        String[] names = new String[getFeatureSize()];
        for (int j = 0; j < positions.length; j++) {
            int p = positions[j];
            names[j] = "TanimotoSimilarityAvgToPos"+p;
        }
        return names;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> document, D dictionary) {
        L percentList = document.getListFromDictionary(dictionary, "positions");
        int size = document.sizeOfList(percentList);
        int[] perc = new int[size];
        for (int i = 0; i < size; i++) perc[i] = (int)document.getIntFromList(percentList, i);

        this.positions = perc;
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (int position : positions) document.addToList(list, position);
        document.addListToDictionary(dictionary, "positions", list);
    }
}
