
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

package de.unijena.bioinf.ChemistryBase.math;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * @author Kai Dührkop
 */
public class Statistics {


    public static double robustAverage(double[] xs) {
        if (xs.length < 4) return expectation(xs);
        final double[] ys = xs.clone();
        Arrays.sort(ys);
        double mean = 0d;
        int i=(int)(ys.length*0.25), n=(int)(ys.length*0.75);
        double sz = n-i;
        for (; i < n; ++i) {
            mean += ys[i];
        }
        mean /= sz;
        return mean;
    }

    public static double robustAverage(float[] xs) {
        if (xs.length < 4) return expectation(xs);
        final float[] ys = xs.clone();
        Arrays.sort(ys);
        double mean = 0d;
        int i=(int)(ys.length*0.25), n=(int)(ys.length*0.75);
        double sz = n-i;
        for (; i < n; ++i) {
            mean += ys[i];
        }
        mean /= sz;
        return mean;
    }


    public static double median(double[] xs) {
        return medianInPlace(xs.clone());
    }

    public static double median(TDoubleArrayList ys) {
        return medianInPlace(ys.toArray());
    }

    private static double medianInPlace(final double[] xs) {
        Arrays.sort(xs);
        if (xs.length%2==0) {
            double a = xs[xs.length/2 - 1], b = xs[xs.length/2];
            return (a+b)/2d;
        } else {
            return xs[xs.length/2];
        }
    }



    /**
     * Computes pearson correlation coefficient between xs and ys
     */
    public static double pearson(double[] xs, double[] ys) {
        final double Exs = expectation(xs);
        final double Eys = expectation(ys);
        final double n = (Math.sqrt(variance(xs, Exs)) * Math.sqrt(variance(ys, Eys)));
        return (covariance(xs, ys, Exs, Eys) / n);
    }

    public static double cosine(double[] xs, double[] ys) {
        if (xs.length!=ys.length) throw new IllegalArgumentException("Both vectors should have same length");
        double x = 0d;
        double y = 0d;
        double xy = 0d;
        for (int k=0; k < xs.length; ++k) {
            x += xs[k]*xs[k];
            y += ys[k]*ys[k];
            xy += xs[k]*ys[k];
        }
        return xy/Math.sqrt(x*y);
    }


    public static double spearman(double[] xs, double[] ys, double delta) {
        final double[] rxs = toRank(xs, delta);
        final double[] rys = toRank(ys, delta);
        return pearson(rxs, rys);

    }

    private static double[] toRank(double[] xs, double delta) {
        final double[] rxs = xs.clone();
        final Integer[] stupid = new Integer[xs.length];
        for (int i = 0; i < stupid.length; ++i) stupid[i] = i;
        Arrays.sort(stupid, new Comparator<Integer>() {
            @Override
            public int compare(Integer i, Integer j) {
                return Double.compare(rxs[i], rxs[j]);
            }
        });
        for (int i = 0; i < rxs.length; ++i) {
            int j = i + 1;
            for (; j < rxs.length && (xs[stupid[j]] - xs[stupid[i]]) < delta; ++j) {
            }
            --j;
            for (int l = i; l <= j; ++l) {
                rxs[stupid[l]] = i + (j - i) / 2d;
            }
            i = j;
        }
        return rxs;
    }


    /**
     * Computes mean of xs
     */
    public static double expectation(double[] xs) {
        double sum = 0d;
        for (int i = 0; i < xs.length; ++i) {
            sum += xs[i];
        }
        sum /= xs.length;
        return sum;
    }
    public static double expectation(float[] xs) {
        double sum = 0d;
        for (int i = 0; i < xs.length; ++i) {
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
        for (int i = 0; i < xs.length; ++i) {
            final double d = xs[i] - expectation;
            sum += (d * d);
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
        for (int i = 0; i < xs.length; ++i) {
            sum += (xs[i] - expectation_x) * (ys[i] - expectation_y);
        }
        return sum / xs.length;
    }

    /**
     * permutes the array randomly
     */
    public static void shuffle(double[] array) {
        shuffle(array, array.length, 0, array.length);
    }

    /**
     * permutes a part of the array randomly
     */
    public static void shuffle(double[] array, int number, int offset, int length) {
        if (number > length) throw new IndexOutOfBoundsException("number cannot be smaller than length");
        final Random r = new Random();
        for (int i = offset; i < number; ++i) {
            final int k = r.nextInt(length - i) + i;
            final double mem = array[i];
            array[i] = array[k];
            array[k] = mem;
        }
    }

    /**
     * permutes the array randomly
     */
    public static void shuffle(Object[] array) {
        shuffle(array, array.length, 0, array.length);
    }

    /**
     * permutes a part of the array randomly
     */
    public static void shuffle(Object[] array, int number, int offset, int length) {
        if (number > length) throw new IndexOutOfBoundsException("number cannot be smaller than length");
        final Random r = new Random();
        for (int i = offset; i < number; ++i) {
            final int k = r.nextInt(length - i) + i;
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
        if (srcLength + srcPos > source.length)
            throw new IndexOutOfBoundsException("[" + srcPos + "..." + (srcPos + srcLength) +
                    "] are out of bounds <" + source.length + ">");
        if (destPos + destLength > dest.length)
            throw new IndexOutOfBoundsException("[" + destPos + "..." + (destPos + destLength) +
                    "] are out of bounds <" + dest.length + ">");
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
        shuffle(array, array.length, 0, array.length);
    }

    public static void shuffle(int[] array, int number, int offset, int length) {
        if (number > length) throw new IndexOutOfBoundsException("number cannot be smaller than length");
        final Random r = new Random();
        for (int i = offset; i < number; ++i) {
            final int k = r.nextInt(length - i) + i;
            final int mem = array[i];
            array[i] = array[k];
            array[k] = mem;
        }
    }

    public static void sample(int[] source, int srcPos, int srcLength, int[] dest, int destPos, int destLength) {
        if (srcLength + srcPos > source.length)
            throw new IndexOutOfBoundsException("[" + srcPos + "..." + (srcPos + srcLength) +
                    "] are out of bounds <" + source.length + ">");
        if (destPos + destLength > dest.length)
            throw new IndexOutOfBoundsException("[" + destPos + "..." + (destPos + destLength) +
                    "] are out of bounds <" + dest.length + ">");
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

    public static double geometricAverage(double[] xs, boolean useLog) {
        if (useLog) {
            double x = 0d;
            for (double y : xs) x += Math.log(y);
            return Math.exp(x/xs.length);
        } else {
            double x = 1d;
            for (double y : xs) x*=y;
            return Math.pow(x,1d/xs.length);
        }
    }


    public static double robustGeometricAverage(double[] xs, boolean useLog) {
        if (xs.length < 4) return geometricAverage(xs, useLog);
        final double[] ys = xs.clone();
        Arrays.sort(ys);
        if (useLog) {
            double geom = 0d;
            int i=(int)(ys.length*0.25), n=(int)(ys.length*0.75);
            double sz = n-i;
            for (; i < n; ++i) {
                geom += Math.log(ys[i]);
            }
            geom /= sz;
            return geom;
        } else {
            double geom = 1d;
            int i=(int)(ys.length*0.25), n=(int)(ys.length*0.75);
            double sz = n-i;
            for (; i < n; ++i) {
                geom *= ys[i];
            }
            return Math.pow(geom, 1d/sz);
        }
    }
}
