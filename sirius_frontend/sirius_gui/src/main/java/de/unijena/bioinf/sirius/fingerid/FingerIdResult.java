package de.unijena.bioinf.sirius.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.CompoundCandidate;

import java.util.List;

public class FingerIdResult {

    protected List<Scored<CompoundCandidate>> candidates;
    protected double confidence;
    protected ProbabilityFingerprint predictedFingerprint;

    public FingerIdResult(List<Scored<CompoundCandidate>> candidates, double confidence, ProbabilityFingerprint predictedFingerprint) {
        this.candidates = candidates;
        this.confidence = confidence;
        this.predictedFingerprint = predictedFingerprint;
    }

    public List<Scored<CompoundCandidate>> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Scored<CompoundCandidate>> candidates) {
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
