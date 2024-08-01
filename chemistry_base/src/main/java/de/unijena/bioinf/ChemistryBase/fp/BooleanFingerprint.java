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

import java.util.Iterator;
import java.util.stream.Collectors;

public class BooleanFingerprint extends Fingerprint {

    protected final boolean[] fingerprint;

    public BooleanFingerprint(FingerprintVersion version, boolean[] fp) {
        super(version);
        if (fp.length != version.size()) throw new IllegalArgumentException("fp length does not match fingerprint version length: " + fp.length + " vs. " + version.size());
        this.fingerprint = fp.clone();
    }

    @Override
    public ArrayFingerprint asArray() {
        final TShortArrayList list = new TShortArrayList(fingerprint.length/10);
        for (int i=0; i < fingerprint.length; ++i) {
            if (fingerprint[i]) list.add((short)fingerprintVersion.getAbsoluteIndexOf(i));
        }
        return new ArrayFingerprint(fingerprintVersion, list.toArray());
    }

    @Override
    public BooleanFingerprint asBooleans() {
        return this;
    }

    @Override
    public String toOneZeroString() {
        final char[] buf = new char[fingerprintVersion.size()];
        for (int k=0; k < fingerprint.length; ++k) {
            buf[k] = fingerprint[k] ? '1' : '0';
        }
        return new String(buf);
    }

    @Override
    public boolean[] toBooleanArray() {
        return fingerprint.clone();
    }

    @Override
    public short[] toIndizesArray() {
        TShortArrayList indizes = new TShortArrayList(400);
        for (int k=0; k < fingerprint.length; ++k) {
            if (fingerprint[k]) indizes.add((short)fingerprintVersion.getAbsoluteIndexOf(k));
        }
        return indizes.toArray();
    }

    @Override
    public Fingerprint asDeterministic() {
        return this;
    }

    @Override
    public ProbabilityFingerprint asProbabilistic() {
        final double[] values = new double[fingerprint.length];
        for (int i=0; i < fingerprint.length; ++i)
            if (fingerprint[i])
                values[i] = 1d;
        return new ProbabilityFingerprint(fingerprintVersion, values);
    }

    @Override
    public String toTabSeparatedString() {
        return stream().map(FPIter::toString).collect(Collectors.joining("\t"));
    }

    @Override
    public double[] toProbabilityArray() {
        final double[] ary = new double[fingerprintVersion.size()];
        for (int k=0; k < fingerprint.length; ++k) {
            if (fingerprint[k]) ary[k] = 1d;
        }
        return ary;
    }

    @Override
    public boolean isSet(int index) {
        return fingerprint[fingerprintVersion.getRelativeIndexOf(index)];
    }

    @Override
    public int cardinality() {
        int count=0;
        for (boolean val : fingerprint)
            if (val)
                ++count;
        return count;
    }

    @Override
    public FPIter2 foreachUnion(AbstractFingerprint fp) {
        enforceCompatibility(fp);
        if (fp instanceof BooleanFingerprint) return new PairwiseUnionIterator(this, (BooleanFingerprint) fp, -1, -1);
        else throw new IllegalArgumentException("Pairwise iterators are only supported for same type fingerprints;");
        // We cannot express this in javas type system -_- In theory somebody could just implement a pairwise iterator
        // for mixed types
    }

    @Override
    public FPIter2 foreachIntersection(AbstractFingerprint fp) {
        enforceCompatibility(fp);
        if (fp instanceof BooleanFingerprint) return new PairwiseIntersectionIterator(this, (BooleanFingerprint) fp, -1, -1);
        else throw new IllegalArgumentException("Pairwise iterators are only supported for same type fingerprints;");
        // We cannot express this in javas type system -_- In theory somebody could just implement a pairwise iterator
        // for mixed types
    }

    @Override
    public FPIter2 foreachPair(AbstractFingerprint fp) {
        enforceCompatibility(fp);
        if (fp instanceof BooleanFingerprint) return new PairwiseIterator(this, (BooleanFingerprint) fp, -1, -1);
        else if (fp instanceof ProbabilityFingerprint) return new ProbabilityFingerprint.PairwiseBooleanProb(this, (ProbabilityFingerprint)fp, -1);
        else return super.foreachPair(fp);
        // We cannot express this in javas type system -_- In theory somebody could just implement a pairwise iterator
        // for mixed types
    }

    @Override
    public FPIter iterator() {
        return new BIter(-1);
    }

    @Override
    public FPIter presentFingerprints() {
        return new BIterJustOnes(-1);
    }

    protected final class BIterJustOnes extends FPIter {

        int current, next;

        private BIterJustOnes(int current, int next) {
            this.current = current;
            this.next = next;
        }

        public BIterJustOnes(int current) {
            this.current = current;
            this.next = findNext();
        }

        private int findNext() {
            for (int i=current+1; i < fingerprint.length; ++i) {
                if (fingerprint[i]) {
                    return i;
                }
            }
            return fingerprint.length;
        }

        @Override
        public FPIter jumpTo(int index) {
            int r = fingerprintVersion.getClosestRelativeIndexTo(index);
            if (r<0) r = -r - 1;
            --r;
            BIterJustOnes j = new BIterJustOnes(r);
            j.next();
            return j;
        }


        @Override
        public boolean isSet() {
            return fingerprint[current];
        }


        @Override
        public int getIndex() {
            return fingerprintVersion.getAbsoluteIndexOf(current);
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return fingerprintVersion.getMolecularProperty(fingerprintVersion.getAbsoluteIndexOf(current));
        }

        @Override
        public FPIter clone() {
            return new BIterJustOnes(current,next);
        }

        @Override
        public boolean hasNext() {
            return next < fingerprint.length;
        }

        @Override
        public FPIter next() {
            current = next;
            next = findNext();
            return this;
        }
    }

    protected final class BIter extends FPIter {

        private int offset;

        public BIter(int offset) {
            this.offset = offset;
        }

        @Override
        public boolean isSet() {
            return fingerprint[offset];
        }

        @Override
        public int getIndex() {
            return fingerprintVersion.getAbsoluteIndexOf(offset);
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return fingerprintVersion.getMolecularProperty(fingerprintVersion.getAbsoluteIndexOf(offset));
        }

        @Override
        public FPIter jumpTo(int index) {
            int r = fingerprintVersion.getClosestRelativeIndexTo(index);
            if (r<0) r = -r - 1;
            return new BIter(r);
        }

        @Override
        public FPIter clone() {
            return new BIter(offset);
        }

        @Override
        public boolean hasNext() {
            return offset+1 < fingerprint.length;
        }

        public String toString() {
            return isSet() ? "1" : "0";
        }

        @Override
        public FPIter next() {
            ++offset;
            return this;
        }
    }

    protected static class PairwiseIterator implements FPIter2 {
        protected final BooleanFingerprint left, right;
        protected int current,next;

        public PairwiseIterator(BooleanFingerprint left, BooleanFingerprint right, int current, int next) {
            this.left = left;
            this.right = right;
            this.current = current;
            this.next = next;
            if (next < 0) next();
        }

        @Override
        public PairwiseIterator clone() {
            return new PairwiseIterator(left,right,current,next);
        }

        @Override
        public PairwiseIterator next() {
            current=next;
            ++next;
            return this;
        }

        @Override
        public FPIter2 jumpTo(int index) {
            int r = left.fingerprintVersion.getClosestRelativeIndexTo(index);
            if (r < 0) r = -r - 1;
            return new PairwiseIterator(left,right, r, r+1);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return next < left.fingerprint.length;
        }

        @Override
        public double getLeftProbability() {
            return left.fingerprint[current] ? 1 : 0;
        }

        @Override
        public double getRightProbability() {
            return right.fingerprint[current] ? 1 : 0;
        }

        @Override
        public boolean isLeftSet() {
            return left.fingerprint[current];
        }

        @Override
        public boolean isRightSet() {
            return right.fingerprint[current];
        }

        @Override
        public int getIndex() {
            return left.getFingerprintVersion().getAbsoluteIndexOf(current);
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return left.fingerprintVersion.getMolecularProperty(current);
        }

        @Override
        public Iterator<FPIter2> iterator() {
            return clone();
        }
    }

    private final class PairwiseUnionIterator extends PairwiseIterator {

        public PairwiseUnionIterator(BooleanFingerprint left, BooleanFingerprint right, int current, int next) {
            super(left, right, current, next);
        }

        @Override
        public PairwiseUnionIterator next() {
            current=next;
            for (++next; next < left.fingerprint.length; ++next)
                if (left.fingerprint[next] || right.fingerprint[next]) break;
            return this;
        }

        @Override
        public FPIter2 jumpTo(int index) {
            int r = fingerprintVersion.getClosestRelativeIndexTo(index);
            if (r<0) r = -r - 1;
            int current = -1;
            next = -1;
            for (int i=r; i < left.fingerprint.length; ++i) {
                if (left.fingerprint[i] || right.fingerprint[i]) {
                    if (current<0) current = i;
                    else {
                        next = i;
                        break;
                    }
                }
            }
            if (next < 0) next = fingerprint.length;
            return new PairwiseUnionIterator(left,right,current,next);
        }


        public PairwiseUnionIterator clone() {
            return new PairwiseUnionIterator(left,right,current,next);
        }
    }


    private final class PairwiseIntersectionIterator extends PairwiseIterator {
        private PairwiseIntersectionIterator(BooleanFingerprint left, BooleanFingerprint right, int current,int next) {
            super(left,right,current,next);
        }

        @Override
        public FPIter2 jumpTo(int index) {
            int r = fingerprintVersion.getClosestRelativeIndexTo(index);
            if (r<0) r = -r - 1;
            int current = -1;
            int next = -1;
            for (int i=r; i < left.fingerprint.length; ++i) {
                if (left.fingerprint[i] && right.fingerprint[i]) {
                    if (current<0) current = i;
                    else {
                        next = i;
                        break;
                    }
                }
            }
            if (next < 0) next = fingerprint.length;
            return new PairwiseIntersectionIterator(left,right,current,next);
        }

        @Override
        public PairwiseIntersectionIterator next() {
            current=next;
            for (++next; next < left.fingerprint.length; ++next)
                if (left.fingerprint[next] && right.fingerprint[next]) break;
            return this;
        }
        public PairwiseIntersectionIterator clone() {
            return new PairwiseIntersectionIterator(left,right,current,next);
        }
    }

}
