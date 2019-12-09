package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

import java.util.List;

public class FingerIdResult implements Annotated<ResultAnnotation> {
    protected final Annotations<ResultAnnotation> annotations;
    public final FTree sourceTree;

    @Override
    public Annotations<ResultAnnotation> annotations() {
        return annotations;
    }

    public FingerIdResult(FTree sourceTree) {
        this.annotations = new Annotations<>();
        this.sourceTree = sourceTree;
    }

    public ConfidenceScore getConfidence() {
        return getAnnotation(ConfidenceResult.class).orElse(ConfidenceResult.NaN).score;
    }

    public ProbabilityFingerprint getPredictedFingerprint() {
        return getAnnotation(FingerprintResult.class).map(r -> r.fingerprint).orElse(null);
    }

    public List<Scored<CompoundCandidate>> getCandidates() {
        return getAnnotation(FingerblastResult.class).map(r -> r.getResults()).orElse(null);
    }
}
