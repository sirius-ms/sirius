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
