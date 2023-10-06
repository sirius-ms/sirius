package de.unijena.bioinf.spectraldb;

import de.unijena.bionf.spectral_alignment.GaussianSpectralAlignment;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.ModifiedCosine;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SpectralAlignmentTypeTest {

    @Test
    public void testGetScorer() {
        assertTrue(SpectralAlignmentType.GAUSSIAN.getScorer(null) instanceof GaussianSpectralAlignment);
        assertTrue(SpectralAlignmentType.INTENSITY.getScorer(null) instanceof IntensityWeightedSpectralAlignment);
        assertTrue(SpectralAlignmentType.MODIFIED_COSINE.getScorer(null) instanceof ModifiedCosine);
    }
}
