package de.unijena.bioinf.MassDecomposer;

/**
 * A simple POJO which defines a range from min to max (including max).
 */
public class Interval {

    private final long min;
    private final long max;

    public Interval(long min, long max) {
        this.min = min;
        this.max = max;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public String toString() {
        return "(" + min + " .. " + max + ")";
    }
}
