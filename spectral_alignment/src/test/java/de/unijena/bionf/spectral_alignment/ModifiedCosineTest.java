package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ModifiedCosineTest {

    @Test
    public void testScore() {

        final SimpleMutableSpectrum A = new SimpleMutableSpectrum();
        A.addPeak(1, 1);
        A.addPeak(5, 1);
        A.addPeak(8, 1);
        A.addPeak(15, 0.5);
        final SimpleMutableSpectrum B = new SimpleMutableSpectrum();
        B.addPeak(1, 1);
        B.addPeak(8, 1);
        B.addPeak(15, 1);
        B.addPeak(18, 1);
        B.addPeak(20, 1);
        B.addPeak(25, 1);

        final ModifiedCosine modifiedCosine = new ModifiedCosine(new Deviation(10));
        SpectralSimilarity spectralSimilarity = modifiedCosine.score(new SimpleSpectrum(A), new SimpleSpectrum(B), 20, 30);

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

}