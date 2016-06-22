package de.unijena.bioinf.ChemistryBase.fp;

import com.google.common.base.Joiner;

import java.util.Iterator;

public class ProbabilityFingerprint extends AbstractFingerprint {

    protected final double[] fingerprint;

    public ProbabilityFingerprint(FingerprintVersion fingerprintVersion, double[] fingerprint) {
        super(fingerprintVersion);
        this.fingerprint = fingerprint.clone();
        if (fingerprint.length != fingerprintVersion.size()) throw new IllegalArgumentException("fp length does not match fingerprint version length: " + fingerprint.length + " vs. " + fingerprintVersion.size());

    }

    @Override
    public Fingerprint asDeterministic() {
        final boolean[] fp = new boolean[fingerprint.length];
        for (int i=0; i < fp.length; ++i) fp[i] = fingerprint[i]>=0.5;
        return new BooleanFingerprint(getFingerprintVersion(), fp);
    }

    @Override
    public ProbabilityFingerprint asProbabilistic() {
        return this;
    }

    @Override
    public String toTabSeparatedString() {
        return Joiner.on('\t').join(this);
    }

    @Override
    public double[] toProbabilityArray() {
        return fingerprint.clone();
    }

    @Override
    public boolean isSet(int index) {
        return fingerprint[getFingerprintVersion().getRelativeIndexOf(index)]>=0.5;
    }

    @Override
    public int cardinality() {
        int count=0;
        for (double val : fingerprint)
            if (val >= 0.5)
                ++count;
        return count;
    }

    @Override
    public FPIter iterator() {
        return new BIter(-1);
    }

    @Override
    public FPIter presentFingerprints() {
        return new BIterJustOnes(-1);
    }


    @Override
    public FPIter2 foreachUnion(AbstractFingerprint fp) {
        if (fp instanceof ProbabilityFingerprint) return new PairwiseUnionIterator(this, (ProbabilityFingerprint) fp, -1, -1);
        else throw new IllegalArgumentException("Pairwise iterators are only supported for same type fingerprints;");
        // We cannot express this in javas type system -_- In theory somebody could just implement a pairwise iterator
        // for mixed types
    }

    @Override
    public FPIter2 foreachIntersection(AbstractFingerprint fp) {
        if (fp instanceof ProbabilityFingerprint) return new PairwiseIntersectionIterator(this, (ProbabilityFingerprint) fp, -1, -1);
        else throw new IllegalArgumentException("Pairwise iterators are only supported for same type fingerprints;");
        // We cannot express this in javas type system -_- In theory somebody could just implement a pairwise iterator
        // for mixed types
    }

    @Override
    public FPIter2 foreachPair(AbstractFingerprint fp) {
        if (fp instanceof ProbabilityFingerprint) return new PairwiseIterator(this, (ProbabilityFingerprint) fp, -1, -1);
        else if (fp instanceof BooleanFingerprint) return new PairwiseProbBoolean(this,(BooleanFingerprint)fp, -1);
        else return super.foreachPair(fp);
        // We cannot express this in javas type system -_- In theory somebody could just implement a pairwise iterator
        // for mixed types
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
                if (fingerprint[i] >= 0.5) {
                    return i;
                }
            }
            return fingerprint.length;
        }

        @Override
        public boolean isSet() {
            return fingerprint[current] >= 0.5;
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
            return fingerprint[offset]>=0.5;
        }

        @Override
        public double getProbability() {
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

        public String toString() {
            return String.valueOf(fingerprint[offset]);
        }

        @Override
        public FPIter clone() {
            return new BIter(offset);
        }

        @Override
        public boolean hasNext() {
            return offset+1 < fingerprint.length;
        }

        @Override
        public FPIter next() {
            ++offset;
            return this;
        }
    }


    protected static class PairwiseIterator implements FPIter2 {
        protected final ProbabilityFingerprint left, right;
        protected int current,next;

        public PairwiseIterator(ProbabilityFingerprint left, ProbabilityFingerprint right, int current, int next) {
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
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return next < left.fingerprint.length;
        }

        @Override
        public double getLeftProbability() {
            return left.fingerprint[current];
        }

        @Override
        public double getRightProbability() {
            return right.fingerprint[current];
        }

        @Override
        public boolean isLeftSet() {
            return left.fingerprint[current]>=0.5;
        }

        @Override
        public boolean isRightSet() {
            return right.fingerprint[current]>=0.5;
        }

        @Override
        public int getIndex() {
            return left.fingerprintVersion.getAbsoluteIndexOf(current);
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

    private final static class PairwiseUnionIterator extends PairwiseIterator {

        public PairwiseUnionIterator(ProbabilityFingerprint left, ProbabilityFingerprint right, int current, int next) {
            super(left, right, current, next);
        }

        @Override
        public PairwiseUnionIterator next() {
            current=next;
            for (++next; next < left.fingerprint.length; ++next)
                if (left.fingerprint[next]>=0.5 || right.fingerprint[next]>=0.5) break;
            return this;
        }

        @Override
        public PairwiseUnionIterator clone() {
            return new PairwiseUnionIterator(left,right,current,next);
        }


    }


    private final static class PairwiseIntersectionIterator extends PairwiseIterator {
        private PairwiseIntersectionIterator(ProbabilityFingerprint left, ProbabilityFingerprint right, int current,int next) {
            super(left,right,current,next);
        }

        @Override
        public PairwiseIntersectionIterator next() {
            current=next;
            for (++next; next < left.fingerprint.length; ++next)
                if (left.fingerprint[next]>=0.5 && right.fingerprint[next]>=0.5) break;
            return this;
        }
        @Override
        public PairwiseIntersectionIterator clone() {
            return new PairwiseIntersectionIterator(left,right,current,next);
        }
    }

    protected static class PairwiseBooleanProb implements FPIter2 {
        private final BooleanFingerprint left;
        private final ProbabilityFingerprint right;
        private int offset;

        public PairwiseBooleanProb(BooleanFingerprint left, ProbabilityFingerprint right,int offset) {
            this.left = left;
            this.right = right;
            this.offset=offset;
        }

        @Override
        public FPIter2 clone() {
            return new PairwiseBooleanProb(left,right,offset);
        }

        @Override
        public double getLeftProbability() {
            return left.fingerprint[offset] ? 1 : 0;
        }

        @Override
        public double getRightProbability() {
            return right.fingerprint[offset];
        }

        @Override
        public boolean isLeftSet() {
            return left.fingerprint[offset];
        }

        @Override
        public boolean isRightSet() {
            return right.fingerprint[offset]>=0.5d;
        }

        @Override
        public int getIndex() {
            return left.fingerprintVersion.getAbsoluteIndexOf(offset);
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return left.getFingerprintVersion().getMolecularProperty(getIndex());
        }

        @Override
        public Iterator<FPIter2> iterator() {
            return clone();
        }

        @Override
        public boolean hasNext() {
            return (offset+1) < left.fingerprint.length;
        }

        @Override
        public FPIter2 next() {
            ++offset;
            return this;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    protected static class PairwiseProbBoolean implements FPIter2 {
        private final BooleanFingerprint right;
        private final ProbabilityFingerprint left;
        private int offset;

        public PairwiseProbBoolean(ProbabilityFingerprint left, BooleanFingerprint right,int offset) {
            this.left = left;
            this.right = right;
            this.offset=offset;
        }

        @Override
        public FPIter2 clone() {
            return new PairwiseProbBoolean(left,right,offset);
        }

        @Override
        public double getLeftProbability() {
            return left.fingerprint[offset];
        }

        @Override
        public double getRightProbability() {
            return right.fingerprint[offset] ? 1 : 0;
        }

        @Override
        public boolean isLeftSet() {
            return left.fingerprint[offset]>=0.5d;
        }

        @Override
        public boolean isRightSet() {
            return right.fingerprint[offset];
        }

        @Override
        public int getIndex() {
            return left.fingerprintVersion.getAbsoluteIndexOf(offset);
        }

        @Override
        public MolecularProperty getMolecularProperty() {
            return left.getFingerprintVersion().getMolecularProperty(getIndex());
        }

        @Override
        public Iterator<FPIter2> iterator() {
            return clone();
        }

        @Override
        public boolean hasNext() {
            return (offset+1) < left.fingerprint.length;
        }

        @Override
        public FPIter2 next() {
            ++offset;
            return this;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
