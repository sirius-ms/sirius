package de.unijena.bioinf.ChemistryBase.algorithm;

import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * I really hate the very limited support of Java for binary search. There are many scenarios possible, so feel free to implement all these
 * kind of methods whenever you need them.
 */
public final class BinarySearch {

    /**
     * Given a list of objects which can be sorted using some integer key. Search for a given integer key,
     */
    public static <T> int searchForInt(List<T> list, ToIntFunction<T> intfunc, int key) {
        if (!(list instanceof RandomAccess))
            list = (List<T>)Arrays.asList(list.toArray());
        return binarySearchForInt1(list, intfunc, 0, list.size(), key);
    }

    /**
     * Given a function that maps an integer to an integer. Search for a given integer.
     */
    public static <T> int searchForIntByIndex(IntToIntFunction intfunc, int fromIndex, int toIndex, int key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = intfunc.applyAsInt(mid);

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Given a function that maps an integer to a double. Search for a given double.
     */
    public static <T> int searchForDoubleByIndex(IntToDoubleFunction f, int fromIndex, int toIndex, double key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = f.applyAsDouble(mid);

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Given a list of objects which can be sorted using some double key. Search for a given double key,
     */
    public static <T> int searchForDouble(List<T> list, ToDoubleFunction<T> doublefunc, double key) {
        if (!(list instanceof RandomAccess))
            list = (List<T>)Arrays.asList(list.toArray());
        return binarySearchForDouble1(list, doublefunc, 0, list.size(), key);
    }

    private static <T> int  binarySearchForInt1(List<T> a, ToIntFunction<T> func, int fromIndex, int toIndex, int key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = func.applyAsInt(a.get(mid));

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }
    private static <T> int  binarySearchForDouble1(List<T> a, ToDoubleFunction<T> func, int fromIndex, int toIndex, double key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = func.applyAsDouble(a.get(mid));

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    private interface IntToIntFunction {
        public int applyAsInt(int v);
    }

}
