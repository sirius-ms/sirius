package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpectralMatcherTest {

    @Test
    public void testMatching() {
        SpectralAlignmentType alignment = SpectralAlignmentType.MODIFIED_COSINE;
        Deviation peakDeviation = new Deviation(10);
        Deviation precursorDeviation = new Deviation(0);
        String dbName = "test_db";

        SpectralMatcher matcher = new SpectralMatcher(alignment, peakDeviation, precursorDeviation, dbName);

        SimpleSpectrum dummySpectrumA = new SimpleSpectrum(new double[] {1d}, new double[] {1d});
        SimpleSpectrum dummySpectrumB = new SimpleSpectrum(new double[] {1d, 2d}, new double[] {0.5d, 1d});

        Ms2ReferenceSpectrum emptyReference = Ms2ReferenceSpectrum.builder()
                .spectrum(SimpleSpectrum.empty())
                .precursorMz(0d)
                .build();

        Ms2ReferenceSpectrum dummyReferenceB = Ms2ReferenceSpectrum.builder()
                .spectrum(dummySpectrumB)
                .precursorMz(3d)
                .build();

        matcher.setQuery(0, dummySpectrumA, 1d);
        matcher.addMatches(Arrays.asList(emptyReference, dummyReferenceB));

        matcher.setQuery(1, dummySpectrumB, 3d);
        matcher.addMatch(dummyReferenceB);

        SpectralSearchResult result = matcher.getResult();

        assertEquals(alignment, result.getAlignmentType());
        assertEquals(peakDeviation, result.getPeakDeviation());
        assertEquals(precursorDeviation, result.getPrecursorDeviation());

        List<SpectralSearchResult.SearchResult> results = result.getResults();
        assertEquals(2, results.size());

        // Top match should be query B with reference B
        assertEquals(1, results.get(0).getQuerySpectrumIndex());
        assertEquals(1d, results.get(0).getSimilarity().similarity, 1e-9);
        assertEquals(dbName, results.get(0).getDbName());

        // Second match should be query A with reference B
        assertEquals(0, results.get(1).getQuerySpectrumIndex());
        assertTrue(results.get(1).getSimilarity().similarity < 1);
    }
}