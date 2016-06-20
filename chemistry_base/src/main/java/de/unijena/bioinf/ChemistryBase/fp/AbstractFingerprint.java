package de.unijena.bioinf.ChemistryBase.fp;

import java.util.Iterator;

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
