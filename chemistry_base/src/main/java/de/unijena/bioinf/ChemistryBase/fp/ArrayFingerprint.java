package de.unijena.bioinf.ChemistryBase.fp;

import com.google.common.base.Joiner;

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
    public String toOneZeroString() {
        final char[] buffer = new char[fingerprintVersion.size()];
        for (short index : indizes) {
            buffer[fingerprintVersion.getRelativeIndexOf(index)] = '1';
        }
        return new String(buffer);
    }

    @Override
    public boolean[] toBooleanArray() {
        final boolean[] buffer = new boolean[fingerprintVersion.size()];
        for (short index : indizes) {
            buffer[fingerprintVersion.getRelativeIndexOf(index)] = true;
        }
        return buffer;
    }

    @Override
    public short[] toIndizesArray() {
        return indizes.clone();
    }

    @Override
    public double tanimoto(Fingerprint other) {
        if (other instanceof ArrayFingerprint) return tanimoto((ArrayFingerprint)other);
        else return super.tanimoto(other);
    }

    @Override
    public double dotProduct(Fingerprint other) {
        if (other instanceof ArrayFingerprint) return dotProduct((ArrayFingerprint)other);
        else return super.dotProduct(other);
    }

    public double dotProduct(ArrayFingerprint other) {
        final short[] as = indizes, bs=other.indizes;
        int a=0, b=0, union=0;
        while(a < as.length && b < bs.length) {
            if (as[a]==bs[b]) {
                ++union;
                ++a; ++b;
            } else if (as[a] > bs[b]) {
                ++b;
            } else {
                ++a;
            }
        }
        return union;
    }

    public double plusMinusdotProduct(Fingerprint other) {
        if (other instanceof ArrayFingerprint) return plusMinusdotProduct((ArrayFingerprint)other);
        else return super.plusMinusdotProduct(other);
    }

    public double plusMinusdotProduct(ArrayFingerprint other) {
        final int length = fingerprintVersion.size();
        final short[] as = indizes, bs=other.indizes;
        int a=0, b=0, intersection=0;
        while(a < as.length && b < bs.length) {
            if (as[a]==bs[b]) {
                ++intersection;
                ++a; ++b;
            } else if (as[a] > bs[b]) {
                ++b;
            } else {
                ++a;
            }
        }

        // |A n B| = (|A| + |B|) - |A u B|
        final int union = as.length + bs.length - intersection;
        // number of (1,1) pairs: intersection
        // number of {-1,1} pairs: union  - intersection
        // number of (-1,-1) pairs: length - union
        // dot product is intersection + (length-union) - (union - intersection)

        return intersection + (length-union) - (union-intersection);
    }

    public double tanimoto(ArrayFingerprint other) {
        final short[] as = indizes, bs=other.indizes;
        int a=0, b=0, intersection=0;
        while(a < as.length && b < bs.length) {
            if (as[a]==bs[b]) {
                ++intersection;
                ++a; ++b;
            } else if (as[a] > bs[b]) {
                ++b;
            } else {
                ++a;
            }
        }

        // |A n B| = (|A| + |B|) - |A u B|
        final int union = as.length + bs.length - intersection;

        // Jaccard := |(A n B)| / (A u B)
        return ((double)intersection)/union;
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
    public String toTabSeparatedString() {
        return Joiner.on('\t').join(this);
    }

    @Override
    public double[] toProbabilityArray() {
        final double[] ary = new double[fingerprintVersion.size()];
        for (int index : indizes) ary[fingerprintVersion.getRelativeIndexOf(index)] = 1d;
        return ary;
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
        else return super.foreachPair(fp);
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
        private int relative;

        public ArrayIterator(int offset, int absolute) {
            this.offset = offset;
            this.relative = absolute;
        }

        @Override
        public boolean isSet() {
            return offset < indizes.length && fingerprintVersion.getAbsoluteIndexOf(relative)==indizes[offset];
        }

        @Override
        public int getIndex() {
            return fingerprintVersion.getAbsoluteIndexOf(relative);
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return fingerprintVersion.getMolecularProperty(relative);
        }

        @Override
        public FPIter clone() {
            return new ArrayIterator(offset, relative);
        }

        @Override
        public boolean hasNext() {
            return relative < fingerprintVersion.size();
        }

        public String toString() {return isSet() ? "1" : "0";}

        @Override
        public FPIter next() {
            ++relative;
            if (offset < indizes.length && fingerprintVersion.getAbsoluteIndexOf(relative) > indizes[offset]) ++offset;
            return this;
        }
    }

    private static class PairwiseIterator implements FPIter2 {
        protected final ArrayFingerprint left,right;
        protected int l, r, relative;
        protected final FingerprintVersion fingerprintVersion;

        protected PairwiseIterator(ArrayFingerprint left, ArrayFingerprint right, int c, int l, int r) {
            this.left = left;
            this.right = right;
            this.relative = c;
            this.l = l;
            this.r = r;
            this.fingerprintVersion = left.fingerprintVersion;
        }

        @Override
        public FPIter2 clone() {
            return new PairwiseIterator(left,right,relative,l,r);
        }

        @Override
        public FPIter2 next() {
            ++relative;
            if (l < left.indizes.length && fingerprintVersion.getAbsoluteIndexOf(relative) > left.indizes[l]) ++l;
            if (r < right.indizes.length && fingerprintVersion.getAbsoluteIndexOf(relative) > right.indizes[r]) ++r;
            return this;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return (relative+1) < left.fingerprintVersion.size();
        }

        @Override
        public double getLeftProbability() {
            return l < left.indizes.length &&  fingerprintVersion.getAbsoluteIndexOf(relative) == left.indizes[l] ? 1 : 0;
        }

        @Override
        public double getRightProbability() {
            return r < right.indizes.length &&  fingerprintVersion.getAbsoluteIndexOf(relative) == right.indizes[r] ? 1 : 0;
        }

        @Override
        public boolean isLeftSet() {
            return l < left.indizes.length && fingerprintVersion.getAbsoluteIndexOf(relative)==left.indizes[l];
        }

        @Override
        public boolean isRightSet() {
            return r < right.indizes.length && fingerprintVersion.getAbsoluteIndexOf(relative)==right.indizes[r];
        }

        @Override
        public int getIndex() {
            return fingerprintVersion.getAbsoluteIndexOf(relative);
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return left.fingerprintVersion.getMolecularProperty(fingerprintVersion.getAbsoluteIndexOf(relative));
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
            if (left.indizes.length==0) {
                if (right.indizes.length==0) max=-1;
                else max = right.indizes[right.indizes.length-1];
            } else if (right.indizes.length==0) max=left.indizes[left.indizes.length-1];
            else max = Math.max(left.indizes[left.indizes.length-1],right.indizes[right.indizes.length-1]);
        }

        @Override
        public PairwiseUnionIterator clone() {
            return new PairwiseUnionIterator(left,right,relative,l,r);
        }

        @Override
        public FPIter2 next() {
            ++relative;
            if (l < left.indizes.length && relative > left.indizes[l]) ++l;
            if (r < right.indizes.length &&  relative > right.indizes[r]) ++r;
            relative = Math.min(l >= left.indizes.length ? Integer.MAX_VALUE : left.indizes[l], r >= right.indizes.length ? Integer.MAX_VALUE : right.indizes[r]);
            return this;
        }

        @Override
        public boolean hasNext() {
            return relative < max;
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
            return new PairwiseIntersectionIterator(left,right,relative,l,r);
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
            l=nl; r=nr;relative=left.indizes[nl];
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
