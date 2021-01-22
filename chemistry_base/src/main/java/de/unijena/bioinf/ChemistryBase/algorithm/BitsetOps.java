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

package de.unijena.bioinf.ChemistryBase.algorithm;

public final class BitsetOps {

    public static void set(long[] vec, int index) {
        int word = index/64;
        int bit = index%64;
        vec[word] |= 1L<<bit;
    }

    public static long maskFor(int index) {
        return (1L<<index);
    }

    public static boolean get(long[] vec, int index) {
        int word = index/64;
        int bit = index%64;
        return (vec[word] & (1L<<bit))!=0;
    }

    public static void clear(long[] vec, int index) {
        int word = index/64;
        int bit = index%64;
        vec[word] &= ~(1L<<bit);
    }

    public static void set(long[] vec, int index, boolean value) {
        int word = index/64;
        int bit = index%64;
        if (value) vec[word] |= 1L<<bit;
        else vec[word] &= ~(1L<<bit);
    }

    public static double jaccard(long[] a, long[] b) {
        int union = 0, intersection = 0;
        for (int i=0, n=Math.min(a.length,b.length); i < n; ++i) {
            intersection += Long.bitCount(a[i]&b[i]);
            union += Long.bitCount(a[i]|b[i]);
        }
        if (a.length>b.length) {
            for (int i=b.length; i < a.length; ++i) {
                union += Long.bitCount(a[i]);
            }
        } else if (b.length > a.length) {
            for (int i=a.length; i < b.length; ++i) {
                union += Long.bitCount(b[i]);
            }
        }
        if (union==0) return 0d;
        return ((double)intersection)/((double)union);
    }

    public static int numberOfCommonBits(long[] a, long[] b) {
        int count = 0;
        for (int i=0, n=Math.min(a.length,b.length); i < n; ++i) {
            count += Long.bitCount(a[i]&b[i]);
        }
        return count;
    }


    public static int nextSetBit(final long[] bits, int fromIndex) {
        int u = fromIndex>>6;
        if (u >= bits.length)
            return -1;

        long word = bits[u] & (0xffffffffffffffffL << fromIndex);

        while (true) {
            if (word != 0)
                return (u * 64) + Long.numberOfTrailingZeros(word);
            if (++u == bits.length)
                return -1;
            word = bits[u];
        }
    }


    public static long set(long vec, int index) {
        return vec | (1L<<index);
    }
    public static long clear(long vec, int index) {
        return vec & ~(1L<<index);
    }
    public static long set(long vec, int index, boolean value) {
        if (value) return set(vec,index);
        else return clear(vec,index);
    }
    public static boolean get(long vec, int index) {
        return (vec & (1L<<index)) != 0;
    }
    public static int numberOfCommonBits(long a, long b) {
        return Long.bitCount(a&b);
    }
    public static int nextSetBit(final long bits, int fromIndex) {
        long word = bits & (0xffffffffffffffffL << fromIndex);
        if (word != 0)
            return Long.numberOfTrailingZeros(word);
        else return -1;
    }


    public static long difference(long a, long b) {
        return a & ~b;
    }

    public static int cardinality(long bitset) {
        return Long.bitCount(bitset);
    }
}
