package de.unijena.bioinf.lcms.datatypes;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.lcms.centroiding.CentroidIndividualSpectraStrategy;
import org.h2.mvstore.WriteBuffer;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * A spectrum must read back out of the store exactly as it went in.
 * <p>
 * Intensities are stored as floats on purpose, so the only way to hold that is for a spectrum to carry float
 * precision intensities from the moment it is built. Where it does not, a resident page and an evicted one hand
 * out different numbers for the same id, and two intensities that were distinct become tied - which silently moves
 * every "most intensive peak within" search onto a different peak.
 */
public class SpectrumStoredPrecisionTest {

    private static final Deviation DEV = new Deviation(8);

    private static SimpleSpectrum roundTrip(SimpleSpectrum s) {
        final SpectrumDatatype dt = new SpectrumDatatype();
        final WriteBuffer wb = new WriteBuffer();
        dt.write(wb, s);
        final ByteBuffer bb = wb.getBuffer();
        bb.flip();
        return dt.read(bb);
    }

    private static void assertSurvivesTheStore(SimpleSpectrum s) {
        final SimpleSpectrum back = roundTrip(s);
        assertEquals("peak count", s.size(), back.size());
        for (int k = 0; k < s.size(); ++k) {
            assertEquals("m/z at " + k, s.getMzAt(k), back.getMzAt(k), 0d);
            assertEquals("intensity at " + k, s.getIntensityAt(k), back.getIntensityAt(k), 0d);
        }
    }

    /** m/z must never be narrowed - a float m/z is worthless at these masses */
    @Test
    public void massesSurviveTheStoreExactly() {
        final double[] mz = {100.00000001, 557.489344704, 1000.123456789012};
        final double[] in = {1e5, 2e5, 3e5};
        final SimpleSpectrum s = new SimpleSpectrum(mz, in);
        final SimpleSpectrum back = roundTrip(s);
        for (int k = 0; k < s.size(); ++k) assertEquals(s.getMzAt(k), back.getMzAt(k), 0d);
    }

    @Test
    public void roundedIntensitiesSurviveTheStoreExactly() {
        final double[] mz = new double[300];
        final double[] in = new double[300];
        double m = 100;
        for (int i = 0; i < mz.length; ++i) {
            m += 0.37;
            mz[i] = m;
            in[i] = 1e5 + i * 3.14159e6; // magnitudes a float cannot hold exactly
        }
        SpectrumDatatype.roundIntensitiesToStoredPrecision(in);
        assertSurvivesTheStore(new SimpleSpectrum(mz, in));
    }

    /**
     * The defect itself: unrounded intensities that differ as doubles collide as floats, and the search then
     * takes the earlier peak instead of the true maximum. Rounding first makes both answers agree.
     */
    @Test
    public void tieCollapseMovesTheChosenPeakUnlessRoundedFirst() {
        final double mz = 557.4893;
        final double[] masses = {mz - 0.0005, mz + 0.0005};

        final double[] raw = {1.00000000e8, 1.00000001e8}; // strictly increasing as doubles, equal as floats
        final SimpleSpectrum unrounded = new SimpleSpectrum(masses, raw.clone());
        assertNotEquals("this is the bug: the store moves the chosen peak",
                Spectrums.mostIntensivePeakWithin(unrounded, mz, DEV),
                Spectrums.mostIntensivePeakWithin(roundTrip(unrounded), mz, DEV));

        final double[] rounded = raw.clone();
        SpectrumDatatype.roundIntensitiesToStoredPrecision(rounded);
        final SimpleSpectrum fixed = new SimpleSpectrum(masses, rounded);
        assertEquals("after rounding, residency cannot change the answer",
                Spectrums.mostIntensivePeakWithin(fixed, mz, DEV),
                Spectrums.mostIntensivePeakWithin(roundTrip(fixed), mz, DEV));
        assertSurvivesTheStore(fixed);
    }

    /** centroiding computes intensities of its own, so its output has to be at stored precision too */
    @Test
    public void centroidedSpectraAreAlreadyAtStoredPrecision() {
        final SimpleMutableSpectrum profile = new SimpleMutableSpectrum();
        // three gaussian peaks sampled densely enough for the detector to centroid them
        final double[] centres = {200.0501, 201.0534, 202.0567};
        final double[] heights = {8.3e7, 3.1e7, 5.7e6};
        for (int p = 0; p < centres.length; ++p) {
            for (int i = -12; i <= 12; ++i) {
                final double d = i * 0.0008;
                profile.addPeak(centres[p] + d, heights[p] * Math.exp(-(d * d) / (2 * 0.0015 * 0.0015)));
            }
        }
        final MSMSScan scan = MSMSScan.builder().peaks(new SimpleSpectrum(profile)).centroided(false).build();
        new CentroidIndividualSpectraStrategy().centroidMsMsScan(scan);

        final SimpleSpectrum centroided = scan.getPeaks();
        assertTrue("the detector should find peaks", centroided.size() > 0);
        for (int k = 0; k < centroided.size(); ++k) {
            final double v = centroided.getIntensityAt(k);
            assertEquals("centroided intensity " + k + " is not at stored precision", (double) (float) v, v, 0d);
        }
        assertSurvivesTheStore(centroided);
    }
}
