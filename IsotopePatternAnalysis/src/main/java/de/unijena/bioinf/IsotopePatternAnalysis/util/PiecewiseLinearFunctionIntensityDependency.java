package de.unijena.bioinf.IsotopePatternAnalysis.util;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

/**
 * A piecewise linear function F: Intensity -> R.
 * Each

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

    @Override
    public <G, D, L> PiecewiseLinearFunctionIntensityDependency readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L bagsList = document.getListFromDictionary(dictionary, "bags");
        final double[] bags = new double[document.sizeOfList(bagsList)];
        for (int i = 0; i < bags.length; i++) {
            bags[i] = document.getDoubleFromList(bagsList, i);
        }
        final L valuesList = document.getListFromDictionary(dictionary, "values");
        final double[] values = new double[document.sizeOfList(valuesList)];
        for (int i = 0; i < values.length; i++) {
            values[i] = document.getDoubleFromList(valuesList, i);
        }
        return new PiecewiseLinearFunctionIntensityDependency(bags, values);
    }
}
