package de.unijena.bioinf.IsotopePatternAnalysis.util;

import java.util.Arrays;

public class FixedBagIntensityDependency implements IntensityDependency {

    protected double[] bags;
    protected double[] values;

    public FixedBagIntensityDependency(double[] bags, double[] values) {
        this.bags = Arrays.copyOf(bags, bags.length);
        this.values = Arrays.copyOf(values, values.length);
    }

    public double[] getBags() {
        return bags;
    }

    public double[] getValues() {
        return values;
    }

    @Override
    public double getValueAt(double intensity) {
        double minDiff = Double.POSITIVE_INFINITY;
        int minIndex = 0;
        for (int i=0; i < bags.length; ++i) {
            final double diff = Math.abs(bags[i] - intensity);
            if (diff < minDiff) {
                minDiff = diff;
                minIndex = i;
            }
        }
        return values[minIndex];
    }
}
