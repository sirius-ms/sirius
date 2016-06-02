package de.unijena.bioinf.ChemistryBase.ms.fp;

import java.util.Arrays;
import java.util.Iterator;

public class ArrayFingerprint extends Fingerprint {

    protected final short[] indizes;

    public ArrayFingerprint(FingerprintVersion fingerprintVersion, short[] indizes) {
        super(fingerprintVersion);
        this.indizes = indizes;
    }

    @Override
    public ArrayFingerprint asArray() {
        return this;
    }

    @Override
    public BooleanFingerprint asBooleans() {
        final boolean[] values = new boolean[fingerprintVersion.size()];
        for (int index : indizes) values[fingerprintVersion.getRelativeIndexOf(index)] = true;
        return new BooleanFingerprint(fingerprintVersion, values);
    }

    @Override
    public double tanimoto(Fingerprint other) {
        return 0;
    }

    @Override
    public Fingerprint asDeterministic() {
        return this;
    }

    @Override
    public ProbabilityFingerprint asProbabilistic() {
        final double[] values = new double[fingerprintVersion.size()];
        for (int index : indizes) values[fingerprintVersion.getRelativeIndexOf(index)] = 1d;
        return new ProbabilityFingerprint(fingerprintVersion, values);
    }

    @Override
    public boolean isSet(int index) {
        return Arrays.binarySearch(indizes, (short)index)>=0;
    }

    @Override
    public int cardinality() {
        return indizes.length;
    }

    @Override
    public FPIter iterator() {
        return new ArrayIterator(0,-1);
    }

    @Override
    public FPIter presentFingerprints() {
        return new OnlySetIterator(-1);
    }

    @Override
    public FPIter2 foreachUnion(AbstractFingerprint fp) {
        if (fp instanceof  ArrayFingerprint)
            return new PairwiseUnionIterator(this, (ArrayFingerprint)fp, -1,0,0);
        else throw new IllegalArgumentException("Pairwise iterators are only supported for same type fingerprints;");
        // We cannot express this in javas type system -_- In theory somebody could just implement a pairwise iterator
        // for mixed types
    }

    @Override
    public FPIter2 foreachIntersection(AbstractFingerprint fp) {
        if (fp instanceof  ArrayFingerprint)
            return new PairwiseIntersectionIterator(this, (ArrayFingerprint)fp, -1,0,0);
        else throw new IllegalArgumentException("Pairwise iterators are only supported for same type fingerprints;");
        // We cannot express this in javas type system -_- In theory somebody could just implement a pairwise iterator
        // for mixed types
    }

    @Override
    public FPIter2 foreachPair(AbstractFingerprint fp) {
        if (fp instanceof  ArrayFingerprint)
            return new PairwiseIterator(this, (ArrayFingerprint)fp, -1,0,0);
        else throw new IllegalArgumentException("Pairwise iterators are only supported for same type fingerprints;");
        // We cannot express this in javas type system -_- In theory somebody could just implement a pairwise iterator
        // for mixed types
    }

    private final class OnlySetIterator extends FPIter {

        private int offset;

        public OnlySetIterator(int offset) {
            this.offset = offset;
        }

        @Override
        public boolean isSet() {
            return true;
        }

        @Override
        public int getIndex() {
            return indizes[offset];
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return fingerprintVersion.getMolecularProperty(indizes[offset]);
        }

        @Override
        public FPIter clone() {
            return new OnlySetIterator(offset);
        }

        @Override
        public boolean hasNext() {
            return offset+1 < indizes.length;
        }

        @Override
        public FPIter next() {
            ++offset;
            return this;
        }
    }

    private final class ArrayIterator extends FPIter {

        private int offset;
        private int absolute;

        public ArrayIterator(int offset, int absolute) {
            this.offset = offset;
            this.absolute = absolute;
        }

        @Override
        public boolean isSet() {
            return offset < indizes.length && absolute==indizes[offset];
        }

        @Override
        public int getIndex() {
            return absolute;
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return fingerprintVersion.getMolecularProperty(absolute);
        }

        @Override
        public FPIter clone() {
            return new ArrayIterator(offset,absolute);
        }

        @Override
        public boolean hasNext() {
            return absolute < fingerprintVersion.size();
        }

        @Override
        public FPIter next() {
            ++absolute;
            if (offset < indizes.length && absolute > indizes[offset]) ++offset;
            return this;
        }
    }

    private static class PairwiseIterator implements FPIter2 {
        protected final ArrayFingerprint left,right;
        protected int l, r, c;

        public PairwiseIterator(ArrayFingerprint left, ArrayFingerprint right, int c, int l, int r) {
            this.left = left;
            this.right = right;
            this.c = c;
            this.l = l;
            this.r = r;
        }

        @Override
        public FPIter2 clone() {
            return new PairwiseIterator(left,right,c,l,r);
        }

        @Override
        public FPIter2 next() {
            ++c;
            if (c > left.indizes[l]) ++l;
            if (c > right.indizes[r]) ++r;
            return this;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return c < left.fingerprintVersion.size();
        }

        @Override
        public double getLeftProbability() {
            return l < left.indizes.length &&  c == left.indizes[l] ? 1 : 0;
        }

        @Override
        public double getRightProbability() {
            return r < right.indizes.length &&  c == right.indizes[r] ? 1 : 0;
        }

        @Override
        public boolean isLeftSet() {
            return l < left.indizes.length && c==left.indizes[l];
        }

        @Override
        public boolean isRightSet() {
            return r < right.indizes.length && c==right.indizes[r];
        }

        @Override
        public int getIndex() {
            return c;
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return left.fingerprintVersion.getMolecularProperty(c);
        }

        @Override
        public Iterator<FPIter2> iterator() {
            return clone();
        }
    }

    private static class PairwiseUnionIterator extends PairwiseIterator {
        private final int max;
        public PairwiseUnionIterator(ArrayFingerprint left, ArrayFingerprint right, int c, int l, int r) {
            super(left, right, c, l, r);
            max = Math.max(left.indizes[left.indizes.length-1],right.indizes[right.indizes.length-1]);
        }

        @Override
        public PairwiseUnionIterator clone() {
            return new PairwiseUnionIterator(left,right,c,l,r);
        }

        @Override
        public FPIter2 next() {
            ++c;
            if (c > left.indizes[l]) ++l;
            if (c > right.indizes[r]) ++r;
            c = Math.min(l >= left.indizes.length ? Integer.MAX_VALUE : left.indizes[l], r >= right.indizes.length ? Integer.MAX_VALUE : right.indizes[r]);
            return this;
        }

        @Override
        public boolean hasNext() {
            return c < max;
        }
    }
    private static class PairwiseIntersectionIterator extends PairwiseIterator {
        int nl,nr;
        public PairwiseIntersectionIterator(ArrayFingerprint left, ArrayFingerprint right, int c, int l, int r) {
            super(left, right, c, l, r);
            nl=l; nr=l;
            if (c<0) findNext();
        }

        @Override
        public PairwiseIntersectionIterator clone() {
            return new PairwiseIntersectionIterator(left,right,c,l,r);
        }

        private boolean findNext() {
            while (true){
                if (left.indizes[nl] < right.indizes[nr]) ++nl;
                if (nl >= left.indizes.length) break;
                if (left.indizes[nl] > right.indizes[nr]) ++nr;
                if (nr >= right.indizes.length) break;
                if (left.indizes[nl]==right.indizes[nr]) return true;
            }
            return false;
        }

        @Override
        public FPIter2 next() {
            l=nl; r=nr;c=left.indizes[nl];
            ++nl; ++nr;
            findNext();
            return this;
        }

        @Override
        public boolean hasNext() {
            return nl < left.indizes.length && nr < right.indizes.length;
        }
    }
}
