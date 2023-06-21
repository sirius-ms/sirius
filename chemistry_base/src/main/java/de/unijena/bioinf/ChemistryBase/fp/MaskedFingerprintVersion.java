/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.fp;

import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TShortShortHashMap;

import java.util.Arrays;
import java.util.BitSet;

public class MaskedFingerprintVersion extends FingerprintVersion{

    private FingerprintVersion innerVersion;
    private BitSet mask;
    private int[] allowedIndizes;
    private TShortShortHashMap mapping;

    public static MaskedFingerprintVersion fromString(String s) {
        MaskedFingerprintVersion.Builder b = buildMaskFor(CdkFingerprintVersion.getDefault());
        int k=0;
        for (String token : s.split("\t")) {
            b.set(k++, token.equalsIgnoreCase("x"));
        }
        return b.toMask();
    }

    public static MaskedFingerprintVersion.Builder buildMaskFor(FingerprintVersion version) {
        if (version instanceof MaskedFingerprintVersion) {
            throw new IllegalArgumentException("Fingerprint is already masked. Use #modify instead!");
        } else {
            return new Builder(version);
        }
    }

    public static MaskedFingerprintVersion allowAll(FingerprintVersion v) {
        Builder b = buildMaskFor(v);
        b.enableAll();
        return b.toMask();
    }

    public int[] allowedIndizes() {
        return allowedIndizes.clone();
    }

    public ArrayFingerprint mask(short[] values) {
        return mask(new ArrayFingerprint(innerVersion, values));
    }
    public BooleanFingerprint mask(boolean[] values) {
        return mask(new BooleanFingerprint(innerVersion, values));
    }
    public ProbabilityFingerprint mask(double[] values) {
        return mask(new ProbabilityFingerprint(innerVersion, values));
    }

    public <T extends AbstractFingerprint> T mask(T fingerprint) {
        if (fingerprint.fingerprintVersion instanceof MaskedFingerprintVersion) {
            if (fingerprint.fingerprintVersion == this)
                return fingerprint;
            if (!compatible(fingerprint.fingerprintVersion)) {
                throw new RuntimeException("Fingerprint is already masked by a fingerprint mask which is not compatible to this mask: " + toString() +  " vs " + fingerprint.fingerprintVersion.toString());
            }

            fingerprint = ((MaskedFingerprintVersion) fingerprint.fingerprintVersion).unmask(fingerprint);


        } else if (!innerVersion.compatible(fingerprint.getFingerprintVersion())) {
            throw new RuntimeException("Fingerprint is not compatible to mask. Given fingerprint is version " + fingerprint.getFingerprintVersion().toString() + ", mask is version " + innerVersion.toString());
        }
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
                while (i < allowedIndizes.length && j < indizes.length) {
                    if (allowedIndizes[i] < indizes[j]) ++i;
                    else if (allowedIndizes[i] > indizes[j]) ++j;
                    else {
                        list.add((short)allowedIndizes[i]);
                        ++i; ++j;
                    }
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

    /**
     * returns a new fingerprint without masking. All masked bits are set to false. In other words: the fingerprint
     * has the same set of molecular properties, just without the masking.
     */
    protected Fingerprint unmask(Fingerprint fp) {
        if (fp instanceof ArrayFingerprint) {
            return new ArrayFingerprint(getMaskedFingerprintVersion(), ((ArrayFingerprint) fp).indizes);
        } else {
            return new ArrayFingerprint(getMaskedFingerprintVersion(), fp.toIndizesArray());
        }
    }
    protected <T extends AbstractFingerprint> T unmask(T fp) {
        if (fp instanceof ProbabilityFingerprint) return (T)unmask((ProbabilityFingerprint)fp);
        else return (T)unmask((Fingerprint)fp);
    }
    /**
     * returns a new fingerprint without masking. All masked bits are set to 0%. In other words: the fingerprint
     * has the same set of molecular properties, just without the masking.
     */
    protected ProbabilityFingerprint unmask(ProbabilityFingerprint fp) {
        final double[] complete = new double[getMaskedFingerprintVersion().size()];
        for (FPIter f : fp) {
            complete[f.getIndex()] = f.getProbability();
        }
        return new ProbabilityFingerprint(getMaskedFingerprintVersion(), complete);
    }

    public MaskedFingerprintVersion getIntersection(MaskedFingerprintVersion other) {
        if (!innerVersion.compatible(other.innerVersion)) throw new RuntimeException("Fingerprint is not compatible to mask. Given fingerprint is version " + other.innerVersion.toString() + ", mask is version " + innerVersion.toString());
        final BitSet intersection = (BitSet) mask.clone();
        intersection.and(other.mask);
        return new MaskedFingerprintVersion(innerVersion, intersection);
    }
    public MaskedFingerprintVersion getUnion(MaskedFingerprintVersion other) {
        if (!innerVersion.compatible(other.innerVersion)) throw new RuntimeException("Fingerprint is not compatible to mask. Given fingerprint is version " + other.innerVersion.toString() + ", mask is version " + innerVersion.toString());
        final BitSet intersection = (BitSet) mask.clone();
        intersection.or(other.mask);
        return new MaskedFingerprintVersion(innerVersion, intersection);
    }

    @Override
    public int getRelativeIndexOf(int absoluteIndex) {
        return mapping.get((short)absoluteIndex);
    }
    public int getAbsoluteIndexOf(int relativeIndex) {
        return allowedIndizes[relativeIndex];
    }

    @Override
    protected int getClosestRelativeIndexTo(int absoluteIndex) {
        return Arrays.binarySearch(allowedIndizes, absoluteIndex);
    }

    @Override
    public boolean hasProperty(int absoluteIndex) {
        return mapping.containsKey((short)absoluteIndex);
    }

    protected MaskedFingerprintVersion(FingerprintVersion innerVersion, BitSet mask) {
        this.innerVersion = innerVersion;
        this.mask = (BitSet) mask.clone();
        this.allowedIndizes = new int[mask.cardinality()];
        int k=0;
        for (int i = mask.nextSetBit(0); i >= 0 && i < mask.size(); i = mask.nextSetBit(i+1)) {
            allowedIndizes[k++] = i;
        }
        this.mapping = new TShortShortHashMap(allowedIndizes.length, 0.75f, (short)-1, (short)-1);
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
    public MolecularProperty getMolecularProperty(int absoluteIndex) {
        return innerVersion.getMolecularProperty(absoluteIndex);
    }

    @Override
    public String toString() {
        return "Masked fingerprint version: " + allowedIndizes.length + " bits in use. Derived from " + innerVersion.toString();
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
            return innerVersion.compatible(other.innerVersion) && mask.intersects(other.mask);
        } else if (isNotFiltering()) {
            return innerVersion.compatible(fingerprintVersion);
        } else return false;
    }

    @Override
    public boolean identical(FingerprintVersion fingerprintVersion) {
        if (this == fingerprintVersion) return true;
        else if (fingerprintVersion.getClass().equals(MaskedFingerprintVersion.class)){
            MaskedFingerprintVersion other = (MaskedFingerprintVersion)fingerprintVersion;
            return innerVersion.identical(other.innerVersion) && mask.equals(other.mask);
        } else if (isNotFiltering()) {
            return innerVersion.identical(fingerprintVersion);
        } else return false;
    }

    public MaskedFingerprintVersion invert() {
        final BitSet copy = (BitSet) mask.clone();
        copy.flip(0, copy.size());
        return new MaskedFingerprintVersion(innerVersion, copy);
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

        public Builder invert() {
            this.bitSet.flip(0, bitSet.size());
            return this;
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
