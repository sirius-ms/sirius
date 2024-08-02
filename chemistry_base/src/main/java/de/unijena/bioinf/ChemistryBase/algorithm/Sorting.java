package de.unijena.bioinf.ChemistryBase.algorithm;

import java.util.Arrays;
import java.util.Comparator;

public class Sorting {

    // argsort, todo: implement a faster version in future

    public static int[] argsort(float[] array) {
        Integer[] ids = new Integer[array.length];
        for (int i=0; i < ids.length; ++i) {
            ids[i]=i;
        }
        Arrays.sort(ids, (i,j)->Float.compare(array[i],array[j]));
        return Arrays.stream(ids).mapToInt(x->x).toArray();
    }
    public static int[] argsort(double[] array) {
        Integer[] ids = new Integer[array.length];
        for (int i=0; i < ids.length; ++i) {
            ids[i]=i;
        }
        Arrays.sort(ids, Comparator.comparingDouble(i -> array[i]));
        return Arrays.stream(ids).mapToInt(x->x).toArray();
    }

}
