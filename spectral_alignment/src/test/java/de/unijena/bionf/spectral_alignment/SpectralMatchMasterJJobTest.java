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
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SpectralMatchMasterJJobTest {

    @Test
    public void compute() {
        CosineQueryUtils utils = new CosineQueryUtils(new ModifiedCosine(new Deviation(10)));
        CosineQuerySpectrum s1 = utils.createQueryWithoutLoss(new SimpleSpectrum(new double[]{1d}, new double[]{1d}), 1d);
        CosineQuerySpectrum s2 = utils.createQueryWithoutLoss(new SimpleSpectrum(new double[] {1d, 2d}, new double[] {0.5d, 1d}), 3d);
        CosineQuerySpectrum s3 = utils.createQueryWithoutLoss(new SimpleSpectrum(new double[]{2d}, new double[]{1d}), 2d);

        List<SpectralSimilarity> expectedMatches = Arrays.asList(utils.cosineProduct(s1, s1), utils.cosineProduct(s1, s2), utils.cosineProduct(s1, s3));

        List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> queries = Arrays.asList(Pair.of(s1, s1), Pair.of(s1, s2), Pair.of(s1, s3));

        SpectralMatchMasterJJob job = new SpectralMatchMasterJJob(utils, queries);


        SiriusJobs.getGlobalJobManager().submitJob(job);
        assertEquals(expectedMatches, job.takeResult());
    }
}