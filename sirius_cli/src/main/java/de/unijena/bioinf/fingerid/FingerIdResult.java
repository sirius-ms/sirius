package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.annotations.Annotaion;
import de.unijena.bioinf.ms.annotations.Annotated;

import java.util.List;

public class FingerIdResult implements Annotated {

    // ADDITIONAL OUTPUT OPTIONS
    public static final String CANDIDATE_LISTS = "fingerid.candidates";

    protected List<Scored<FingerprintCandidate>> candidates;
    protected double confidence;
    protected ProbabilityFingerprint predictedFingerprint;
    protected FTree resolvedTree;
    protected Annotations annotations;

    public FingerIdResult(List<Scored<FingerprintCandidate>> candidates, double confidence, ProbabilityFingerprint predictedFingerprint, FTree resolvedTree) {
        this.candidates = candidates;
        this.confidence = confidence;
        this.predictedFingerprint = predictedFingerprint;
        this.resolvedTree = resolvedTree;
        this.annotations = new Annotations();
    }

    public PrecursorIonType getPrecursorIonType() {
        return resolvedTree.getAnnotationOrThrow(PrecursorIonType.class);
    }

    public FTree getResolvedTree() {
        return resolvedTree;
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

    @Override
    public Annotations<Annotaion> annotations() {
        return annotations;
    }
}
