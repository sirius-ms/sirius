package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

public class EvaluationInstance {

    protected String name;
    protected ProbabilityFingerprint fingerprint;
    protected LabeledCompound compound;

    public EvaluationInstance(String name, ProbabilityFingerprint fingerprint, LabeledCompound compound) {
        this.compound = compound;
        this.fingerprint = fingerprint;
        this.name = name;
    }
}
