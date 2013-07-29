package de.unijena.bioinf.IsotopePatternAnalysis.util;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

public class LinearIntensityDependency implements IntensityDependency {

    private double zeroIntensity, fullIntensity;

    public LinearIntensityDependency(){
        this.zeroIntensity = 1;
        this.fullIntensity = 1;
    }

    public LinearIntensityDependency(double fullIntensity, double zeroIntensity) {
        this.zeroIntensity = zeroIntensity;
        this.fullIntensity = fullIntensity;
    }



    public double getZeroIntensity() {
        return zeroIntensity;
    }

    public void setZeroIntensity(double zeroIntensity) {
        this.zeroIntensity = zeroIntensity;
    }

    public double getFullIntensity() {
        return fullIntensity;
    }

    public void setFullIntensity(double fullIntensity) {
        this.fullIntensity = fullIntensity;
    }

    @Override
    public double getValueAt(double intensity) {
        return fullIntensity*(intensity) + zeroIntensity*(1-intensity);
    }

    @Override
    public <G, D, L> LinearIntensityDependency readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final double zeroIntensity = document.getDoubleFromDictionary(dictionary, "zeroIntensity");
        final double fullIntensity = document.getDoubleFromDictionary(dictionary, "fullIntensity");
        return new LinearIntensityDependency(fullIntensity, zeroIntensity);
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "zeroIntensity", zeroIntensity);
        document.addToDictionary(dictionary, "fullIntensity", fullIntensity);
    }
}
