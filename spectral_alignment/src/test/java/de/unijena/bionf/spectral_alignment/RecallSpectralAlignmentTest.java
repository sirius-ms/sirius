package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class RecallSpectralAlignmentTest {

    @Test
    public void testEmptySpectra(){
        OrderedSpectrum<Peak> measuredSpectrum = Spectrums.empty();
        OrderedSpectrum<Peak> predictedSpectrum = Spectrums.empty();
        RecallSpectralAlignment recallSpectralAlignment = new RecallSpectralAlignment(new Deviation(5,0.01));
        SpectralSimilarity spectralSimilarity = recallSpectralAlignment.score(measuredSpectrum, predictedSpectrum);
        assertEquals(0d, spectralSimilarity.similarity, 0);
        assertEquals(0, spectralSimilarity.sharedPeaks);
        assertTrue(recallSpectralAlignment.getMatchedMsrdPeaks(measuredSpectrum, predictedSpectrum).isEmpty());
    }

    @Test
    public void testSingleMatches(){
        double[] measuredMzVals = new double[]{50d, 75d, 100d, 125d, 150d, 175d, 180d, 200d};
        double[] measuredIntensities = new double[]{1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d};
        OrderedSpectrum<Peak> measuredSpectrum = new SimpleSpectrum(measuredMzVals, measuredIntensities);

        double[] predMzVals = new double[]{49.9998, 75.0006, 80.3423, 95.29484, 125.001, 125.01, 149.9989, 175.1, 200.001};
        double[] predIntensities = new double[]{1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d};
        OrderedSpectrum<Peak> predictedSpectrum = new SimpleSpectrum(predMzVals, predIntensities);

        // Test score:
        RecallSpectralAlignment recallSpectralAlignment = new RecallSpectralAlignment(new Deviation(10, 0.001));
        SpectralSimilarity spectralSimilarity = recallSpectralAlignment.score(measuredSpectrum, predictedSpectrum);
        assertEquals(5d / 8, spectralSimilarity.similarity, 0);

        // Test matched peaks:
        List<Peak> matchedMeasuredPeaks = recallSpectralAlignment.getMatchedMsrdPeaks(measuredSpectrum, predictedSpectrum);
        Double[] mzValuesOfMatchedPeaks = matchedMeasuredPeaks.stream().map(Peak::getMass).sorted().toArray(Double[]::new);

        assertEquals(5, matchedMeasuredPeaks.size());
        assertArrayEquals(new Double[]{50d, 75d, 125d, 150d, 200d}, mzValuesOfMatchedPeaks);
    }

    @Test
    public void testMultipleMatches(){
        double[] measuredMzVals = new double[]{50d, 75d, 75.0002, 100d, 125d, 150d, 175d, 175.0001, 180d, 200d};
        double[] measuredIntensities = new double[]{1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d};
        OrderedSpectrum<Peak> measuredSpectrum = new SimpleSpectrum(measuredMzVals, measuredIntensities);

        double[] predictedMzVals = new double[]{49.9998, 50.0004, 74.9998, 75d, 75.0005, 115d, 120d, 174.999, 175.001, 183d, 200.001};
        double[] predictedIntensities = new double[]{1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d};
        OrderedSpectrum<Peak> predictedSpectrum = new SimpleSpectrum(predictedMzVals, predictedIntensities);

        RecallSpectralAlignment recallSpectralAlignment = new RecallSpectralAlignment(new Deviation(10, 0.001));
        SpectralSimilarity spectralSimilarity = recallSpectralAlignment.score(measuredSpectrum, predictedSpectrum);
        assertEquals(0.6, spectralSimilarity.similarity, 0);

        List<Peak> matchedMeasuredPeaks = recallSpectralAlignment.getMatchedMsrdPeaks(measuredSpectrum, predictedSpectrum);
        Double[] mzValuesOfMatchedPeaks = matchedMeasuredPeaks.stream().map(Peak::getMass).sorted().toArray(Double[]::new);

        assertEquals(6, matchedMeasuredPeaks.size());
        assertArrayEquals(new Double[]{50d, 75d, 75.0002, 175d, 175.0001, 200d}, mzValuesOfMatchedPeaks);
    }

    @Test
    public void testNoMatch(){
        double[] measuredMzVals = new double[]{50d, 75d, 100d, 125d, 150d, 175d, 180d, 200d};
        double[] measuredIntensities = new double[]{1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d};
        OrderedSpectrum<Peak> measuredSpectrum = new SimpleSpectrum(measuredMzVals, measuredIntensities);

        double[] predictedMzVals = new double[]{60d, 80d, 110d, 140d, 160d, 178d, 190d, 210d};
        double[] predictedIntensities = measuredIntensities;
        OrderedSpectrum<Peak> predictedSpectrum = new SimpleSpectrum(predictedMzVals, predictedIntensities);

        RecallSpectralAlignment recallSpectralAlignment = new RecallSpectralAlignment(new Deviation(10, 0.001));
        SpectralSimilarity spectralSimilarity = recallSpectralAlignment.score(measuredSpectrum, predictedSpectrum);
        assertEquals(0d, spectralSimilarity.similarity, 0);

        List<Peak> matchedMeasuredPeaks = recallSpectralAlignment.getMatchedMsrdPeaks(measuredSpectrum, predictedSpectrum);
        assertTrue(matchedMeasuredPeaks.isEmpty());
    }
}
