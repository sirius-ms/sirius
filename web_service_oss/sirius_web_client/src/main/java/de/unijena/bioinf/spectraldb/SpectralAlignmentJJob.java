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

package de.unijena.bioinf.spectraldb;

import com.google.common.collect.Streams;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.chemdb.annotations.SpectralAlignmentScorer;
import de.unijena.bioinf.chemdb.annotations.SpectralSearchDB;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.projectspace.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bionf.spectral_alignment.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SpectralAlignmentJJob extends BasicMasterJJob<SpectralSearchResult> {

    private final WebAPI<?> api;

    private final Ms2Experiment experiment;

    private CosineQueryUtils queryUtils;
    private Deviation precursorDev;

    public SpectralAlignmentJJob(WebAPI<?> api, Ms2Experiment experiment) {
        super(JobType.SCHEDULER);
        this.experiment = experiment;
        this.api = api;
    }

    @Override
    protected SpectralSearchResult compute() throws Exception {
        Deviation peakDev = experiment.getAnnotationOrDefault(SpectraMatchingMassDeviation.class).allowedPeakDeviation;
        precursorDev = experiment.getAnnotationOrDefault(SpectraMatchingMassDeviation.class).allowedPrecursorDeviation;

        List<Ms2Spectrum<Peak>> queries = experiment.getMs2Spectra();

        SpectralAlignmentType alignmentType = experiment.getAnnotationOrDefault(SpectralAlignmentScorer.class).spectralAlignmentType;

        queryUtils = new CosineQueryUtils(alignmentType.getScorer(peakDev));
        List<CosineQuerySpectrum> cosineQueries = getCosineQueries(queryUtils, queries);

        List<SpectralMatchMasterJJob> jobs = new ArrayList<>();

        JobProgressMerger progressMonitor = new JobProgressMerger(this.pcs);


        List<SpectralMatchMasterJJob> dbJobs = getAlignmentJJobs(queryUtils, cosineQueries, experiment.getAnnotationOrDefault(SpectralSearchDB.class).searchDBs, api.getChemDB());
        dbJobs.forEach(job -> {
            job.setClearInput(false);
            job.addJobProgressListener(progressMonitor);
            jobs.add(submitSubJob(job));
        });


        if (jobs.isEmpty())
            return null;

        Stream<SpectralSearchResult.SearchResult> results = jobs.stream()
                .flatMap(this::extractResults)
                .sorted((a, b) -> Double.compare(b.getSimilarity().similarity, a.getSimilarity().similarity));

        return SpectralSearchResult.builder()
                .precursorDeviation(precursorDev)
                .peakDeviation(peakDev)
                .alignmentType(alignmentType)
                .results(Streams.mapWithIndex(results, (r, index) -> {
                    r.setRank((int) index + 1);
                    return r;
                }).toList())
                .build();
    }

    public List<CosineQuerySpectrum> getCosineQueries(CosineQueryUtils utils, List<Ms2Spectrum<Peak>> queries) {
        return Streams.mapWithIndex(queries.stream(), (q, index) ->
                utils.createQueryWithoutLoss(
                        new IndexedQuerySpectrumWrapper(new SimpleSpectrum(q), (int) index),
                        q.getPrecursorMz())).toList();
    }

    public List<SpectralMatchMasterJJob> getAlignmentJJobs(CosineQueryUtils utils, List<CosineQuerySpectrum> queries, Collection<CustomDataSources.Source> searchDbs, WebWithCustomDatabase db) {
        return queries.stream().map(query -> {
            try {
                List<Ms2ReferenceSpectrum> references = db.lookupSpectra(query.getPrecursorMz(), precursorDev, true, searchDbs);

                List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> pairs = references.stream().map(r ->
                        Pair.of(query, utils.createQueryWithoutLoss(new Ms2ReferenceSpectrumWrapper(r), r.getPrecursorMz()))).toList();

                return new SpectralMatchMasterJJob(utils, pairs);
            } catch (ChemicalDatabaseException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    public Stream<SpectralSearchResult.SearchResult> extractResults(SpectralMatchMasterJJob job) {

        List<SpectralSimilarity> similarities = job.takeResult();

        if (similarities.isEmpty()) {
            return Stream.empty();
        }

        List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> input = job.getQueries();
        int queryIndex = ((IndexedQuerySpectrumWrapper) input.get(0).getLeft().getSpectrum()).getQueryIndex();

        return IntStream.range(0, similarities.size())
                .filter(i -> similarities.get(i).sharedPeaks > 0)
                .mapToObj(i -> {
                    Ms2ReferenceSpectrum reference = ((Ms2ReferenceSpectrumWrapper) input.get(i).getRight().getSpectrum()).getMs2ReferenceSpectrum();
                    return SpectralSearchResult.SearchResult.builder()
                            .dbName(reference.getLibraryName())
                            .dbId(reference.getLibraryId())
                            .querySpectrumIndex(queryIndex)
                            .similarity(similarities.get(i))
                            .referenceUUID(reference.getUuid())
                            .build();
                });
    }
}
