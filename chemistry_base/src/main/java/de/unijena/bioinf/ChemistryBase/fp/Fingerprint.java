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

public abstract class Fingerprint extends AbstractFingerprint {

    public Fingerprint(FingerprintVersion fingerprintVersion) {
        super(fingerprintVersion);
    }

    public abstract ArrayFingerprint asArray();
    public abstract BooleanFingerprint asBooleans();

    public abstract String toOneZeroString();

    public abstract boolean[] toBooleanArray();
    public abstract short[] toIndizesArray();

    public static ArrayFingerprint fromOneZeroString(FingerprintVersion version, String fp) {
        fp = fp.trim();
        if (fp.length() != version.size()) throw new RuntimeException("Fingerprint version does not match given string: " + version.size() + " bits vs. " + fp.length());
        TShortArrayList indizes = new TShortArrayList(400);
        for (int k=0; k < fp.length(); ++k) {
            if (fp.charAt(k) == '1') indizes.add((short)version.getAbsoluteIndexOf(k));
        }
        return new ArrayFingerprint(version, indizes.toArray());
    }

    public static ArrayFingerprint fromTabSeparatedString(FingerprintVersion version, String s) {
        if (s.length()==0) return new ArrayFingerprint(version, new short[]{});
        final String[] split = s.split("\t");
        final short[] indizes = new short[split.length];
        for (int k=0; k < split.length; ++k) indizes[k] = Short.parseShort(split[k]);
        return new ArrayFingerprint(version,indizes);
    }


    public static ArrayFingerprint fromOneZeroString(String fp) {
        return fromOneZeroString(CdkFingerprintVersion.getDefault(), fp);
    }

    public static ArrayFingerprint fromCommaSeparatedString(FingerprintVersion version, String s) {
        if (s.length()==0) return new ArrayFingerprint(version, new short[]{});
        if (s.charAt(0)=='{' || s.charAt(0)=='[' || s.charAt(0)=='(') s = s.substring(1, s.length()-1);
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
