
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

package de.unijena.bioinf.fingerid;

import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;
import java.util.HashSet;

public class Mask {

    private final static int USED_INDEX = -1;
    private final static int ALWAYS_TRUE = -2;
    private final static int ALWAYS_FALSE = -3;
    private final static int PUBCHEM_SPECIAL = -4;
    private final static int DISABLED = -5;

    private final static char   ALWAYS_TRUE_TOKEN = 't',
                                ALWAYS_FALSE_TOKEN = 'f',
                                USED_INDEX_TOKEN = 'x',
                                PUBCHEM_TOKEN = 'p',
                                DISABLED_TOKEN = 'd';

    private int[] usedIndizes;
    private final int[] bits;

    public static Mask compute(boolean[][] fingerprints) {
        return compute(fingerprints, 1);
    }

    public static Mask compute(boolean[][] fingerprints, String[] inchikeys, int minNumber, int maxNumber) {
        final int N = fingerprints[0].length;
        final int[] bits = new int[N];
        Arrays.fill(bits, USED_INDEX);
        final Mask m = new Mask(bits);
        m.removeDuplicates(fingerprints, minNumber, maxNumber, inchikeys);
        return m;
    }

    public static Mask compute(boolean[][] fingerprints, int minNumber, int maxNumber) {
        final int N = fingerprints[0].length;
        final int[] bits = new int[N];
        Arrays.fill(bits, USED_INDEX);
        final Mask m = new Mask(bits);
        m.removeDuplicates(fingerprints, minNumber, maxNumber, null);
        return m;
    }
    public static Mask compute(boolean[][] fingerprints, int threshold) {
        return compute(fingerprints, threshold, fingerprints.length-threshold);
    }

    public void removeDuplicates(boolean[][] fingerprints) {
        removeDuplicates(fingerprints, 1, fingerprints.length-1, null);
    }

    public void removeDuplicates(boolean[][] fingerprints, int threshold) {
        removeDuplicates(fingerprints, threshold, fingerprints.length-threshold, null);
    }

    /**
     * @return a map (a to b) where a is an index of a duplicated MP and b is the index of the used MP
     */
    public TIntIntHashMap getDuplicates() {
        final TIntIntHashMap map = new TIntIntHashMap();
        for (int i=0; i < bits.length; ++i) {
            if (bits[i] >= 0) {
                map.put(i, bits[i]);
            }
        }
        return map;
    }

    public void removeDuplicates(boolean[][] fingerprints, int minNumber, int maxNumber, String[] inchikeys) {
        final int N = fingerprints[0].length;
        usedIndizes=null;
        HashSet<String> usedKeys = new HashSet<String>();
        // STEP 1: Search for empty COLUMNS
        if (minNumber==1 && maxNumber==fingerprints.length-1) {
            eachCol:
            for (int k = 0; k < N; ++k) {
                final boolean bit = fingerprints[0][k];
                for (int i = 1; i < fingerprints.length; ++i) {
                    if (fingerprints[i][k] != bit) continue eachCol;
                }
                bits[k] = bit ? ALWAYS_TRUE : ALWAYS_FALSE;
            }
        } else {
            final int[] counts = new int[fingerprints[0].length];
            for (int k = 0; k < N; ++k) {
                usedKeys.clear();
                for (int i = 0; i < fingerprints.length; ++i) {
                    if (inchikeys!=null && !usedKeys.add(inchikeys[i])) {
                        continue;
                    }
                    if (fingerprints[i][k]) {
                        ++counts[k];
                    }
                }
            }
            for (int i=0; i < counts.length; ++i) {
                if (counts[i] < minNumber) bits[i] = ALWAYS_FALSE;
                else if (counts[i] > maxNumber) bits[i] = ALWAYS_TRUE;
            }
            System.out.println("Counts:");
            for (int k = 0; k < N; ++k) {
                System.out.println(k + "\t" + counts[k]);
            }
        }
        // STEP 2: Search for identical columns
        for (int k = 0; k < N; ++k) {
            if (bits[k] != USED_INDEX) continue;
            eachOtherCol:
            for (int l = k + 1; l < N; ++l) {
                if (bits[l] != USED_INDEX) continue;
                for (int i = 0; i < fingerprints.length; ++i) {
                    if (fingerprints[i][k] != fingerprints[i][l]) continue eachOtherCol;
                }
                bits[l] = k;
            }
        }
    }

    public static Mask fromString(String[] str) {
        final int N = str.length;
        final int[] bits = new int[N];
        for (int i = 0; i < str.length; ++i) {
            final String token = str[i];
            if (token.length() == 1 && !Character.isDigit(token.charAt(0))) {
                final char t = token.charAt(0);
                if (t == 'x' || t == 'X') {
                    bits[i] = USED_INDEX;
                } else if (t == 'f' || t == 'F') {
                    bits[i] = ALWAYS_FALSE;
                } else if (t == 't' || t == 'T') {
                    bits[i] = ALWAYS_TRUE;
                } else if (t == 'd') {
                    bits[i] = DISABLED;
                } else if (t == 'p') {
                    bits[i] = PUBCHEM_SPECIAL;
                } else throw new IllegalArgumentException("Unknown token: '" + t + "'");
            } else {
                final int k = Integer.parseInt(token);
                bits[i] = k;
            }
        }
        return new Mask(bits);
    }

    public Mask(int size) {
        this.bits = new int[size];
        Arrays.fill(bits, USED_INDEX);

    }

    public Mask(int size, int[] indizes) {
        this.bits = new int[size];
        Arrays.fill(bits, DISABLED);
        for (int i : indizes) bits[i] = USED_INDEX;
    }

    Mask(int[] bits) {
        this.bits = bits;
    }

    public String apply(String fp) {
        rebuildUsedIndizes();
        final StringBuilder truncated = new StringBuilder(usedIndizes.length);
        for (int i = 0; i < usedIndizes.length; ++i)
            truncated.append(fp.charAt(usedIndizes[i]));
        return truncated.toString();
    }

    public String unapply(String fp) {
        final StringBuilder complete = new StringBuilder(bits.length);
        int k = 0;
        for (int i = 0; i < bits.length; ++i) {
            final int b = bits[i];
            if (b == ALWAYS_TRUE) complete.append('1');
            else if (b == ALWAYS_FALSE) complete.append('0');
            else if (b == USED_INDEX) complete.append(fp.charAt(k++));
            else if (b < 0) complete.append('0');
            else {
                if (b >= i) throw new RuntimeException("Duplicate appears before it's origin");
                complete.append(complete.charAt(b));
            }
        }
        return complete.toString();
    }

    public double[] unapply(double[] fp) {
        final double[] complete = new double[bits.length];
        int k = 0;
        for (int i = 0; i < bits.length; ++i) {
            final int b = bits[i];
            if (b == ALWAYS_TRUE) complete[i] = 1d;
            else if (b == ALWAYS_FALSE) complete[i] = 0d;
            else if (b == USED_INDEX) complete[i] = fp[k++];
            else if (b < 0) complete[i] = 0d;
            else {
                if (b >= i) throw new RuntimeException("Duplicate appears before it's origin");
                complete[i] = complete[b];
            }
        }
        return complete;
    }

    public boolean[] apply(boolean[] fp) {
        rebuildUsedIndizes();
        final boolean[] truncated = new boolean[usedIndizes.length];
        for (int i = 0; i < usedIndizes.length; ++i)
            truncated[i] = fp[usedIndizes[i]];
        return truncated;
    }

    public boolean has(int index) {
        return bits[index]==USED_INDEX;
    }

    public int numberOfFingerprints() {
        return bits.length;
    }

    public int[] usedIndizes() {
        rebuildUsedIndizes();
        return usedIndizes.clone();
    }

    private void rebuildUsedIndizes() {
        if (usedIndizes!=null) return;
        int counter = 0;
        for (int k=0; k < bits.length; ++k) {
            if (bits[k] == USED_INDEX) ++counter;
        }
        this.usedIndizes = new int[counter];
        counter=0;
        for (int k=0; k < bits.length; ++k) {
            if (bits[k] == USED_INDEX) usedIndizes[counter++] = k;
        }
    }

    public void disableFingerprint(int index) {
        bits[index] = DISABLED;
        usedIndizes=null;
    }

    public boolean[] unapply(boolean[] fp) {
        final boolean[] complete = new boolean[bits.length];
        int k = 0;
        for (int i = 0; i < bits.length; ++i) {
            final int b = bits[i];
            if (b == ALWAYS_TRUE) complete[i] = true;
            else if (b == ALWAYS_FALSE) complete[i] = false;
            else if (b == USED_INDEX) complete[i] = fp[k++];
            else if (b > 0) {
                if (b >= i) throw new RuntimeException("Duplicate appears before it's origin");
                complete[i] = complete[b];
            }
        }
        return complete;
    }


    public String toString() {
        final StringBuilder buf = new StringBuilder(bits.length * 4);
        for (int i = 0; i < bits.length; ++i) {
            final int b = bits[i];
            if (b >= 0) buf.append(b);
            else {
                final char token;
                switch (b) {
                    case ALWAYS_TRUE: token = ALWAYS_TRUE_TOKEN; break;
                    case ALWAYS_FALSE: token = ALWAYS_FALSE_TOKEN; break;
                    case PUBCHEM_SPECIAL: token = PUBCHEM_TOKEN; break;
                    case USED_INDEX: token = USED_INDEX_TOKEN; break;
                    case DISABLED: token = DISABLED_TOKEN; break;
                    default: throw new RuntimeException("unknown bit " + b);
                }
                buf.append(token);
            }
            if (i + 1 < bits.length) buf.append('\t');
        }
        return buf.toString();
    }

}
