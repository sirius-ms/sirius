package de.unijena.bioinf.ChemistryBase.algorithm;

import java.util.Arrays;

/**
 * A fixed size queue of double values. Inserting a new value into a full queue will lead to a removal of the smallest element
 */
public final class BoundedDoubleQueue {

    private final double[] values;
    private int length;

    public BoundedDoubleQueue(int size) {
        this.values = new double[size];
        Arrays.fill(values, Double.NEGATIVE_INFINITY);
        this.length = 0;
    }

    public int length() {
        return length;
    }

    public double min() {
        return values[0];
    }

    public double max() {
        return values[1];
    }

    public boolean add(double value) {
        final int index = (length <= 5) ? linearSearch(value) : binarySearch(value);
        if (length < values.length) {
            if (index < length) System.arraycopy(values, index, values, index+1, length-index);
            values[index] = value;
            ++length;
            return true;
        } else {
            if (index > 0) {
                if (index > 1) System.arraycopy(values, 1, values, 0, index-1);
                values[index-1] = value;
                return true;
            } else return false;
        }
    }

    private int binarySearch(double value) {
        final int index = Arrays.binarySearch(values, 0, length, value);
        if (index >= 0) return index;
        else return (-index-1);
    }

    private int linearSearch(double value) {
        for (int i=0; i < length; ++i) {
            if (values[i] > value) {
                return i;
            }
        }
        return length;
    }
}
