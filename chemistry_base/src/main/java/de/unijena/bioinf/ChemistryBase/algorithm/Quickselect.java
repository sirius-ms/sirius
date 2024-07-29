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

public class Quickselect {

    public static double quickselectInplace(double[] array, int from, int to, int k) {
        return quickselectInplace1(array, from, to-1, k);
    }

    private static double quickselectInplace1(double[] a, int l, int r, int k) {
        long randomState = System.nanoTime()*Double.doubleToLongBits(a[0]+a[a.length-1]);
        while (true) {
            if (l==r) return a[l];
            randomState = randomLong(randomState);
            long q = randomState;
            if (q<0) q = -randomState;
            q = l + (q%(r-l+1));
            int pivot = partition(a,l,r,(int)q);
            if (k==pivot) return a[k];
            else if (k < pivot) r = pivot-1;
            else l = pivot+1;
        }

    }

    private static int partition(double[] a, int l, int r, int pivot) {
        double v = a[pivot];
        a[pivot] = a[r];
        a[r] = v;
        int s = l;
        for (int i=l; i < r; ++i) {
            if (a[i] < v) {
                double x = a[s];
                a[s] = a[i];
                a[i] = x;
                ++s;
            }
        }
        v = a[r];
        a[r] = a[s];
        a[s] = v;
        return s;
    }

    private static long randomLong(long x) {
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return x;
    }

    public static double quickselectInplace(float[] array, int from, int to, int k) {
        return quickselectInplace1(array, from, to-1, k);
    }

    private static double quickselectInplace1(float[] a, int l, int r, int k) {
        long randomState = System.nanoTime()*Double.doubleToLongBits(a[0]+a[a.length-1]);
        while (true) {
            if (l==r) return a[l];
            randomState = randomLong(randomState);
            long q = randomState;
            if (q<0) q = -randomState;
            q = l + (q%(r-l+1));
            int pivot = partition(a,l,r,(int)q);
            if (k==pivot) return a[k];
            else if (k < pivot) r = pivot-1;
            else l = pivot+1;
        }

    }

    private static int partition(float[] a, int l, int r, int pivot) {
        float v = a[pivot];
        a[pivot] = a[r];
        a[r] = v;
        int s = l;
        for (int i=l; i < r; ++i) {
            if (a[i] < v) {
                float x = a[s];
                a[s] = a[i];
                a[i] = x;
                ++s;
            }
        }
        v = a[r];
        a[r] = a[s];
        a[s] = v;
        return s;
    }

}
