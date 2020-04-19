package de.unijena.bioinf.ChemistryBase.algorithm;

public class BitsetOps {

    public static void set(long[] vec, int index) {
        int word = index/64;
        int bit = index%64;
        vec[word] |= 1L<<bit;
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

    public static int numberOfCommonBits(long[] a, long[] b) {
        int count = 0;
        for (int i=0, n=Math.min(a.length,b.length); i < n; ++i) {
            count += Long.bitCount(a[i]&b[i]);
        }
        return count;
    }


    public static int nextSetBit(final long[] bits, int fromIndex) {
        int u = fromIndex>>6;
        if (u > bits.length)
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
    public static int numberOfCommonBits(long a, long b) {
        return Long.bitCount(a&b);
    }
    public static int nextSetBit(final long bits, int fromIndex) {
        long word = bits & (0xffffffffffffffffL << fromIndex);
        if (word != 0)
            return Long.numberOfTrailingZeros(word);
        else return -1;
    }




}
