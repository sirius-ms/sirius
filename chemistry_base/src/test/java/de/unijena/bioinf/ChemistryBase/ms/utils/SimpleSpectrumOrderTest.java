package de.unijena.bioinf.ChemistryBase.ms.utils;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * A {@link SimpleSpectrum} declares itself an {@link de.unijena.bioinf.ChemistryBase.ms.OrderedSpectrum}, so every
 * lookup in {@link Spectrums} reaches its peaks by binary search. It therefore has to actually be sorted by mass.
 * <p>
 * The constructor that copies its input sorted the input rather than the copy it had just taken, so the spectrum kept
 * an unsorted copy while still advertising itself as ordered. A binary search over such a spectrum silently misses
 * peaks that are there, which is invisible - no exception, just a wrong or absent answer.
 */
public class SimpleSpectrumOrderTest {

    /** the copying constructor is protected, so a subclass stands in for the callers that pass the flag through */
    private static class Subclass extends SimpleSpectrum {
        Subclass(double[] masses, double[] intensities, boolean noCopyAndOrder) {
            super(masses, intensities, noCopyAndOrder);
        }
    }

    @Test
    public void aCopiedSpectrumIsSortedByMass() {
        final double[] masses = {500.5, 100.1, 300.3, 200.2, 400.4};
        final double[] intensities = {5d, 1d, 3d, 2d, 4d};

        final SimpleSpectrum spectrum = new Subclass(masses, intensities, false);

        // ascending in mass, and both arrays permuted together so every peak keeps its own intensity
        final double[] mz = new double[spectrum.size()], intensity = new double[spectrum.size()];
        for (int k = 0; k < spectrum.size(); ++k) {
            mz[k] = spectrum.getMzAt(k);
            intensity[k] = spectrum.getIntensityAt(k);
        }
        assertArrayEquals(new double[]{100.1, 200.2, 300.3, 400.4, 500.5}, mz, 0d);
        assertArrayEquals(new double[]{1d, 2d, 3d, 4d, 5d}, intensity, 0d);
    }

    /** and a binary search finds every peak it contains, which is the property the marker interface promises */
    @Test
    public void everyPeakOfACopiedSpectrumIsFoundByBinarySearch() {
        final double[] masses = {500.5, 100.1, 300.3, 200.2, 400.4};
        final double[] intensities = {5d, 1d, 3d, 2d, 4d};

        final SimpleSpectrum spectrum = new Subclass(masses, intensities, false);

        for (double mz : masses) {
            assertTrue("binary search lost the peak at " + mz,
                    Spectrums.mostIntensivePeakWithin(spectrum, mz,
                            new de.unijena.bioinf.ChemistryBase.ms.Deviation(1, 1e-6)) >= 0);
        }
    }

    /** copying must not reorder the caller's arrays behind its back either */
    @Test
    public void theCallersArraysAreLeftAlone() {
        final double[] masses = {500.5, 100.1, 300.3};
        final double[] intensities = {5d, 1d, 3d};

        new Subclass(masses, intensities, false);

        assertArrayEquals(new double[]{500.5, 100.1, 300.3}, masses, 0d);
        assertArrayEquals(new double[]{5d, 1d, 3d}, intensities, 0d);
    }
}
