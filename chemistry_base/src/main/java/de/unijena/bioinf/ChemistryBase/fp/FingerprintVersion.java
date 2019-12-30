package de.unijena.bioinf.ChemistryBase.fp;

/**
 * consists of a MASK that determines which bits are used and a list of Fingerprint types that determine which bits are
 * available
 */
public abstract class FingerprintVersion {
    public abstract MolecularProperty getMolecularProperty(int index);
    public abstract int size();
    public abstract boolean compatible(FingerprintVersion fingerprintVersion);

    public int getRelativeIndexOf(int absoluteIndex) {
        return absoluteIndex;
    }
    public int getAbsoluteIndexOf(int relativeIndex) {
        return relativeIndex;
    }
    public boolean hasProperty(int absoluteIndex) {
        return absoluteIndex < size();
    }

    /**
     * returns the index of the molecular property either with the given absolute index or -(insertionPoint - 1) where
     * insertion point is same as in Arrays.binarysearch.
     */
    protected int getClosestRelativeIndexTo(int absoluteIndex) {
        return absoluteIndex;
    }
}
