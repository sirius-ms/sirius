package de.unijena.bioinf.ChemistryBase.utils;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpectrumsExtractMostIntenseWindowTest {

    @Test
    void testExtractMostIntensivePeaks_NullSpectrum() {
        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(null, 2, 0.1);
        assertNull(result);
    }

    @Test
    void testExtractMostIntensivePeaks_EmptySpectrum() {
        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(new SimpleMutableSpectrum(), 2, 0.1);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractMostIntensivePeaks_SinglePeak() {
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        spectrum.addPeak(10.0, 100.0);

        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(spectrum, 1, 0.1);

        assertEquals(1, result.size());
        assertEquals(10.0, result.getMzAt(0));
        assertEquals(100.0, result.getIntensityAt(0));
    }

    @Test
    void testExtractMostIntensivePeaks_MultiplePeaksWithinWindow() {
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        spectrum.addPeak(10.0, 100.0);
        spectrum.addPeak(10.05, 110.0);
        spectrum.addPeak(10.1, 90.0);

        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(spectrum, 2, 0.1);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(peak -> peak.getMass() == 10.05));
        assertTrue(result.stream().anyMatch(peak -> peak.getMass() == 10.0));
    }

    @Test
    void testExtractMostIntensivePeaks_MultiplePeaksOutsideWindow() {
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        spectrum.addPeak(10.0, 100.0);
        spectrum.addPeak(10.2, 90.0);
        spectrum.addPeak(10.4, 110.0);

        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(spectrum, 1, 0.1);

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(peak -> peak.getMass() == 10.4));
        assertTrue(result.stream().anyMatch(peak -> peak.getMass() == 10.0));
        assertTrue(result.stream().anyMatch(peak -> peak.getMass() == 10.2));
    }

    @Test
    void testExtractMostIntensivePeaks_SameIntensityPeaks() {
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        spectrum.addPeak(10.0, 100.0);
        spectrum.addPeak(10.1, 100.0);

        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(spectrum, 1, 0.1);

        assertEquals(1, result.size());
        assertEquals(10.0, result.getMzAt(0));
        assertEquals(100.0, result.getIntensityAt(0));
    }

}
