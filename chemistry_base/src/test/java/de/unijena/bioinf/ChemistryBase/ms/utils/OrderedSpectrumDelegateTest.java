package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import org.junit.Test;

import static org.junit.Assert.*;

public class OrderedSpectrumDelegateTest {

    @Test
    public void testConstructor() {
        new OrderedSpectrumDelegate<>(SimpleSpectrum.empty());
        new OrderedSpectrumDelegate<>(Spectrums.wrap(new double[] {1d}, new double[] {1d}));
        new OrderedSpectrumDelegate<>(Spectrums.wrap(new double[] {1d, 1d}, new double[] {1d, 1d}));
        new OrderedSpectrumDelegate<>(Spectrums.wrap(new double[] {1d, 2d}, new double[] {1d, 1d}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnordered() {
        new OrderedSpectrumDelegate<>(Spectrums.wrap(new double[] {2d, 1d}, new double[] {1d, 1d}));
    }

    @Test
    public void testDelegation() {
        Spectrum<Peak> innerSpectrum = Spectrums.wrap(new double[]{1d, 2d}, new double[]{1d, 1d});
        OrderedSpectrumDelegate<Peak> spectrum = new OrderedSpectrumDelegate<>(innerSpectrum);
        assertTrue(Spectrums.haveEqualPeaks(innerSpectrum, spectrum));
    }
}