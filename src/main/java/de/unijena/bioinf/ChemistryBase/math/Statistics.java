package de.unijena.bioinf.ChemistryBase.math;

import java.util.Random;

/**
 * @author Kai Dührkop
 */
public class Statistics {

    /**
     * Computes pearson correlation coefficient between xs and ys
     */
    public static double pearson(double[] xs, double[] ys) {
        final double Exs = expectation(xs);
        final double Eys = expectation(ys);
        final double n = (Math.sqrt(variance(xs, Exs)) * Math.sqrt(variance(ys, Eys)));
        return (covariance(xs, ys, Exs, Eys) / n);
    }

    /**
     * Computes mean of xs
     */
    public static double expectation(double[] xs) {
        double sum = 0d;
        for (int i=0; i < xs.length; ++i) {
            sum += xs[i];
        }
        sum /= xs.length;
        return sum;
    }

    /**
     * Computes variance of xs
     */
    public static double variance(double[] xs) {
        return variance(xs, expectation(xs));
    }

    /**
     * Computes variance of xs if mean is already computed
     */
    public static double variance(double[] xs, double expectation) {
        double sum = 0d;
        for (int i=0; i < xs.length; ++i) {
            final double d = xs[i] - expectation;
            sum += (d*d);
        }
        return sum / xs.length;
    }

    /**
     * Computes covariance of xs and ys
     */
    public static double covariance(double[] xs, double[] ys) {
        return covariance(xs, ys, expectation(xs), expectation(ys));
    }

    public static double covariance(double[] xs, double[] ys, double expectation_x, double expectation_y) {
        if (xs.length != ys.length) throw new IllegalArgumentException("Both arrays should have the same length!");
        double sum = 0d;
        for (int i=0; i < xs.length; ++i) {
            sum += (xs[i] - expectation_x) * (ys[i] - expectation_y);
        }
        return sum / xs.length;
    }

    /**
     * permutes the array randomly
     */
    public static void shuffle(double[] array) {
        shuffle(array, 0, array.length, array.length);
    }

    /**
     * permutes a part of the array randomly
     */
    public static void shuffle(double[] array, int number, int offset, int length) {
        if (number > length) throw new IndexOutOfBoundsException("number cannot be smaller than length");
        final Random r = new Random();
        for (int i=offset; i < number; ++i) {
            final int k = r.nextInt(length-i)+i;
            final double mem = array[i];
            array[i] = array[k];
            array[k] = mem;
        }
    }

    /**
     * permutes the array randomly
     */
    public static void shuffle(Object[] array) {
        shuffle(array, 0, array.length, array.length);
    }

    /**
     * permutes a part of the array randomly
     */
    public static void shuffle(Object[] array, int number, int offset, int length) {
        if (number > length) throw new IndexOutOfBoundsException("number cannot be smaller than length");
        final Random r = new Random();
        for (int i=offset; i < number; ++i) {
            final int k = r.nextInt(length-i)+i;
            final Object mem = array[i];
            array[i] = array[k];
            array[k] = mem;
        }
    }

    /**
     * copies randomly values from the array into the destination array.
     * This is done WITHOUT repition
     */
    public static void sample(double[] source, int srcPos, int srcLength, double[] dest, int destPos, int destLength) {
        if (srcLength+srcPos > source.length)
            throw new IndexOutOfBoundsException("["+srcPos+"..."+(srcPos+srcLength)+
                    "] are out of bounds <"+source.length+">");
        if (destPos+destLength > dest.length)
            throw new IndexOutOfBoundsException("["+destPos+"..."+(destPos+destLength)+
                    "] are out of bounds <"+dest.length+">");
        if (srcLength < destLength)
            throw new IndexOutOfBoundsException("too few elements in src array");
        if (destPos + destLength == srcPos + srcLength) {
            sample1(source, srcPos, dest, destPos, srcLength);
            return;
        }
        final double[] dst = new double[srcLength];
        System.arraycopy(source, srcPos, dst, 0, srcLength);
        shuffle(dst, destLength, 0, dst.length);
        System.arraycopy(dst, 0, dest, destPos, destLength);
    }

    private static void sample1(double[] src, int srcPos, double[] dest, int dstPos, int length) {
        System.arraycopy(src, srcPos, dest, dstPos, length);
        shuffle(dest, dstPos, length, length);
    }

    public static void shuffle(int[] array) {
        shuffle(array, 0, array.length, array.length);
    }

    public static void shuffle(int[] array, int number, int offset, int length) {
        if (number > length) throw new IndexOutOfBoundsException("number cannot be smaller than length");
        final Random r = new Random();
        for (int i=offset; i < number; ++i) {
            final int k = r.nextInt(length-i)+i;
            final int mem = array[i];
            array[i] = array[k];
            array[k] = mem;
        }
    }

    public static void sample(int[] source, int srcPos, int srcLength, int[] dest, int destPos, int destLength) {
        if (srcLength+srcPos > source.length)
            throw new IndexOutOfBoundsException("["+srcPos+"..."+(srcPos+srcLength)+
                    "] are out of bounds <"+source.length+">");
        if (destPos+destLength > dest.length)
            throw new IndexOutOfBoundsException("["+destPos+"..."+(destPos+destLength)+
                    "] are out of bounds <"+dest.length+">");
        if (srcLength < destLength)
            throw new IndexOutOfBoundsException("too few elements in src array");
        if (destPos + destLength == srcPos + srcLength) {
            sample1(source, srcPos, dest, destPos, srcLength);
            return;
        }
        final int[] dst = new int[srcLength];
        System.arraycopy(source, srcPos, dst, 0, srcLength);
        shuffle(dst, destLength, 0, dst.length);
        System.arraycopy(dst, 0, dest, destPos, destLength);
    }

    private static void sample1(int[] src, int srcPos, int[] dest, int dstPos, int length) {
        System.arraycopy(src, srcPos, dest, dstPos, length);
        shuffle(dest, dstPos, length, length);
    }


}
