package de.unijena.bioinf.IsotopePatternAnalysis.util;

public class FixedIntensity implements IntensityDependency {
    private final double value;

    public FixedIntensity(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public double getValueAt(double intensity) {
        return value;
    }
}
