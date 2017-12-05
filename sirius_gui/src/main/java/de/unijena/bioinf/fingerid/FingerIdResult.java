package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.FingerprintCandidate;

import java.util.List;

public class FingerIdResult {

    // ADDITIONAL OUTPUT OPTIONS
    public static final String CANDIDATE_LISTS = "fingerid.candidates";

    protected List<Scored<FingerprintCandidate>> candidates;
    protected double confidence;
    protected ProbabilityFingerprint predictedFingerprint;

    public FingerIdResult(List<Scored<FingerprintCandidate>> candidates, double confidence, ProbabilityFingerprint predictedFingerprint) {
        this.candidates = candidates;
        this.confidence = confidence;
        this.predictedFingerprint = predictedFingerprint;
    }

    public List<Scored<FingerprintCandidate>> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Scored<FingerprintCandidate>> candidates) {
        this.candidates = candidates;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public ProbabilityFingerprint getPredictedFingerprint() {
        return predictedFingerprint;
    }

    public void setPredictedFingerprint(ProbabilityFingerprint predictedFingerprint) {
        this.predictedFingerprint = predictedFingerprint;
    }


}
