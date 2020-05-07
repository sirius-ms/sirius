package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

/**
 * Encapsulates the result of the CSI:FingerID prediction step
 */
public class FingerprintResult implements ResultAnnotation  {

    public final ProbabilityFingerprint fingerprint;

    public FingerprintResult(ProbabilityFingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }
}
