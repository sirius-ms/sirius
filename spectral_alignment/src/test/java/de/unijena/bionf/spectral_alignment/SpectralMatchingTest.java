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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SpectralMatchingTest {

    private final CosineQueryUtils utils = new CosineQueryUtils(new ModifiedCosine(new Deviation(10)));
    private final CosineQuerySpectrum s1 = utils.createQueryWithIntensityTransformation(new SimpleSpectrum(new double[]{1d}, new double[]{1d}), 1d, true);
    private final CosineQuerySpectrum s2 = utils.createQueryWithIntensityTransformation(new SimpleSpectrum(new double[]{1d, 2d}, new double[]{0.5d, 1d}), 3d, true);
    private final CosineQuerySpectrum s3 = utils.createQueryWithIntensityTransformation(new SimpleSpectrum(new double[]{1.001d, 2d}, new double[]{3d, 2d}), 2d, true);
    private final CosineQuerySpectrum s4 = utils.createQueryWithIntensityTransformation(new SimpleSpectrum(new double[]{1.002d, 2.003d}, new double[]{1.7d, 2.1d}), 2d, true);
    private final CosineQuerySpectrum s5 = utils.createQueryWithIntensityTransformation(new SimpleSpectrum(new double[]{1.003d, 2.002d}, new double[]{1.5d, 1.6d}), 2d, true);
    private final CosineQuerySpectrum s6 = utils.createQueryWithIntensityTransformation(new SimpleSpectrum(new double[]{.999d, 2.001d}, new double[]{1d, 1.2d}), 2d, true);


    @Test
    public void testMatchParallel() {
        CosineSpectraMatcher matcher = new CosineSpectraMatcher(utils);
        List<SpectralSimilarity> matches = matcher.matchParallel(s1, Arrays.asList(s1, s2, s3));

        List<SpectralSimilarity> expectedMatches = Arrays.asList(utils.cosineProduct(s1, s1), utils.cosineProduct(s1, s2), utils.cosineProduct(s1, s3));

        assertEquals(expectedMatches, matches);
    }

    @Test
    public void testMatchAllParallel() {
        List<CosineQuerySpectrum> queries = Arrays.asList(s1, s2, s3, s4, s5, s6);

        CosineSpectraMatcher matcher = new CosineSpectraMatcher(utils);
        List<List<SpectralSimilarity>> matches = matcher.matchAllParallel(queries);

        List<List<SpectralSimilarity>> expectedMatches = List.of(
                List.of(utils.cosineProduct(s1, s2), utils.cosineProduct(s1, s3), utils.cosineProduct(s1, s4), utils.cosineProduct(s1, s5), utils.cosineProduct(s1, s6)),
                List.of(utils.cosineProduct(s2, s3), utils.cosineProduct(s2, s4), utils.cosineProduct(s2, s5), utils.cosineProduct(s2, s6)),
                List.of(utils.cosineProduct(s3, s4), utils.cosineProduct(s3, s5), utils.cosineProduct(s3, s6)),
                List.of(utils.cosineProduct(s4, s5), utils.cosineProduct(s4, s6)),
                List.of(utils.cosineProduct(s5, s6))
        );

        assertEquals(expectedMatches, matches);
    }

    //todo progress is currently disabled, re-enable if we have a good solution
    /*@Test
    public void testProgressReporting() {
        List<CosineQuerySpectrum> queries = Arrays.asList(s1, s2);

        CosineSpectraMatcher matcher = new CosineSpectraMatcher(utils);
        SpectralMatchMasterJJob job = matcher.matchAllParallelJob(queries);

        List<Long> progressSequence = new ArrayList<>();
        job.addJobProgressListener(evt -> progressSequence.add(evt.getProgress()));

        SiriusJobs.getGlobalJobManager().submitJob(job);
        job.takeResult();

        assertEquals(Arrays.asList(0L,1L,2L), progressSequence);  // first and last are from the Master JJob itself
    }*/

    @Test(expected = IllegalArgumentException.class)
    public void testOrderedSpectraMatcherWithModifiedCosine() {
        new OrderedSpectraMatcher(SpectralMatchingType.MODIFIED_COSINE, new Deviation(10));
    }

    //todo fix well defined output for faulty inputs
    /*@Test
    public void testOrderedSpectraMatcher() {
        OrderedSpectraMatcher matcher = new OrderedSpectraMatcher(SpectralMatchingType.INTENSITY, new Deviation(10));

        CosineQueryUtils utilsIntensity = new CosineQueryUtils(SpectralMatchingType.INTENSITY.getScorer(new Deviation(10)));

        List<OrderedSpectrum<Peak>> queries = Arrays.asList(s1.spectrum, s2.spectrum);

        List<List<SpectralSimilarity>> matches = matcher.matchAllParallel(queries);

        List<List<SpectralSimilarity>> expectedMatches = Arrays.asList(
                Arrays.asList(utilsIntensity.cosineProduct(s1, s2)));

        assertEquals(expectedMatches, matches);
    }*/

    @Test
    public void testClearInputs() {
        List<CosineQuerySpectrum> queries = List.of(s1,s2);

        CosineSpectraMatcher matcher = new CosineSpectraMatcher(utils);
        SpectralMatchMasterJJob job1 = matcher.matchAllParallelJob(queries);
        SpectralMatchMasterJJob job2 = matcher.matchAllParallelJob(queries);

        job1.setClearInput(false);

        SiriusJobs.getGlobalJobManager().submitJob(job1);
        SiriusJobs.getGlobalJobManager().submitJob(job2);
        job1.takeResult();
        job2.takeResult();

        assertNotNull(job1.getQueries());
        assertNull(job2.getQueries());
    }
}