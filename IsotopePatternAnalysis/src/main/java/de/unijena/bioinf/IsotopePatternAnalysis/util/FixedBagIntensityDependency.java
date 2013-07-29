package de.unijena.bioinf.IsotopePatternAnalysis.util;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

import java.util.Arrays;

public class FixedBagIntensityDependency implements IntensityDependency {

    protected double[] bags;
    protected double[] values;

    public FixedBagIntensityDependency(){
        bags = new double[0];
        values = new double[0];
    }
    public FixedBagIntensityDependency(double[] bags, double[] values) {
        if (bags.length != values.length) throw new IllegalArgumentException("array sizes differ");
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

    @Override
    public <G, D, L> FixedBagIntensityDependency readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
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
        return new FixedBagIntensityDependency(bags, values);
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L bagsList = document.newList();
        for (double bag : bags) {
            document.addToList(bagsList, bag);
        }
        final L valuesList = document.newList();
        for (double value : values) {
            document.addToList(valuesList, value);
        }
        document.addListToDictionary(dictionary, "bags", bagsList);
        document.addListToDictionary(dictionary, "values", valuesList);
    }
}
