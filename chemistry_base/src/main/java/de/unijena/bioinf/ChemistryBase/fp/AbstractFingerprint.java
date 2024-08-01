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

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TShortArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractFingerprint implements Iterable<FPIter> {

    protected final FingerprintVersion fingerprintVersion;

    public AbstractFingerprint(FingerprintVersion fingerprintVersion) {
        if (fingerprintVersion == null) throw new NullPointerException();
        this.fingerprintVersion = fingerprintVersion;
    }

    public Stream<FPIter> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    public abstract Fingerprint asDeterministic();
    public abstract ProbabilityFingerprint asProbabilistic();

    public FingerprintVersion getFingerprintVersion() {
        return fingerprintVersion;
    }

    protected final void enforceCompatibility(AbstractFingerprint other) {
        if (!isCompatible(other)) {
            throw new IllegalArgumentException("fingerprint versions differ: " + fingerprintVersion.toString() + " vs. " + other.fingerprintVersion.toString());
        }
    }

    /**
     * returns true if two fingerprints have the same set of indizes, or, Tanimoto of 1.
     */
    public boolean isSameSet(AbstractFingerprint other) {
        for (FPIter2 x : foreachPair(other)) {
            if (x.isLeftSet() != x.isRightSet())
                return false;
        }
        return cardinality()==other.cardinality();
    }

    public boolean isCompatible(AbstractFingerprint other) {
        return other.fingerprintVersion.compatible(fingerprintVersion);
    }

    public double tanimoto(Fingerprint other) {
        return Tanimoto.tanimoto(this, other);
    }

    public String toCommaSeparatedString() {
        final StringBuilder buffer = new StringBuilder();
        final FPIter iter  = presentFingerprints();
        if (!iter.hasNext()) return "";
        buffer.append(iter.next().getIndex());
        while (iter.hasNext()) {
            buffer.append(',');
            buffer.append(iter.next().getIndex());
        }
        return buffer.toString();
    }

    public abstract String toTabSeparatedString();

    public abstract double[] toProbabilityArray();

    public byte[] toProbabilityArrayBinary() {
        return convertToBinary(toProbabilityArray());
    }

    public static byte[] convertToBinary(short[] data) {
        if (data == null)
            return null;
        final ByteBuffer buffer = ByteBuffer.allocate(data.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (short val : data)
            buffer.putShort(val);
        buffer.rewind();
        return buffer.array();
    }

    public static byte[] convertToBinary(double[] data) {
        if (data == null)
            return null;
        final ByteBuffer buffer = ByteBuffer.allocate(data.length * 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (double val : data)
            buffer.putDouble(val);
        buffer.rewind();
        return buffer.array();
    }

    public static double[] convertToDoubles(byte[] bytes) {
        final TDoubleArrayList data = new TDoubleArrayList(2000);
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        while (buf.position() < buf.limit()) {
            data.add(buf.getDouble());
        }
        return data.toArray();
    }

    public static short[] convertToShorts(byte[] bytes) {
        final TShortArrayList data = new TShortArrayList(2000);
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        while (buf.position() < buf.limit()) {
            data.add(buf.getShort());
        }
        return data.toArray();
    }

    public abstract boolean isSet(int index);

    public abstract int cardinality();

    public abstract FPIter iterator();

    public abstract FPIter presentFingerprints();

    public PredictionPerformance getPerformance(Fingerprint truth) {
        PredictionPerformance.Modify m = new PredictionPerformance().modify();
        for (FPIter2 iter : foreachPair(truth)) {
            m.update(iter.isRightSet(), iter.isLeftSet());
        }
        return m.done();
    }

    public abstract FPIter2 foreachUnion(AbstractFingerprint fp);

    public abstract FPIter2 foreachIntersection(AbstractFingerprint fp);

    public FPIter2 foreachPair(AbstractFingerprint fp) {
        return new GeneralPairIter(iterator(), fp.iterator());
    }

    protected static class GeneralPairIter implements FPIter2 {

        private final FPIter left, right;

        public GeneralPairIter(FPIter left, FPIter right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public FPIter2 clone() {
            return new GeneralPairIter(left.clone(), right.clone());
        }

        @Override
        public double getLeftProbability() {
            return left.getProbability();
        }

        @Override
        public double getRightProbability() {
            return right.getProbability();
        }

        @Override
        public boolean isLeftSet() {
            return left.isSet();
        }

        @Override
        public boolean isRightSet() {
            return right.isSet();
        }

        @Override
        public int getIndex() {
            return left.getIndex();
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return left.getMolecularProperty();
        }

        @Override
        public FPIter2 jumpTo(int index) {
            FPIter l2 = left.jumpTo(index);
            FPIter r2 = right.jumpTo(index);
            if (l2.getIndex() < r2.getIndex()) {
                return new GeneralPairIter(l2, right.jumpTo(l2.getIndex()));
            } else {
                return new GeneralPairIter(l2.jumpTo(r2.getIndex()), r2);
            }
        }

        @Override
        public Iterator<FPIter2> iterator() {
            return clone();
        }

        @Override
        public boolean hasNext() {
            return left.hasNext() && right.hasNext();
        }

        @Override
        public FPIter2 next() {
            left.next();
            right.next();
            return this;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
