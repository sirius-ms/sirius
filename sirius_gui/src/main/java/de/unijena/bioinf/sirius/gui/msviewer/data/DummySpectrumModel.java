package de.unijena.bioinf.sirius.gui.msviewer.data;

public class DummySpectrumModel implements MSViewerDataModel{
    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public double getMass(int index) {
        return 0;
    }

    @Override
    public double getRelativeIntensity(int index) {
        return 0;
    }

    @Override
    public double getAbsoluteIntensity(int index) {
        return 0;
    }

    @Override
    public String getMolecularFormula(int index) {
        return null;
    }

    @Override
    public PeakInformation getInformations(int index) {
        return null;
    }

    @Override
    public boolean isMarked(int index) {
        return false;
    }

    @Override
    public boolean isImportantPeak(int index) {
        return false;
    }

    @Override
    public boolean isUnimportantPeak(int index) {
        return false;
    }

    @Override
    public boolean isPlusZeroPeak(int index) {
        return false;
    }

    @Override
    public boolean isIsotope(int index) {
        return false;
    }

    @Override
    public int[] getIsotopePeaks(int index) {
        return new int[0];
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public int getIndexWithMass(double mass) {
        return 0;
    }

    @Override
    public int findIndexOfPeak(double mass, double tolerance) {
        return 0;
    }
}
