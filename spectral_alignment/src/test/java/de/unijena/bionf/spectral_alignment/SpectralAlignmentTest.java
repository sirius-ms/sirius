/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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
    private final GaussianSpectralMatching gaussianScorer = new GaussianSpectralMatching(new Deviation(10));
    private final ModifiedCosine modifiedCosineScorer = new ModifiedCosine(new Deviation(10));

    private final AbstractSpectralMatching[] scorers = {intensityScorer, gaussianScorer, modifiedCosineScorer};

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
        ModifiedCosine.Result res = modifiedCosine.scoreWithResult(left, right, precursorMzLeft, precursorMzRight, 1d);

        assertEquals(3.5, res.getSimilarity().similarity, 1e-9);
        assertEquals(4, res.getSimilarity().sharedPeaks);

        Map<Integer, Integer> expectedAssignment = new HashMap<>();
        expectedAssignment.put(0, 0);  // ASSIGN 1.0 WITH 1.0
        expectedAssignment.put(1, 2);  // ASSIGN 5.0 WITH 15.0
        expectedAssignment.put(2, 1);  // ASSIGN 8.0 WITH 8.0
        expectedAssignment.put(3, 5);  // ASSIGN 15.0 WITH 25.0

        Map<Integer, Integer> actualAssignment = new HashMap<>();

        for (int i=0; i < res.getAssignment().length; i+=2) {
            actualAssignment.put(res.getAssignment()[i], res.getAssignment()[i+1]);
        }

        assertEquals(expectedAssignment, actualAssignment);
    }

    @Test
    public void testGaussian() {
        GaussianSpectralMatching gaussianAlignment = new GaussianSpectralMatching(new Deviation(10));
        SpectralSimilarity spectralSimilarity = gaussianAlignment.score(left, right);

        assertEquals(49735.919716217, spectralSimilarity.similarity, 1e-9);
        assertEquals(3, spectralSimilarity.sharedPeaks);
    }

    @Test
    public void testIntensity() {
        IntensityWeightedSpectralAlignment intensityAlignment = new IntensityWeightedSpectralAlignment(new Deviation(10));
        SpectralSimilarity spectralSimilarity = intensityAlignment.score(left, right);

        assertEquals(2.5, spectralSimilarity.similarity, 1e-9);
        assertEquals(3, spectralSimilarity.sharedPeaks);
    }

    @Test
    public void testEmptySpectra() {
        for (AbstractSpectralMatching scorer : scorers) {
            SpectralSimilarity similarity = scorer.score(SimpleSpectrum.empty(), SimpleSpectrum.empty(), 0, 0);
            assertEquals(0, similarity.similarity, 1e-9);
            assertEquals(0, similarity.sharedPeaks);
        }
    }

    @Test
    public void testNormalized() {
        for (AbstractSpectralMatching scorer : scorers) {
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
        for (AbstractSpectralMatching scorer : scorers) {
            SpectralSimilarity similarity = normalized(scorer, spectrum, spectrum, precursorMz, precursorMz);
            assertEquals(1, similarity.similarity, 1e-9);
            assertEquals(spectrum.size(), similarity.sharedPeaks);
        }
    }

    private SpectralSimilarity normalized(AbstractSpectralMatching scorer, SimpleSpectrum left, SimpleSpectrum right, double leftMz, double rightMz) {
        CosineQueryUtils utils = new CosineQueryUtils(scorer);
        CosineQuerySpectrum leftQuery = utils.createQuery(left, leftMz);
        CosineQuerySpectrum rightQuery = utils.createQuery(right, rightMz);
        return utils.cosineProduct(leftQuery, rightQuery);
    }
}