package de.unijena.bioinf.IsotopePatternAnalysis.util;

public class LinearIntensityDependency implements IntensityDependency {

    private double zeroIntensity, fullIntensity;


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
}
