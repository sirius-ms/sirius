package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import gnu.trove.list.array.TDoubleArrayList;

public abstract class BasicMutableSpectrum<P extends Peak> extends AbstractSpectrum<P> implements MutableSpectrum<P> {

    protected TDoubleArrayList masses;
    protected TDoubleArrayList intensities;

    public <T extends Peak, S extends Spectrum<T>> BasicMutableSpectrum(S immutable) {
        this.masses = new TDoubleArrayList(Spectrums.copyMasses(immutable));
        this.intensities = new TDoubleArrayList(Spectrums.copyIntensities(immutable));
    }

    public BasicMutableSpectrum() {
        this.masses = new TDoubleArrayList();
        this.intensities = new TDoubleArrayList();
    }

    public BasicMutableSpectrum(int size) {
        this.masses = new TDoubleArrayList(size);
        this.intensities = new TDoubleArrayList(size);
    }

    @Override
    public int size() {
        return masses.size();
    }

    @Override
    public void addPeak(P peak) {
        masses.add(peak.getMass());
        intensities.add(peak.getIntensity());
    }

    @Override
    public void addPeak(double mz, double intensity) {
        masses.add(mz);
        intensities.add(intensity);
    }

    @Override
    public void setPeakAt(int index, P peak) {
        masses.set(index, peak.getMass());
        intensities.set(index, peak.getIntensity());
    }

    @Override
    public P removePeakAt(int index) {
        final P p = getPeakAt(index);
        masses.remove(index, 1);
        intensities.remove(index, 1);
        return p;
    }

    @Override
    public double getMzAt(int index) {
        return masses.get(index);
    }

    @Override
    public double getIntensityAt(int index) {
        return intensities.get(index);
    }

    @Override
    public void setMzAt(int index, double mz) {
        masses.set(index, mz);
    }

    @Override
    public void setIntensityAt(int index, double intensity) {
        intensities.set(index, intensity);
    }

    @Override
    public void swap(int index1, int index2) {
        final double mz = masses.get(index1);
        final double in = intensities.get(index1);
        masses.set(index1, masses.get(index2));
        intensities.set(index1, intensities.get(index2));
        masses.set(index2, mz);
        intensities.set(index2, in);
    }

}
