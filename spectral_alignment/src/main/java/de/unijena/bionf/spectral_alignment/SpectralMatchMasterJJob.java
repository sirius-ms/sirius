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

import de.unijena.bioinf.jjobs.BasicMasterJJob;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * A JJob class for scheduling spectral alignment matches for given pairs of spectra
 */
public class SpectralMatchMasterJJob extends BasicMasterJJob<List<SpectralSimilarity>> {

    private CosineQueryUtils queryUtils;
    private List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> queries;

    public SpectralMatchMasterJJob(CosineQueryUtils queryUtils, List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> queries) {
        super(JobType.CPU);
        this.queryUtils = queryUtils;
        this.queries = queries;
    }

    @Override
    protected List<SpectralSimilarity> compute() throws Exception {
        List<SpectralMatchJJob> jobs = queries.stream().map(q -> new SpectralMatchJJob(queryUtils, q.getLeft(), q.getRight())).toList();

        queryUtils = null;
        queries = null;

        jobs.forEach(this::submitSubJob);
        return jobs.stream().map(SpectralMatchJJob::takeResult).toList();
    }
}
