package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.Candidate;
import de.unijena.bioinf.fingerid.FingerprintStatistics;
import de.unijena.bioinf.fingerid.Query;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public class PlattFeatures implements FeatureCreator {
    private double[] quantiles = new double[]{0.05, 0.10, 0.25, 0.50, 0.75, 0.90, 0.95};
    private double[] quantilesAbs = new double[]{0.5, 0.10, 0.25, 0.45};
    private int featureSize;

    private FingerprintStatistics statistics;
    private int[] unbiasedPositions;

    public PlattFeatures() {
        this.featureSize = quantiles.length+quantilesAbs.length+1;
    }

    @Override
    public void prepare(FingerprintStatistics statistics) {
        this.statistics = statistics;
        TIntArrayList unbiased = new TIntArrayList();
        for (int i = 0; i < statistics.numberOfFingerprints; i++) {
            if (!statistics.isBiased(i)) unbiased.add(i);
        }
        this.unbiasedPositions = unbiased.toArray();
    }


    @Override
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates) {
        final double[] scores = new double[featureSize];
        final double[] unbiasedFP = new double[unbiasedPositions.length];
        final double[] platt = query.getPlattScores();
        for (int i = 0; i < unbiasedPositions.length; i++) {
            final int pos = unbiasedPositions[i];
            unbiasedFP[i] = platt[pos];
        }
        Arrays.sort(unbiasedFP);

        for (int i = 0; i < quantiles.length; i++) {
            final double quantile = quantiles[i];
            scores[i] = quantile(unbiasedFP, quantile);
        }

        //deviation from 0.5
        for (int i = 0; i < unbiasedFP.length; i++) unbiasedFP[i] = Math.abs(unbiasedFP[i]-0.5);
        Arrays.sort(unbiasedFP);

        for (int i = 0; i < quantilesAbs.length; i++) {
            final double quantile = quantilesAbs[i];
            scores[quantiles.length+i] = quantile(unbiasedFP, quantile);
        }

        //deviation from middle as platt scores where transformed
        scores[scores.length-1] = getStdDev(unbiasedFP);

        return scores;
    }

    @Override
    public int getFeatureSize() {
        return featureSize;
    }

    @Override
    public boolean isCompatible(Query query, Candidate[] rankedCandidates) {
        return true;
    }

    @Override
    public String[] getFeatureNames() {
        final String[] names = new String[featureSize];
        for (int i = 0; i < quantiles.length; i++) {
            names[i] = "quantile"+quantiles[i];
        }

        for (int i = 0; i < quantilesAbs.length; i++) {
            final double quantile = quantilesAbs[i];
            names[quantiles.length+i] = "quantileAbs"+quantilesAbs[i];
        }

        names[names.length-1] = "devFromMiddle";

        return names;
    }

    //ToDo test
    private double quantile(double[] sortedNumbers, double quantile){
        int n = (int)Math.round((quantile * sortedNumbers.length + 0.5));
        return sortedNumbers[n];
    }

    private double getStdDev(double[] scores){
        double sum = 0.0;
        for (int i = 0; i < scores.length; i++) {
            sum += Math.pow(scores[i], 2);
        }
        return Math.sqrt(sum/(scores.length));
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L quantilesList = document.getListFromDictionary(dictionary, "quantiles");
        int size = document.sizeOfList(quantilesList);
        double[] quantiles = new double[size];
        for (int i = 0; i < size; i++) quantiles[i] = document.getDoubleFromList(quantilesList, i);
        this.quantiles = quantiles;
        L quantilesAbsList = document.getListFromDictionary(dictionary, "quantilesAbs");
        size = document.sizeOfList(quantilesAbsList);
        double[] quantilesAbs = new double[size];
        for (int i = 0; i < size; i++) quantilesAbs[i] = document.getDoubleFromList(quantilesAbsList, i);
        this.quantilesAbs = quantilesAbs;
        this.featureSize = quantiles.length+quantilesAbs.length+1;
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (double d : quantiles) document.addToList(list, d);
        document.addListToDictionary(dictionary, "quantiles", list);
        list = document.newList();
        for (double d : quantilesAbs) document.addToList(list, d);
        document.addListToDictionary(dictionary, "quantilesAbs", list);
    }
}


