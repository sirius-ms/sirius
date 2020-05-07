package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

public class CanopusResult implements DataAnnotation {
    protected ProbabilityFingerprint canopusFingerprint;

    public ProbabilityFingerprint getCanopusFingerprint() {
        return canopusFingerprint;
    }

    public CanopusResult(ProbabilityFingerprint canopusFingerprint) {
        this.canopusFingerprint = canopusFingerprint;
    }
}
