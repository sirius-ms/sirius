package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class OrderedSpectrumDelegate<T extends Peak> implements OrderedSpectrum<T> {

    private final Spectrum<T> delegate;

    public OrderedSpectrumDelegate(Spectrum<T> delegate) {
        if (delegate instanceof OrderedSpectrum<T> || Spectrums.isMassOrderedSpectrum(delegate)) {
            this.delegate = delegate;
        } else {
            throw new IllegalArgumentException("Expected an ordered spectrum, got " + delegate);
        }
    }

    @Override
    public double getMzAt(int index) {
        return delegate.getMzAt(index);
    }

    @Override
    public double getIntensityAt(int index) {
        return delegate.getIntensityAt(index);
    }

    @Override
    public T getPeakAt(int index) {
        return delegate.getPeakAt(index);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }
}
