package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

import java.io.IOException;

public class BayesnetScoringWithDynamicComputation implements FingerblastScoringMFSpecific {

    BayesianNetworkScoringProvider scoringProvider;

    MolecularFormula currentMF = null;
    FingerblastScoring currentScoring = null;
    ProbabilityFingerprint currentEstimatedFingerprint = null;

    //todo used??!?!
    private double threshold = 0.25, minSamples=25;

    public BayesnetScoringWithDynamicComputation(BayesianNetworkScoringProvider scoringProvider) {
        this.scoringProvider = scoringProvider;

    }

    @Override
    public void prepare(ProbabilityFingerprint fingerprint, MolecularFormula formula) {
        currentMF = formula;
        currentEstimatedFingerprint = fingerprint;

        try {
            BayesnetScoring bayesnetScoring = scoringProvider.getScoringOrDefault(formula);
            currentScoring = bayesnetScoring.getScoring();
            currentScoring.prepare(currentEstimatedFingerprint);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void prepare(ProbabilityFingerprint fingerprint) {
        //todo change for all scoring?
        throw new RuntimeException("need to provide molecular formula");
    }

    @Override
    public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
        if (!fingerprint.equals(currentEstimatedFingerprint)) throw new RuntimeException("estimated fingerprint has changed. New scoring needs to be prepared.");
        return currentScoring.score(fingerprint, databaseEntry);
    }

    @Override
    public double getThreshold() {
        return threshold;
    }

    @Override
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public double getMinSamples() {
        return minSamples;
    }

    @Override
    public void setMinSamples(double minSamples) {
        this.minSamples = minSamples;
    }


}
