package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class FBCandidateFingerprints implements ResultAnnotation {
    @NotNull
    protected final List<Fingerprint> fingerprints;

    public FBCandidateFingerprints(@NotNull List<Fingerprint> fingerprints) {
        this.fingerprints = fingerprints;
    }

    public List<Fingerprint> getFingerprints() {
        return Collections.unmodifiableList(fingerprints);
    }
}
