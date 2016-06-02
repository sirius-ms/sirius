package de.unijena.bioinf.ChemistryBase.ms.fp;

import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TShortShortHashMap;

import java.util.BitSet;

public class MaskedFingerprintVersion extends FingerprintVersion{

    private FingerprintVersion innerVersion;
    private BitSet mask;
    private int[] allowedIndizes;
    private TShortShortHashMap mapping;

    public static MaskedFingerprintVersion.Builder buildMaskFor(FingerprintVersion version) {
        if (version instanceof MaskedFingerprintVersion) {
            throw new IllegalArgumentException("Fingerprint is already masked. Use #modify instead!");
        } else {
            return new Builder(version);
        }
    }

    public <T extends AbstractFingerprint> T mask(T fingerprint) {
        if (!innerVersion.compatible(fingerprint.getFingerprintVersion()))
            throw new RuntimeException("Fingerprint is not compatible to mask.");
        if (fingerprint instanceof ProbabilityFingerprint) {
            final double[] ys =((ProbabilityFingerprint) fingerprint).fingerprint;
            final double[] xs = new double[allowedIndizes.length];
            for (int i=0; i < allowedIndizes.length; ++i) xs[i] = ys[allowedIndizes[i]];
            return (T)new ProbabilityFingerprint(this, xs);
        } else if (fingerprint instanceof Fingerprint) {
            if (fingerprint instanceof ArrayFingerprint) {
                int i=0, j=0;
                final TShortArrayList list = new TShortArrayList(Math.min(fingerprint.cardinality(), allowedIndizes.length));
                final short[] indizes = ((ArrayFingerprint) fingerprint).indizes;
                while (i < allowedIndizes.length && j < list.size()) {
                    if (allowedIndizes[i] < indizes[j]) ++i;
                    else if (allowedIndizes[i] > indizes[j]) ++j;
                    else list.add((short)allowedIndizes[i]);
                }
                return (T)new ArrayFingerprint(this, list.toArray());
            } else if (fingerprint instanceof BooleanFingerprint) {
                final boolean[] masked = new boolean[allowedIndizes.length];
                int k=0;
                for (int index : allowedIndizes) masked[k++] = fingerprint.isSet(index);
                return (T) new BooleanFingerprint(this, masked);
            } else {
                throw new RuntimeException("Cannot mask " + fingerprint.getClass());
            }
        } else throw new RuntimeException("Cannot mask " + fingerprint.getClass());
    }

    @Override
    protected int getRelativeIndexOf(int absoluteIndex) {
        return (short)mapping.get((short)absoluteIndex);
    }
    protected int getAbsoluteIndexOf(int relativeIndex) {
        return allowedIndizes[relativeIndex];
    }

    protected MaskedFingerprintVersion(FingerprintVersion innerVersion, BitSet mask) {
        this.innerVersion = innerVersion;
        this.mask = (BitSet) mask.clone();
        this.allowedIndizes = new int[mask.cardinality()];
        int k=0;
        for (int i = mask.nextSetBit(0); i < mask.size(); i = mask.nextSetBit(i+1)) {
            allowedIndizes[k++] = i;
        }
        this.mapping = new TShortShortHashMap(allowedIndizes.length);
        k=0;
        for (int allowedIndex : allowedIndizes) {
            mapping.put((short)allowedIndex, (short)k++);
        }
    }

    public boolean isNotFiltering() {
        return allowedIndizes.length==innerVersion.size();
    }

    public FingerprintVersion getMaskedFingerprintVersion() {
        return innerVersion;
    }

    @Override
    public MolecularProperty getMolecularProperty(int index) {
        return innerVersion.getMolecularProperty(allowedIndizes[index]);
    }

    @Override
    public int size() {
        return allowedIndizes.length;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
        if (this == fingerprintVersion) return true;
        else if (fingerprintVersion.getClass().equals(MaskedFingerprintVersion.class)){
            MaskedFingerprintVersion other = (MaskedFingerprintVersion)fingerprintVersion;
            return innerVersion.compatible(other.innerVersion) && mask.equals(other.mask);
        } else if (isNotFiltering()) {
            return innerVersion.compatible(fingerprintVersion);
        } else return false;
    }

    public Builder modify() {
        return new Builder(innerVersion, mask);
    }

    public static class Builder {
        private final FingerprintVersion version;
        private final BitSet bitSet;
        protected Builder(FingerprintVersion version, BitSet set) {
            this.version = version;
            this.bitSet = (BitSet) set.clone();
        }
        protected Builder(FingerprintVersion version) {
            this.version = version;
            this.bitSet = new BitSet(version.size());
            enableAll();
        }

        public Builder set(int index, boolean value) {
            if (index > version.size()) throw new IndexOutOfBoundsException(index + " is out of fingerprint bond: " + version.size());
            bitSet.set(index, value);
            return this;
        }

        public Builder enableAll() {
            return set(0, version.size(), true);
        }
        public Builder disableAll() {
            return set(0, version.size(), false);
        }

        public Builder set(int from, int to, boolean value) {
            if (from < 0 || to > version.size()) throw new IndexOutOfBoundsException(from + " - " + to  + " is out of fingerprint bond: 0 - " + version.size());
            bitSet.set(from, to, value);
            return this;
        }

        public Builder enable(int index) {
            return set(index,true);
        }
        public Builder enable(int from, int to) {
            return set(from, to, true);
        }

        public Builder disable(int index) {
            return set(index,false);
        }
        public Builder disable(int from, int to) {
            return set(from, to, false);
        }

        public MaskedFingerprintVersion toMask() {
            return new MaskedFingerprintVersion(version, bitSet);
        }

    }
}
