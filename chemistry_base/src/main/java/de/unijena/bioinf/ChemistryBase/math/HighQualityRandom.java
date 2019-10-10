package de.unijena.bioinf.ChemistryBase.math;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * from: http://www.javamex.com/tutorials/random_numbers/numerical_recipes.shtml
 * The authors of Numerical Recipes: The Art of Scientific Computing propose a random number generator
 * which they advocate as giving a good compromise between quality and speed.
 * It is a combined generator: two XORShift generators are combined with an LCG and a multiply with carry generator.
 * (Without going into all the details here, notice the two blocks of three shifts each, which are the XORShifts;
 * the first line which is the LCG, similar to the standard Java Random algorithm, and the line between the two
 * XORShifts, which is a multiply with carry generator.)
 * Their suggested C implementation is trivially portable to Java, and a suggested implementation is given below.
 * In this case, we subclass java.util.Random, and also make the generator thread-safe. If thread safety is not
 * required, then the Lock could be removed.
 */
public class HighQualityRandom extends Random {
    private Lock l = new ReentrantLock();
    private long u;
    private long v = 4101842887655102017L;
    private long w = 1;

    public HighQualityRandom() {
        this(System.nanoTime());
    }
    public HighQualityRandom(long seed) {
        l.lock();
        u = seed ^ v;
        nextLong();
        v = u;
        nextLong();
        w = v;
        nextLong();
        l.unlock();
    }

    public long nextLong() {
        l.lock();
        try {
            u = u * 2862933555777941757L + 7046029254386353087L;
            v ^= v >>> 17;
            v ^= v << 31;
            v ^= v >>> 8;
            w = 4294957665L * (w & 0xffffffff) + (w >>> 32);
            long x = u ^ (u << 21);
            x ^= x >>> 35;
            x ^= x << 4;
            long ret = (x + v) ^ w;
            return ret;
        } finally {
            l.unlock();
        }
    }

    protected int next(int bits) {
        return (int) (nextLong() >>> (64-bits));
    }

}
