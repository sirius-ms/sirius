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
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that provides an API for spectral alignment over {@link CosineQuerySpectrum}
 */
public class CosineSpectraMatcher {

    private final CosineQueryUtils queryUtils;

    public CosineSpectraMatcher(SpectralAlignmentType alignmentType, Deviation maxPeakDeviation) {
        this(alignmentType.getScorer(maxPeakDeviation));
    }

    public CosineSpectraMatcher(AbstractSpectralAlignment spectralAlignmentScorer) {
        this(new CosineQueryUtils(spectralAlignmentScorer));
    }

    public CosineSpectraMatcher(CosineQueryUtils queryUtils) {
        this.queryUtils = queryUtils;
    }

    SpectralSimilarity match(CosineQuerySpectrum left, CosineQuerySpectrum right) {
        return queryUtils.cosineProduct(left, right);
    }

    /**
     * Matches the query against all the references and returns a list of corresponding matches
     */
    List<SpectralSimilarity> matchParallel(CosineQuerySpectrum query, List<CosineQuerySpectrum> references) {
        SpectralMatchMasterJJob job = matchParallelJob(query, references);
        SiriusJobs.getGlobalJobManager().submitJob(job);
        return job.takeResult();
    }

    public SpectralMatchMasterJJob matchParallelJob(CosineQuerySpectrum query, List<CosineQuerySpectrum> references) {
        return new SpectralMatchMasterJJob(queryUtils, references.stream().map(r -> Pair.of(query, r)).toList());
    }

    /**
     * @param spectra list of spectra to match with each other
     * @return a nested list, where element[i][j] is a similarity between spectra[i] and spectra[j]
     */
    List<List<SpectralSimilarity>> matchAllParallel(List<CosineQuerySpectrum> spectra) {
        SpectralMatchMasterJJob job = matchAllParallelJob(spectra);
        SiriusJobs.getGlobalJobManager().submitJob(job);
        List<SpectralSimilarity> flatResult = job.takeResult();
        return unflattenMatchAllResult(flatResult);
    }

    /**
     * @param spectra list of spectra to match with each other
     * @return a job that computes similarities between all pairs in spectra.
     * This job returns a flat list of similarities, which can be unflattened with {@link CosineSpectraMatcher#unflattenMatchAllResult(List)}
     */
    SpectralMatchMasterJJob matchAllParallelJob(List<CosineQuerySpectrum> spectra) {
        return new SpectralMatchMasterJJob(queryUtils, Utils.pairsHalfNoDiag(spectra, spectra.size()));
    }

    /**
     * @param flatResult matches between a list of spectra
     * @return a nested list, where element[i][j] is a similarity between spectra[i] and spectra[j]
     */
    public List<List<SpectralSimilarity>> unflattenMatchAllResult(List<SpectralSimilarity> flatResult) {
        int n = (int) Math.ceil((double) flatResult.size() / 2);
        List<List<SpectralSimilarity>> it = new ArrayList<>(n);
        int k = 0;
        for (int i = 0; i < n; i++) {
            it.add(flatResult.subList(k, k+(n-i)));
            k += n-i;
        }
        return it;
    }
}
