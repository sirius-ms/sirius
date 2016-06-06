package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.*;

import java.util.Arrays;

/**
 * Created by Marcus Ludwig on 29.04.16.
 */
public class DifferentiatingMolecularPropertiesCounter implements FeatureCreator {
    private FingerprintStatistics statistics;
    private double alpha;
    private int numOfWorseCandidates;

    /**
     *
     * @param alpha 0-1: Proportion of candidates which have to differ in this mol prob from the top hit to count
     * @param numOfWorseCandidates number of candidates to compare against
     */
    public DifferentiatingMolecularPropertiesCounter(double alpha, int numOfWorseCandidates){
        this.alpha = alpha;
        this.numOfWorseCandidates = numOfWorseCandidates;
    }

    @Override
    public void prepare(FingerprintStatistics statistics) {
        this.statistics = statistics;
    }

    @Override
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates) {
        int numOfCandidates = (this.numOfWorseCandidates<0 ? rankedCandidates.length : this.numOfWorseCandidates+1);
        Candidate topHit = rankedCandidates[0];
        double[] haveProperty = new double[topHit.fingerprint.length];
        //todo numerically unstable?!
        for (int i = 1; i < numOfCandidates; i++) {
            boolean[] fp  = rankedCandidates[i].fingerprint;
            for (int j = 0; j < fp.length; j++) {
                if (fp[j]) ++haveProperty[j];
            }

        }

        for (int i = 0; i < haveProperty.length; i++) {
            haveProperty[i] = haveProperty[i]/(rankedCandidates.length-1);
        }

        int diffMolPropCounter = 0;
        boolean[] fp = topHit.fingerprint;
        for (int i = 0; i < fp.length; i++) {
            //ToDo just use molecular properties used for scoring?!?!?
            if (statistics.isBiased(i)) continue;
            if (fp[i]){
                if ((1-haveProperty[i])>=alpha) diffMolPropCounter++;
            } else {
                if (haveProperty[i]>=alpha) diffMolPropCounter++;
            }

        }
        return new double[]{diffMolPropCounter};
    }


    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(Query query, Candidate[] rankedCandidates) {
        return rankedCandidates.length>1;
    }

    @Override
    public String[] getFeatureNames() {
        return new String[]{"diffMolProps_vs_"+numOfWorseCandidates};
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //Nothing to do as long as ScoringMethods stay the same
        numOfWorseCandidates = (int)document.getIntFromDictionary(dictionary, "numOfWorseCandidates");
        alpha = document.getDoubleFromDictionary(dictionary, "alpha");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //Nothing to do as long as ScoringMethods stay the same
        document.addToDictionary(dictionary, "numOfWorseCandidates", numOfWorseCandidates);
        document.addToDictionary(dictionary, "alpha", alpha);
    }
}
