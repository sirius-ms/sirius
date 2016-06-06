package de.unijena.bioinf.ChemistryBase.fp;

public abstract class Fingerprint extends AbstractFingerprint {

    public Fingerprint(FingerprintVersion fingerprintVersion) {
        super(fingerprintVersion);
    }

    public abstract ArrayFingerprint asArray();
    public abstract BooleanFingerprint asBooleans();

    public abstract double tanimoto(Fingerprint other);


}
