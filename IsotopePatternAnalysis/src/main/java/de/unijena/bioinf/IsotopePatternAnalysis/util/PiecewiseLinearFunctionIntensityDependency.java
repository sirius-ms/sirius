package de.unijena.bioinf.IsotopePatternAnalysis.util;

/**

 */
public class PiecewiseLinearFunctionIntensityDependency extends FixedBagIntensityDependency {

    public PiecewiseLinearFunctionIntensityDependency(double[] bags, double[] values) {
        super(bags, values);
    }

    @Override
    public double getValueAt(double intensity) {
        int i;
        for (i=0; i < bags.length; ++i) {
            if (intensity > bags[i]) {
                break;
            }
        }
        if (i >= bags.length) return values[bags.length-1];
        if (i==0) return values[0];
        final double p = (intensity-bags[i])/(bags[i-1] - bags[i]);
        return (p*values[i-1] + (1-p)*values[i]);
    }
}
