import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.cmlSpectrumPrediction.RecallSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class RecallSpectralAlignmentTest {

    @Test
    public void testEmptySpectra(){
        OrderedSpectrum<Peak> measuredSpectrum = Spectrums.empty();
        OrderedSpectrum<Peak> predictedSpectrum = Spectrums.empty();
        RecallSpectralAlignment recallSpectralAlignment = new RecallSpectralAlignment(new Deviation(5,0.01));
        SpectralSimilarity spectralSimilarity = recallSpectralAlignment.score(predictedSpectrum, measuredSpectrum);
        assertEquals(0d, spectralSimilarity.similarity, 0);
        assertEquals(0, spectralSimilarity.shardPeaks);
        assertTrue(recallSpectralAlignment.getPreviousMatchedMeasuredPeaks().isEmpty());
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
        SpectralSimilarity spectralSimilarity = recallSpectralAlignment.score(predictedSpectrum, measuredSpectrum);
        assertEquals(5d / 8, spectralSimilarity.similarity, 0);

        // Test matched peaks:
        List<Peak> matchedMeasuredPeaks = recallSpectralAlignment.getPreviousMatchedMeasuredPeaks();
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
        SpectralSimilarity spectralSimilarity = recallSpectralAlignment.score(predictedSpectrum, measuredSpectrum);
        assertEquals(0.6, spectralSimilarity.similarity, 0);

        List<Peak> matchedMeasuredPeaks = recallSpectralAlignment.getPreviousMatchedMeasuredPeaks();
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
        SpectralSimilarity spectralSimilarity = recallSpectralAlignment.score(predictedSpectrum, measuredSpectrum);
        assertEquals(0d, spectralSimilarity.similarity, 0);

        List<Peak> matchedMeasuredPeaks = recallSpectralAlignment.getPreviousMatchedMeasuredPeaks();
        assertTrue(matchedMeasuredPeaks.isEmpty());
    }

    @Test
    public void testMultipleMethodCallings(){
        RecallSpectralAlignment recallSpectralAlignment = new RecallSpectralAlignment(new Deviation(10, 0.001));
        double[][] measuredMzVals = new double[][]{
                {75d, 105d, 135d, 165d, 215d},
                {60d, 90d, 120d, 150d, 200d}};
        double[][] predictedMzVals = new double[][]{
                {75d, 105d, 135d, 165d, 215d},
                {40d, 95, 100d, 149.9998, 200.003}
        };
        double[] intensities = new double[]{1d, 1d, 1d, 1d, 1d};

        // 1. First call of recallSpectralAlignment.score(predictedSpectrum, measuredSpectrum):
        OrderedSpectrum<Peak> measuredSpectrum = new SimpleSpectrum(measuredMzVals[0], intensities);
        OrderedSpectrum<Peak> predictedSpectrum = new SimpleSpectrum(predictedMzVals[0], intensities);
        SpectralSimilarity spectralSimilarity = recallSpectralAlignment.score(predictedSpectrum, measuredSpectrum);
        List<Peak> matchedMeasuredPeaks = recallSpectralAlignment.getPreviousMatchedMeasuredPeaks();
        assertEquals(1d, spectralSimilarity.similarity, 0d);
        assertEquals(5, matchedMeasuredPeaks.size());

        // 2. Second call of recallSpectralAlignment.score(predictedSpectrum, measuredSpectrum):
        measuredSpectrum = new SimpleSpectrum(measuredMzVals[1], intensities);
        predictedSpectrum = new SimpleSpectrum(predictedMzVals[1], intensities);
        spectralSimilarity = recallSpectralAlignment.score(predictedSpectrum, measuredSpectrum);
        matchedMeasuredPeaks = recallSpectralAlignment.getPreviousMatchedMeasuredPeaks();
        assertEquals(0.2, spectralSimilarity.similarity, 0d);
        assertEquals(1, matchedMeasuredPeaks.size());
    }

}
