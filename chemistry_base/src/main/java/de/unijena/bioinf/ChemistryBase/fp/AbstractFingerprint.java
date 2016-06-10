package de.unijena.bioinf.ChemistryBase.fp;

public abstract class AbstractFingerprint implements Iterable<FPIter> {

    protected final FingerprintVersion fingerprintVersion;

    public AbstractFingerprint(FingerprintVersion fingerprintVersion) {
        this.fingerprintVersion = fingerprintVersion;
    }

    public abstract Fingerprint asDeterministic();
    public abstract ProbabilityFingerprint asProbabilistic();

    public FingerprintVersion getFingerprintVersion() {
        return fingerprintVersion;
    }

    public boolean isCompatible(AbstractFingerprint other) {
        return other.fingerprintVersion.compatible(fingerprintVersion);
    }

    public abstract String toTabSeparatedString();

    public abstract double[] toProbabilityArray();

    public abstract boolean isSet(int index);

    public abstract int cardinality();

    public abstract FPIter iterator();

    public abstract FPIter presentFingerprints();


    public abstract FPIter2 foreachUnion(AbstractFingerprint fp);

    public abstract FPIter2 foreachIntersection(AbstractFingerprint fp);

    public abstract FPIter2 foreachPair(AbstractFingerprint fp);


}
