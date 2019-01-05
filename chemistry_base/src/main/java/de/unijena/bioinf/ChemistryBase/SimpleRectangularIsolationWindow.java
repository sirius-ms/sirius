package de.unijena.bioinf.ChemistryBase;

import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Dataset;

public class SimpleRectangularIsolationWindow extends IsolationWindow {

    private double leftBorder;
    private double rightBorder;

    public SimpleRectangularIsolationWindow(double leftBorder, double rightBorder) {
        super(rightBorder-leftBorder);
        this.leftBorder = leftBorder;
        this.rightBorder = rightBorder;
    }

    @Override
    public double getIntensityRatio(double precursorMz, double targetMz) {
        double diff = targetMz-precursorMz;
        if (diff<leftBorder || diff>rightBorder) return 0d;
        return 1d;
    }

    @Override
    public double getIntensity(double targetIntensity, double precursorMz, double targetMz) {
        double diff = targetMz-precursorMz;
        if (diff<leftBorder || diff>rightBorder) return 0d;
        return targetIntensity;
    }

    @Override
    protected void estimateDistribution(IsotopeRatioInformation intensityRatios, Ms2Dataset dataset) {
        throw new NoSuchMethodError("This SimpleRectangularIsolationWindow is immutable.");
    }

    @Override
    public double getLeftBorder() {
        return leftBorder;
    }

    @Override
    public double getRightBorder() {
        return rightBorder;
    }

    @Override
    public double getEstimatedWindowSize() {
        return rightBorder-leftBorder;
    }

    @Override
    public double getEstimatedMassShift() {
        return (rightBorder-leftBorder)/2+leftBorder;
    }
}
