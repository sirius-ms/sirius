package de.unijena.bioinf.ChemistryBase.fp;

import gnu.trove.list.array.TShortArrayList;

public abstract class Fingerprint extends AbstractFingerprint {

    public Fingerprint(FingerprintVersion fingerprintVersion) {
        super(fingerprintVersion);
    }

    public abstract ArrayFingerprint asArray();
    public abstract BooleanFingerprint asBooleans();

    public abstract String toOneZeroString();

    public abstract boolean[] toBooleanArray();
    public abstract short[] toIndizesArray();

    protected final void enforceCompatibility(AbstractFingerprint other) {
        if (!isCompatible(other)) {
            throw new IllegalArgumentException("fingerprint versions differ: " + fingerprintVersion.toString() + " vs. " + other.fingerprintVersion.toString());
        }
    }

    public static ArrayFingerprint fromOneZeroString(FingerprintVersion version, String fp) {
        if (fp.length() != version.size()) throw new RuntimeException("Fingerprint version does not match given string: " + version.size() + " bits vs. " + fp.length());
        TShortArrayList indizes = new TShortArrayList(400);
        for (int k=0; k < fp.length(); ++k) {
            if (fp.charAt(k) == '1') indizes.add((short)version.getAbsoluteIndexOf(k));
        }
        return new ArrayFingerprint(version, indizes.toArray());
    }


    public static ArrayFingerprint fromOneZeroString(String fp) {
        return fromOneZeroString(CdkFingerprintVersion.getDefault(), fp);
    }

    public static ArrayFingerprint fromCommaSeparatedString(FingerprintVersion version, String s) {
        String[] tbs = s.split(",");
        final short[] indizes = new short[tbs.length];
        for (int i=0; i < tbs.length; ++i) indizes[i] = Short.parseShort(tbs[i]);
        return new ArrayFingerprint(version, indizes);
    }
    public static ArrayFingerprint fromCommaSeparatedString(String s) {
        return fromCommaSeparatedString(CdkFingerprintVersion.getDefault(), s);
    }

    /**
     * Computes the dot product of two fingerprints represented as -1|1 vector
     */
    public double plusMinusdotProduct(Fingerprint other) {
        enforceCompatibility(other);
        final int length = fingerprintVersion.size();
        short union=0, intersection=0;
        for (FPIter2 pairwise : foreachPair(other)) {
            final boolean a = pairwise.isLeftSet();
            final boolean b = pairwise.isRightSet();

            if (a || b) ++union;
            if (a && b) ++intersection;
        }
        // number of (1,1) pairs: intersection
        // number of {-1,1} pairs: union  - intersection
        // number of (-1,-1) pairs: length - union
        // dot product is intersection + (length-union) - (union - intersection)

        return intersection + (length-union) - (union-intersection);
    }

    /**
     * Computes the dot product of two fingerprints represented as 0|1 vector
     */
    public double dotProduct(Fingerprint other) {
        enforceCompatibility(other);
        long union=0;
        short left=0, right=0;
        for (FPIter2 pairwise : foreachPair(other)) {
            final boolean a = pairwise.isLeftSet();
            if (a) ++left;
            final boolean b = pairwise.isRightSet();
            if (b) ++right;
            if (a || b) ++union;
        }
        return union;
    }


}
