package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpectralAlignmentTest {

    private static SimpleSpectrum left, right;
    private static double precursorMzLeft, precursorMzRight;

    private final IntensityWeightedSpectralAlignment intensityScorer = new IntensityWeightedSpectralAlignment(new Deviation(10));
    private final GaussianSpectralAlignment gaussianScorer = new GaussianSpectralAlignment(new Deviation(10));
    private final ModifiedCosine modifiedCosineScorer = new ModifiedCosine(new Deviation(10));

    private final AbstractSpectralAlignment[] scorers = {intensityScorer, gaussianScorer, modifiedCosineScorer};

    @BeforeClass
    public static void setUp() {
        final SimpleMutableSpectrum A = new SimpleMutableSpectrum();
        A.addPeak(1, 1);
        A.addPeak(5, 1);
        A.addPeak(8, 1);
        A.addPeak(15, 0.5);

        left = new SimpleSpectrum(A);
        precursorMzLeft = 20;

        final SimpleMutableSpectrum B = new SimpleMutableSpectrum();
        B.addPeak(1, 1);
        B.addPeak(8, 1);
        B.addPeak(15, 1);
        B.addPeak(18, 1);
        B.addPeak(20, 1);
        B.addPeak(25, 1);

        right = new SimpleSpectrum(B);
        precursorMzRight = 30;
    }

    @Test
    public void testModifiedCosine() {
        ModifiedCosine modifiedCosine = new ModifiedCosine(new Deviation(10));
        SpectralSimilarity spectralSimilarity = modifiedCosine.score(left, right, precursorMzLeft, precursorMzRight);

        assertEquals(3.5, spectralSimilarity.similarity, 1e-9);
        assertEquals(4, spectralSimilarity.shardPeaks);

        Map<Integer, Integer> expectedAssignment = new HashMap<>();
        expectedAssignment.put(0, 0);  // ASSIGN 1.0 WITH 1.0
        expectedAssignment.put(1, 2);  // ASSIGN 5.0 WITH 15.0
        expectedAssignment.put(2, 1);  // ASSIGN 8.0 WITH 8.0
        expectedAssignment.put(3, 5);  // ASSIGN 15.0 WITH 25.0

        Map<Integer, Integer> actualAssignment = new HashMap<>();

        for (int i=0; i < modifiedCosine.getAssignment().length; i+=2) {
            actualAssignment.put(modifiedCosine.getAssignment()[i], modifiedCosine.getAssignment()[i+1]);
        }

        assertEquals(expectedAssignment, actualAssignment);
    }

    @Test
    public void testGaussian() {
        GaussianSpectralAlignment gaussianAlignment = new GaussianSpectralAlignment(new Deviation(10));
        SpectralSimilarity spectralSimilarity = gaussianAlignment.score(left, right);

        assertEquals(49735.919716217, spectralSimilarity.similarity, 1e-9);
        assertEquals(3, spectralSimilarity.shardPeaks);
    }

    @Test
    public void testIntensity() {
        IntensityWeightedSpectralAlignment intensityAlignment = new IntensityWeightedSpectralAlignment(new Deviation(10));
        SpectralSimilarity spectralSimilarity = intensityAlignment.score(left, right);

        assertEquals(2.5, spectralSimilarity.similarity, 1e-9);
        assertEquals(3, spectralSimilarity.shardPeaks);
    }

    @Test
    public void testEmptySpectra() {
        for (AbstractSpectralAlignment scorer : scorers) {
            SpectralSimilarity similarity = scorer.score(SimpleSpectrum.empty(), SimpleSpectrum.empty(), 0, 0);
            assertEquals(0, similarity.similarity, 1e-9);
            assertEquals(0, similarity.shardPeaks);
        }
    }

    @Test
    public void testNormalized() {
        for (AbstractSpectralAlignment scorer : scorers) {
            SpectralSimilarity similarity = normalized(scorer, left, right, precursorMzLeft, precursorMzRight);
            assertTrue(similarity.similarity >= 0);
            assertTrue(similarity.similarity <= 1);
        }
    }


    @Test
    public void testSelfSimilarity() {
        testSelfSimilarity(left, precursorMzLeft);
        testSelfSimilarity(right, precursorMzRight);
    }

    private void testSelfSimilarity(SimpleSpectrum spectrum, double precursorMz) {
        for (AbstractSpectralAlignment scorer : scorers) {
            SpectralSimilarity similarity = normalized(scorer, spectrum, spectrum, precursorMz, precursorMz);
            assertEquals(1, similarity.similarity, 1e-9);
            assertEquals(spectrum.size(), similarity.shardPeaks);
        }
    }

    private SpectralSimilarity normalized(AbstractSpectralAlignment scorer, SimpleSpectrum left, SimpleSpectrum right, double leftMz, double rightMz) {
        CosineQueryUtils utils = new CosineQueryUtils(scorer);
        CosineQuerySpectrum leftQuery = utils.createQuery(left, leftMz);
        CosineQuerySpectrum rightQuery = utils.createQuery(right, rightMz);
        return utils.cosineProduct(leftQuery, rightQuery);
    }
}