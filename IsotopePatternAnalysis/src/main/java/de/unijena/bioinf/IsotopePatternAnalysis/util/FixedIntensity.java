package de.unijena.bioinf.IsotopePatternAnalysis.util;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

public class FixedIntensity implements IntensityDependency {
    private double value;

    public FixedIntensity(){
        this.value = 1;
    }
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

    @Override
    public <G, D, L> FixedIntensity readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final double value = document.getDoubleFromDictionary(dictionary, "value");
        return new FixedIntensity(value);
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "value", value);
    }
}
