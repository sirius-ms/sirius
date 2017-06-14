package de.unijena.bioinf.sirius.gui.msviewer.data;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class SiriusSingleSpectrumModel implements MSViewerDataModel {

    protected final Spectrum<? extends Peak> spectrum;
    protected double scale;

    public SiriusSingleSpectrumModel(Spectrum<? extends Peak> spectrum) {
        this.spectrum = spectrum;
        this.scale = Spectrums.getMaximalIntensity(spectrum);
    }

    @Override
    public int getSize() {
        return spectrum.size();
    }

    @Override
    public double getMass(int index) {
        return spectrum.getMzAt(index);
    }

    @Override
    public double getRelativeIntensity(int index) {
        return spectrum.getIntensityAt(index)/scale;
    }

    @Override
    public double getSignalNoise(int index) {
        return 0;
    }

    @Override
    public double getAbsoluteIntensity(int index) {
        return spectrum.getIntensityAt(index);
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
        return Spectrums.mostIntensivePeakWithin(spectrum, mass, new Deviation(1,0.1));
    }

    @Override
    public int findIndexOfPeak(double mass, double tolerance) {
        return Spectrums.mostIntensivePeakWithin(spectrum, mass, new Deviation(1,tolerance));
    }
}
