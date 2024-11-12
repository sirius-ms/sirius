package de.unijena.bioinf.ChemistryBase.utils;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpectrumsExtractMostIntenseTest {

    @Test
    void testExtractMostIntensivePeaks_NullSpectrum() {
        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(null, 5);
        assertNull(result);
    }

    @Test
    void testExtractMostIntensivePeaks_EmptySpectrum() {
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(spectrum, 5);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractMostIntensivePeaks_FewerPeaksThanRequested() {
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        spectrum.addPeak(10.0, 1.0);
        spectrum.addPeak(20.0, 2.0);

        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(spectrum, 5);
        assertEquals(2, result.size());
        assertEquals(10.0, result.getMzAt(0));
        assertEquals(1.0, result.getIntensityAt(0));
        assertEquals(20.0, result.getMzAt(1));
        assertEquals(2.0, result.getIntensityAt(1));
    }

    @Test
    void testExtractMostIntensivePeaks_MorePeaksThanRequested() {
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        spectrum.addPeak(10.0, 1.0);
        spectrum.addPeak(20.0, 4.0);
        spectrum.addPeak(30.0, 3.0);
        spectrum.addPeak(40.0, 2.0);

        SimpleSpectrum result = Spectrums.extractMostIntensivePeaks(spectrum, 2);
        assertEquals(2, result.size());
        assertEquals(20.0, result.getMzAt(0));
        assertEquals(4.0, result.getIntensityAt(0));
        assertEquals(30.0, result.getMzAt(1));
        assertEquals(3.0, result.getIntensityAt(1));
    }
}