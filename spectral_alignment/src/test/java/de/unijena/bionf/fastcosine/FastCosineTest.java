package de.unijena.bionf.fastcosine;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.sirius.annotations.NoiseThresholdSettings;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FastCosineTest {

    private FastCosine fastCosine;
    private Deviation defaultDeviation;
    private NoiseThresholdSettings defaultNoiseSettings;

    @BeforeEach
    void setUp() {
        defaultDeviation = new Deviation(15);  // 15 ppm default
        defaultNoiseSettings = new NoiseThresholdSettings(0.001, 60, NoiseThresholdSettings.BASE_PEAK.NOT_PRECURSOR, 0);
        fastCosine = new FastCosine(defaultDeviation, true, defaultNoiseSettings);
    }

    /**
     * Creates a simple spectrum with given mz and intensity values
     */
    private SimpleSpectrum createSpectrum(double[] mzValues, double[] intensityValues) {

        return new SimpleSpectrum(mzValues, intensityValues);
    }

    /**
     * Creates a SearchPreparedSpectrum directly without normalization
     */
    private SearchPreparedSpectrum createSearchPreparedSpectrum(double parentMass, double[] mzValues, double[] intensityValues) {
        float[] intensities = new float[intensityValues.length];
        double norm = 0;
        for (int i = 0; i < intensityValues.length; i++) {
            intensities[i] = (float) Math.sqrt(intensityValues[i]);
            norm += intensities[i] * intensities[i];
        }
        norm = Math.sqrt(norm);
        for (int i = 0; i < intensities.length; i++) {
            intensities[i] /= norm;
        }
        return new SearchPreparedSpectrum(parentMass, 1.0f, mzValues, intensities);
    }

    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Test FastCosine Constructor with Custom Parameters")
        void testCustomConstructor() {
            Deviation customDeviation = new Deviation(10);
            NoiseThresholdSettings customSettings = new NoiseThresholdSettings(0.002, 50, NoiseThresholdSettings.BASE_PEAK.NOT_PRECURSOR, 0);
            FastCosine customFastCosine = new FastCosine(customDeviation, false, customSettings);
            
            assertEquals(customDeviation, customFastCosine.getMaxDeviation());
        }

        @Test
        @DisplayName("Test FastCosine Default Constructor")
        void testDefaultConstructor() {
            FastCosine defaultFastCosine = new FastCosine();
            
            assertEquals(15, defaultFastCosine.getMaxDeviation().getPpm());
        }
    }

    @Nested
    @DisplayName("FastCosine Similarity Tests")
    class FastCosineSimilarityTests {

        @Test
        @DisplayName("Test fastCosine with identical spectra")
        void testFastCosineIdenticalSpectra() {
            double[] mzValues = {100.0, 200.0, 300.0, 400.0};
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            double parentMass = 500.0;

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastCosine(spectrum1, spectrum2);
            
            assertEquals(1.0, similarity.similarity, 1e-6, "Identical spectra should have a cosine similarity of 1.0");
            assertEquals(4, similarity.sharedPeaks, "Should have 4 peak matches");
            
            // Check peak assignments (every peak should be matched to itself)
            IntList assignments = similarity.getSharedPeakPairs();
            for (int i = 0; i < mzValues.length; i++) {
                int leftIdx = assignments.getInt(i * 2);
                int rightIdx = assignments.getInt(i * 2 + 1);
                assertEquals(i, leftIdx);
                assertEquals(i, rightIdx);
            }
        }

        @Test
        @DisplayName("Test fastCosine with non-matching spectra")
        void testFastCosineNonMatchingSpectra() {
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};
            double[] mzValues2 = {105.0, 205.0, 305.0, 405.0};  // Outside tolerance
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            double parentMass = 500.0;

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastCosine(spectrum1, spectrum2);
            
            assertEquals(0.0, similarity.similarity, 1e-6, "Non-matching spectra should have a cosine similarity of 0.0");
            assertEquals(0, similarity.sharedPeaks, "Should have no peak matches");
        }

        @Test
        @DisplayName("Test fastCosine with partially matching spectra")
        void testFastCosinePartiallyMatchingSpectra() {
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};
            double[] mzValues2 = {100.001, 200.001, 305.0, 405.0};  // First two within tolerance, last two outside
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            double parentMass = 500.0;

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastCosine(spectrum1, spectrum2);
            
            // Should only match the first two peaks
            assertTrue(similarity.similarity > 0.0 && similarity.similarity < 1.0, 
                       "Partially matching spectra should have cosine similarity between 0 and 1");
            assertEquals(2, similarity.sharedPeaks, "Should have 2 peak matches");
            
            // Check that only the first two peaks are matched
            IntList assignments = similarity.getSharedPeakPairs();
            assertEquals(0, assignments.getInt(0));
            assertEquals(0, assignments.getInt(1));
            assertEquals(1, assignments.getInt(2));
            assertEquals(1, assignments.getInt(3));
        }
        
        @Test
        @DisplayName("Test fastCosine with peaks near parent mass")
        void testFastCosineWithPeaksNearParentMass() {
            double parentMass = 500.0;
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0, 499.9};  // Last peak very close to parent
            double[] mzValues2 = {100.0, 200.0, 300.0, 400.0, 499.9};
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0, 50.0};

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastCosine(spectrum1, spectrum2);
            
            // The peak near the parent mass should be ignored
            assertEquals(4, similarity.sharedPeaks, "Should only match the first 4 peaks");
        }
    }

    @Nested
    @DisplayName("FastReverseCosine Similarity Tests")
    class FastReverseCosineSimilarityTests {

        @Test
        @DisplayName("Test fastReverseCosine with identical spectra")
        void testFastReverseCosineSameSpectra() {
            double[] mzValues = {100.0, 200.0, 300.0, 400.0};
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            double parentMass = 500.0;

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastReverseCosine(spectrum1, spectrum2);
            
            assertEquals(1.0, similarity.similarity, 1e-6, "Identical spectra should have a reverse cosine similarity of 1.0");
            assertEquals(4, similarity.sharedPeaks, "Should have 4 peak matches");
            
            // Check that all peaks are matched (using the mass difference from parent)
            IntList assignments = similarity.getSharedPeakPairs();
            for (int i = 0; i < mzValues.length; i++) {
                int leftIdx = assignments.getInt(i * 2);
                int rightIdx = assignments.getInt(i * 2 + 1);
                assertEquals(i, leftIdx);
                assertEquals(i, rightIdx);
            }
        }

        @Test
        @DisplayName("Test fastReverseCosine with complementary spectra")
        void testFastReverseCosineMirrorSpectra() {
            double parentMass1 = 500.0;
            double parentMass2 = 600.0;
            
            // Peaks are complementary relative to their parent masses
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};  // Losses: 400, 300, 200, 100
            double[] mzValues2 = {200.0, 300.0, 400.0, 500.0};  // Losses: 400, 300, 200, 100
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass1, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass2, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastReverseCosine(spectrum1, spectrum2);
            
            assertEquals(1.0, similarity.similarity, 1e-6, "Complementary spectra should have reverse cosine of 1.0");
            assertEquals(4, similarity.sharedPeaks, "Should have 4 peak matches");
            
            // Check assignments
            IntList assignments = similarity.getSharedPeakPairs();
            for (int i = 0; i < mzValues1.length; i++) {
                int leftIdx = assignments.getInt(i * 2);
                int rightIdx = assignments.getInt(i * 2 + 1);
                assertEquals(i, leftIdx);
                assertEquals(i, rightIdx);
            }
        }
        
        @Test
        @DisplayName("Test fastReverseCosine with non-matching spectra")
        void testFastReverseCosineMismatchedSpectra() {
            double parentMass1 = 500.0;
            double parentMass2 = 600.0;
            
            // These have completely different loss patterns
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};  // Losses: 400, 300, 200, 100
            double[] mzValues2 = {150.0, 250.0, 350.0, 450.0};  // Losses: 450, 350, 250, 150
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass1, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass2, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastReverseCosine(spectrum1, spectrum2);
            
            assertEquals(0.0, similarity.similarity, 1e-6, "Non-matching spectra should have reverse cosine of 0.0");
            assertEquals(0, similarity.sharedPeaks, "Should have no matches");
        }
    }

    @Nested
    @DisplayName("FastModifiedCosine Similarity Tests")
    class FastModifiedCosineSimilarityTests {

        @Test
        @DisplayName("Test fastModifiedCosine with identical spectra")
        void testFastModifiedCosineIdenticalSpectra() {
            double[] mzValues = {100.0, 200.0, 300.0, 400.0};
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            double parentMass = 500.0;

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastModifiedCosine(spectrum1, spectrum2);
            
            assertEquals(1.0, similarity.similarity, 1e-6, "Identical spectra should have modified cosine of 1.0");
            assertEquals(4, similarity.sharedPeaks, "Should have 4 peak matches");
        }

        @Test
        @DisplayName("Test fastModifiedCosine with mass shift")
        void testFastModifiedCosineWithMassShift() {
            double parentMass1 = 500.0;
            double parentMass2 = 514.0;  // 14 Da mass shift
            double massShift = parentMass2 - parentMass1;
            
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};
            // Same spectral pattern but shifted by 14 Da
            double[] mzValues2 = {114.0, 214.0, 314.0, 414.0};
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass1, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass2, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastModifiedCosine(spectrum1, spectrum2);
            
            // Should have high similarity despite mass shift
            assertTrue(similarity.similarity > 0.9, "Similar spectra with mass shift should have high modified cosine");
            assertEquals(4, similarity.sharedPeaks, "Should have 4 peak matches");
            
            // Check that the assignments account for the mass shift
            IntList assignments = similarity.getSharedPeakPairs();
            for (int i = 0; i < mzValues1.length; i++) {
                int leftIdx = assignments.getInt(i * 2);
                int rightIdx = assignments.getInt(i * 2 + 1);
                assertEquals(i, leftIdx);
                assertEquals(i, rightIdx);
                
                // Verify the mass shift is consistent
                double leftMz = mzValues1[leftIdx];
                double rightMz = mzValues2[rightIdx];
                assertEquals(massShift, rightMz - leftMz, 0.001, "Mass difference should match the mass shift");
            }
        }
        
        @Test
        @DisplayName("Test fastModifiedCosine with partial mass shift")
        void testFastModifiedCosinePartialMassShift() {
            double parentMass1 = 500.0;
            double parentMass2 = 514.0;  // 14 Da mass shift
            
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};
            // Only some peaks shifted, others at same position
            double[] mzValues2 = {100.0, 200.0, 314.0, 414.0};
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass1, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass2, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastModifiedCosine(spectrum1, spectrum2);
            
            // All peaks should be matched, some directly and some with mass shift
            assertTrue(similarity.similarity > 0.5, "Spectra with partial mass shift should have decent modified cosine");
            assertEquals(4, similarity.sharedPeaks, "Should have 4 peak matches");
        }
        
        @Test
        @DisplayName("Test fastModifiedCosine falls back to fastCosine for same mass")
        void testFastModifiedCosineFallsBackToFastCosine() {
            double[] mzValues = {100.0, 200.0, 300.0, 400.0};
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            double parentMass = 500.0;

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            
            // We should get the same result from both methods
            SpectralSimilarity similarityCosine = fastCosine.fastCosine(spectrum1, spectrum2);
            SpectralSimilarity similarityModified = fastCosine.fastModifiedCosine(spectrum1, spectrum2);
            
            assertEquals(similarityCosine.similarity, similarityModified.similarity, 1e-6, 
                        "ModifiedCosine should fall back to Cosine for same parent mass");
            assertEquals(similarityCosine.sharedPeaks, similarityModified.sharedPeaks, 
                        "Assignments should be the same");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Test with empty spectra")
        void testWithEmptySpectra() {
            double[] emptyMz = {};
            double[] emptyIntensity = {};
            double parentMass = 500.0;
            
            SearchPreparedSpectrum emptySpectrum = createSearchPreparedSpectrum(parentMass, emptyMz, emptyIntensity);
            SearchPreparedSpectrum normalSpectrum = createSearchPreparedSpectrum(
                parentMass, 
                new double[]{100.0, 200.0}, 
                new double[]{1000.0, 500.0}
            );
            
            SpectralSimilarity similarity1 = fastCosine.fastCosine(emptySpectrum, emptySpectrum);
            assertEquals(0.0, similarity1.similarity, 1e-6, "Empty spectra against empty should be 0");
            assertEquals(0, similarity1.sharedPeaks, "No alignments for empty spectra");
            
            SpectralSimilarity similarity2 = fastCosine.fastCosine(emptySpectrum, normalSpectrum);
            assertEquals(0.0, similarity2.similarity, 1e-6, "Empty spectrum against normal should be 0");
            assertEquals(0, similarity2.sharedPeaks, "No alignments for empty vs normal");
        }
        
        @Test
        @DisplayName("Test with very close m/z values")
        void testWithVeryCloseMzValues() {
            double parentMass = 500.0;
            // Create two peaks that are extremely close but within tolerance
            double[] mzValues1 = {100.0, 200.0, 300.0};
            double[] mzValues2 = {100.0000149, 200.0000299, 300.0000449};  // Within 15 ppm
            double[] intensityValues = {1000.0, 500.0, 250.0};
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastCosine(spectrum1, spectrum2);
            
            assertEquals(1.0, similarity.similarity, 1e-6, "Very close m/z values should match");
            assertEquals(3, similarity.sharedPeaks, "Should have 3 matches for very close values");
        }
        
        @Test
        @DisplayName("Test with just-outside-tolerance m/z values")
        void testWithJustOutsideToleranceMzValues() {
            double parentMass = 700.0;
            // Create peaks that are just outside the tolerance
            double[] mzValues1 = {300.0, 400.0, 500.0};
            double[] mzValues2 = {300.0046, 400.00621, 500.00761};  // Just outside 15 ppm
            double[] intensityValues = {1000.0, 500.0, 250.0};
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastCosine(spectrum1, spectrum2);
            
            assertEquals(0.0, similarity.similarity, 1e-6, "Just-outside-tolerance m/z values should not match");
            assertEquals(0, similarity.sharedPeaks, "Should have no matches for outside tolerance");
        }
        
        @Test
        @DisplayName("Test with different deviation settings")
        void testWithDifferentDeviationSettings() {
            double parentMass = 500.0;
            // Create peaks that are just outside the default tolerance
            double[] mzValues1 = {100.0, 200.0, 300.0};
            double[] mzValues2 = {100.0000151, 200.0000301, 300.0000451};  // Just outside 15 ppm
            double[] intensityValues = {1000.0, 500.0, 250.0};
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues);
            
            // Create a FastCosine with more permissive tolerance
            FastCosine moreTolerantFastCosine = new FastCosine(new Deviation(20), true, defaultNoiseSettings);
            
            SpectralSimilarity similarity = moreTolerantFastCosine.fastCosine(spectrum1, spectrum2);
            
            assertEquals(1.0, similarity.similarity, 1e-6, "Values should match with more permissive tolerance");
            assertEquals(3, similarity.sharedPeaks, "Should have 3 matches with more permissive tolerance");
        }
    }
    
    @Nested
    @DisplayName("Peak Assignment Tests")
    class PeakAssignmentTests {
        
        @Test
        @DisplayName("Test fastCosine peak assignments for identical spectra")
        void testFastCosinePeakAssignmentsIdenticalSpectra() {
            double[] mzValues = {100.0, 200.0, 300.0, 400.0};
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            double parentMass = 500.0;

            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastCosine(spectrum1, spectrum2);
            
            // Verify that the sharedPeakPairs list has expected length
            IntList assignments = similarity.getSharedPeakPairs();
            assertNotNull(assignments, "Peak assignments should not be null");
            assertEquals(8, assignments.size(), "Should have 8 values in assignments (4 peak pairs)");
            
            // Verify that each peak is matched to its corresponding peak in the other spectrum
            for (int i = 0; i < mzValues.length; i++) {
                int leftIndex = assignments.getInt(i * 2);
                int rightIndex = assignments.getInt(i * 2 + 1);
                
                assertEquals(i, leftIndex, "Left spectrum peak at index " + i + " should be assigned to itself");
                assertEquals(i, rightIndex, "Right spectrum peak at index " + i + " should be assigned to itself");
                
                // Verify that the matching is correct based on m/z values
                assertEquals(spectrum1.getMzAt(leftIndex), spectrum2.getMzAt(rightIndex), 1e-6, 
                    "Matched peaks should have the same m/z value");
            }
        }
        
        @Test
        @DisplayName("Test fastCosine peak assignments for offset spectra")
        void testFastCosinePeakAssignmentsWithMzOffset() {
            // Spectrum with slight offset in m/z values but still within tolerance
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};
            double[] mzValues2 = {100.001, 200.002, 300.003, 400.004}; // Small offset but within tolerance
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            double parentMass = 500.0;
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastCosine(spectrum1, spectrum2);
            
            IntList assignments = similarity.getSharedPeakPairs();
            assertEquals(8, assignments.size(), "Should have 8 values in assignments (4 peak pairs)");
            
            // Verify each assignment matches peaks with similar m/z values
            for (int i = 0; i < mzValues1.length; i++) {
                int leftIndex = assignments.getInt(i * 2);
                int rightIndex = assignments.getInt(i * 2 + 1);
                
                double leftMz = spectrum1.getMzAt(leftIndex);
                double rightMz = spectrum2.getMzAt(rightIndex);
                
                // Check that the matching is correct based on m/z values
                assertTrue(defaultDeviation.inErrorWindow(leftMz, rightMz), 
                    "Matched peaks should be within the mass tolerance: " + leftMz + " vs " + rightMz);
            }
        }
        
        @Test
        @DisplayName("Test fastCosine peak assignments for partially matching spectra")
        void testFastCosinePeakAssignmentsPartialMatching() {
            // Some peaks match, others don't
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};
            double[] mzValues2 = {100.001, 200.002, 350.0, 450.0}; // First two match, last two don't
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            double parentMass = 500.0;
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastCosine(spectrum1, spectrum2);
            
            IntList assignments = similarity.getSharedPeakPairs();
            assertEquals(4, assignments.size(), "Should have 4 values in assignments (2 peak pairs)");
            
            // Verify that only the first two peaks are matched
            for (int i = 0; i < 2; i++) {
                int leftIndex = assignments.getInt(i * 2);
                int rightIndex = assignments.getInt(i * 2 + 1);
                
                assertEquals(i, leftIndex, "Left spectrum peak at index " + i + " should be assigned");
                assertEquals(i, rightIndex, "Right spectrum peak at index " + i + " should be assigned");
                
                double leftMz = spectrum1.getMzAt(leftIndex);
                double rightMz = spectrum2.getMzAt(rightIndex);
                
                assertTrue(defaultDeviation.inErrorWindow(leftMz, rightMz), 
                    "Matched peaks should be within the mass tolerance");
            }
        }

        @Test
        @DisplayName("Test fastReverseCosine peak assignments")
        void testFastReverseCosinePeakAssignments() {
            double parentMass1 = 500.0;
            double parentMass2 = 600.0;
            
            // Peaks with same loss pattern relative to parent mass
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};  // Losses: 400, 300, 200, 100
            double[] mzValues2 = {200.0, 300.0, 400.0, 500.0};  // Losses: 400, 300, 200, 100
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass1, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass2, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastReverseCosine(spectrum1, spectrum2);
            
            IntList assignments = similarity.getSharedPeakPairs();
            assertEquals(8, assignments.size(), "Should have 8 values in assignments (4 peak pairs)");
            
            // Verify that peaks are matched based on their distance from parent mass
            for (int i = 0; i < 4; i++) {
                int leftIndex = assignments.getInt(i * 2);
                int rightIndex = assignments.getInt(i * 2 + 1);
                
                double leftLoss = parentMass1 - spectrum1.getMzAt(leftIndex);
                double rightLoss = parentMass2 - spectrum2.getMzAt(rightIndex);
                
                assertTrue(defaultDeviation.inErrorWindow(leftLoss, rightLoss), 
                    "Matched peaks should have similar mass losses: " + leftLoss + " vs " + rightLoss);
            }
        }
        
        @Test
        @DisplayName("Test fastModifiedCosine peak assignments with mass shift")
        void testFastModifiedCosinePeakAssignmentsWithMassShift() {
            double parentMass1 = 500.0;
            double parentMass2 = 514.0;  // 14 Da shift (e.g., methyl group)
            double massShift = parentMass2 - parentMass1;
            
            // Shifted spectrum pattern
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};
            double[] mzValues2 = {114.0, 214.0, 314.0, 414.0};  // Shifted by 14 Da
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass1, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass2, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastModifiedCosine(spectrum1, spectrum2);
            
            IntList assignments = similarity.getSharedPeakPairs();
            assertEquals(8, assignments.size(), "Should have 8 values in assignments (4 peak pairs)");
            
            // Verify that assignments account for the mass shift
            for (int i = 0; i < 4; i++) {
                int leftIndex = assignments.getInt(i * 2);
                int rightIndex = assignments.getInt(i * 2 + 1);
                
                double leftMz = spectrum1.getMzAt(leftIndex);
                double rightMz = spectrum2.getMzAt(rightIndex);
                
                // Check that the mass difference matches the shift
                assertEquals(massShift, rightMz - leftMz, 0.001, 
                    "Mass difference between matched peaks should match the mass shift");
            }
        }
        
        @Test
        @DisplayName("Test fastModifiedCosine peak assignments with complex mass pattern")
        void testFastModifiedCosinePeakAssignmentsComplexPattern() {
            double parentMass1 = 500.0;
            double parentMass2 = 514.0;  // 14 Da shift
            
            // Mix of shifted and unshifted peaks (as could happen with real molecules)
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};
            // Some peaks shifted, some not (common fragment ions might be unshifted)
            double[] mzValues2 = {100.0, 214.0, 300.0, 414.0};
            double[] intensityValues = {1000.0, 500.0, 250.0, 100.0};
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass1, mzValues1, intensityValues);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass2, mzValues2, intensityValues);
            
            SpectralSimilarity similarity = fastCosine.fastModifiedCosine(spectrum1, spectrum2);
            
            // Check we have reasonable assignments
            IntList assignments = similarity.getSharedPeakPairs();
            assertNotNull(assignments, "Peak assignments should not be null");
            assertTrue(assignments.size() > 0, "Should have some peak assignments");
            
            // Validate mapping is initialized correctly
            Int2IntMap peakPairsMap = similarity.getSharedPeakPairsMap();
            assertEquals(assignments.size() / 2, peakPairsMap.size(), 
                "Map size should match number of peak pairs");
            
            // For each assignment, verify correctness based on m/z or mass loss pattern
            for (int i = 0; i < assignments.size(); i += 2) {
                int leftIndex = assignments.getInt(i);
                int rightIndex = assignments.getInt(i + 1);
                
                double leftMz = spectrum1.getMzAt(leftIndex);
                double rightMz = spectrum2.getMzAt(rightIndex);
                
                // Either direct match or shifted match
                boolean isDirectMatch = defaultDeviation.inErrorWindow(leftMz, rightMz);
                boolean isShiftedMatch = defaultDeviation.inErrorWindow(leftMz + 14.0, rightMz);
                
                assertTrue(isDirectMatch || isShiftedMatch, 
                    "Peaks should match either directly or with shift: " + leftMz + " vs " + rightMz);
                
                // Verify map contains the same mapping
                assertEquals(rightIndex, peakPairsMap.get(leftIndex), 
                    "Map should contain the same assignments as the list");
            }
        }
    }
    
    @Nested
    @DisplayName("Merged Spectrum Tests")
    class MergedSpectrumTests {
        
        @Test
        @DisplayName("Test prepareMergedQuery basic functionality")
        void testPrepareMergedQuery() {
            double parentMass = 500.0;
            
            // Create two similar but not identical spectra
            double[] mzValues1 = {100.0, 200.0, 300.0, 400.0};
            double[] mzValues2 = {100.001, 200.001, 300.001, 400.001};
            double[] intensityValues1 = {1000.0, 500.0, 250.0, 100.0};
            double[] intensityValues2 = {900.0, 550.0, 280.0, 90.0};
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues1);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues2);
            
            List<SearchPreparedSpectrum> spectraList = Arrays.asList(spectrum1, spectrum2);
            
            SearchPreparedMergedSpectrum mergedSpectrum = fastCosine.prepareMergedQuery(spectraList);
            
            assertNotNull(mergedSpectrum, "Merged spectrum should not be null");
            assertEquals(parentMass, mergedSpectrum.getParentMass(), 1e-6, "Parent mass should be preserved");
            assertEquals(4, mergedSpectrum.size(), "Merged spectrum should have 4 peaks");
            
            // Convert back to regular spectrum for comparison
            SearchPreparedSpectrum regularSpectrum = mergedSpectrum.asSearchPreparedSpectrum();
            
            // Test that the merged spectrum has high similarity with both source spectra
            SpectralSimilarity similarity1 = fastCosine.fastCosine(regularSpectrum, spectrum1);
            SpectralSimilarity similarity2 = fastCosine.fastCosine(regularSpectrum, spectrum2);
            
            assertTrue(similarity1.similarity > 0.9, "Merged spectrum should have high similarity with source 1");
            assertTrue(similarity2.similarity > 0.9, "Merged spectrum should have high similarity with source 2");
        }
        
        @Test
        @DisplayName("Test merged spectrum as upper bound query")
        void testMergedSpectrumAsUpperBoundQuery() {
            double parentMass = 500.0;
            
            // Create two similar but not identical spectra
            double[] mzValues1 = {100.0, 200.0, 300.0};
            double[] mzValues2 = {100.0, 200.0, 310.0};  // Last peak is different
            double[] intensityValues1 = {1000.0, 500.0, 250.0};
            double[] intensityValues2 = {800.0, 600.0, 300.0};
            
            SearchPreparedSpectrum spectrum1 = createSearchPreparedSpectrum(parentMass, mzValues1, intensityValues1);
            SearchPreparedSpectrum spectrum2 = createSearchPreparedSpectrum(parentMass, mzValues2, intensityValues2);
            
            List<SearchPreparedSpectrum> spectraList = Arrays.asList(spectrum1, spectrum2);
            
            SearchPreparedMergedSpectrum mergedSpectrum = fastCosine.prepareMergedQuery(spectraList);
            SearchPreparedSpectrum upperBoundSpectrum = mergedSpectrum.asUpperboundMergedSpectrum();
            
            // Upper bound spectrum should have similarity >= the individual similarities
            SpectralSimilarity sim1 = fastCosine.fastCosine(upperBoundSpectrum, spectrum1);
            SpectralSimilarity sim2 = fastCosine.fastCosine(upperBoundSpectrum, spectrum2);
            SpectralSimilarity directSim = fastCosine.fastCosine(spectrum1, spectrum2);
            
            assertTrue(sim1.similarity >= directSim.similarity, 
                      "Upper bound similarity with spectrum1 should be >= direct similarity");
            assertTrue(sim2.similarity >= directSim.similarity, 
                      "Upper bound similarity with spectrum2 should be >= direct similarity");
        }
    }
}