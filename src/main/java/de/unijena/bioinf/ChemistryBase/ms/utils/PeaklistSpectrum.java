package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.util.Iterator;
import java.util.List;

/**
 * Simple and efficient way to wrap a list of peaks into a spectrum object, such that it can be
 * used for {{@link Spectrums}} methods
 * @param <P>
 */
public class PeaklistSpectrum<P extends Peak> implements Spectrum<P> {

    private final List<P> peaks;

    public PeaklistSpectrum(List<P> peaks) {
        this.peaks = peaks;
    }

    @Override
    public double getMzAt(int index) {
        return peaks.get(index).getMass();
    }

    @Override
    public double getIntensityAt(int index) {
        return peaks.get(index).getIntensity();
    }

    @Override
    public P getPeakAt(int index) {
        return peaks.get(index);
    }

    @Override
    public int size() {
        return peaks.size();
    }

    @Override
    public Iterator<P> iterator() {
        return peaks.iterator();
    }

    @Override
    public <T> T getProperty(String name) {
        return null;
    }

    @Override
    public <T> T getProperty(String name, T defaultValue) {
        return null;
    }
}
