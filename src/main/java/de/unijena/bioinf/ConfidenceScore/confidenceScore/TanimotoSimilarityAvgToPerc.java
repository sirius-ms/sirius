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
public class TanimotoSimilarityAvgToPerc implements FeatureCreator{
    private PredictionPerformance[] statistics;
    private int[] percent;

    public TanimotoSimilarityAvgToPerc(){
        this.percent = new int[]{};
    }

    //todo use also diff between similarities !?
    public TanimotoSimilarityAvgToPerc(int... percent){
        this.percent = percent;
        Arrays.sort(this.percent);
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
        double[] sim = new double[percent.length];
        for (int i = 1; i < rankedCandidates.length; i++) {
            CompoundWithAbstractFP<Fingerprint> rankedCandidate = rankedCandidates[i];
            sum += topHitFp.tanimoto(rankedCandidate.getFingerprint());
            while (pPos<percent.length && (percent[pPos]/100.0)<=(1.0*i/rankedCandidates.length)) {
                sim[pPos] = sum/i;
                pPos++;
            }
        }
        return sim;
    }


    @Override
    public int getFeatureSize() {
        return percent.length;
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
        for (int j = 0; j < percent.length; j++) {
            int perc = percent[j];
            names[j] = "TanimotoSimilarityAvgToPerc"+perc;
        }
        return names;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> document, D dictionary) {
        L percentList = document.getListFromDictionary(dictionary, "percent");
        int size = document.sizeOfList(percentList);
        int[] perc = new int[size];
        for (int i = 0; i < size; i++) perc[i] = (int)document.getIntFromList(percentList, i);

        this.percent = perc;
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (int position : percent) document.addToList(list, position);
        document.addListToDictionary(dictionary, "percent", list);
    }
}
