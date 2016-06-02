package de.unijena.bioinf.ChemistryBase.ms.fp;

/**
 * consists of a MASK that determines which bits are used and a list of Fingerprint types that determine which bits are
 * available
 */
public abstract class FingerprintVersion {
    public abstract MolecularProperty getMolecularProperty(int index);
    public abstract int size();
    public abstract boolean compatible(FingerprintVersion fingerprintVersion);

    protected int getRelativeIndexOf(int absoluteIndex) {
        return absoluteIndex;
    }
    protected int getAbsoluteIndexOf(int relativeIndex) {
        return relativeIndex;
    }
}
