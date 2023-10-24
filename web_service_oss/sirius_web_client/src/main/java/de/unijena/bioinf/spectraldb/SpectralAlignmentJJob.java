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
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.annotations.SpectralAlignmentScorer;
import de.unijena.bioinf.chemdb.annotations.SpectralSearchDB;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.projectspace.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bionf.spectral_alignment.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

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
        Deviation peakDev = experiment.getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
        precursorDev = experiment.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;

        List<SearchableDatabase> databases = experiment.getAnnotationOrDefault(SpectralSearchDB.class).searchDBs;

        List<Ms2Spectrum<Peak>> queries = experiment.getMs2Spectra();

        CdkFingerprintVersion version = api.getCDKChemDBFingerprintVersion();

        SpectralAlignmentType alignmentType = experiment.getAnnotationOrDefault(SpectralAlignmentScorer.class).spectralAlignmentType;

        queryUtils = new CosineQueryUtils(alignmentType.getScorer(peakDev));
        List<CosineQuerySpectrum> cosineQueries = queries.stream().map(q -> queryUtils.createQueryWithoutLoss(new SimpleSpectrum(q), q.getPrecursorMz())).toList();

        List<SpectralMatchMasterJJob> jobs = new ArrayList<>();

        // TODO other databases besides custom databases
        // TODO spectral libraries now need to be imported via DB CLI command -> remove SDB command in CLI

        JobProgressMerger progressMonitor = new JobProgressMerger(this);
        progressMonitor.addPropertyChangeListener(evt -> {
            JobProgressEvent progressEvt = (JobProgressEvent) evt;
            updateProgress(progressEvt.getMaxValue(), progressEvt.getProgress(), "Aligning spectra from " + experiment.getName());
        });

        List<SpectralSearchResult.SearchResult> results = new ArrayList<>();

        databases.stream().filter(SearchableDatabase::isCustomDb).forEach(sdb -> {
            CustomDatabase custom = (CustomDatabase) sdb;
            if (custom.getStatistics().getSpectra() > 0) {
                custom.toChemDB(version).ifPresent(db -> {
                    SpectralLibrary spectralDb = (SpectralLibrary) db;

                    for (int i = 0; i < cosineQueries.size(); i++) {
                        CosineQuerySpectrum query = cosineQueries.get(i);
                        List<Ms2ReferenceSpectrum> references = getReferenceSpectra(query, spectralDb);
                        SpectralMatchMasterJJob job = new SpectralMatchMasterJJob(queryUtils, references.stream().map(r -> Pair.of(query, queryUtils.createQueryWithoutLoss(r.getSpectrum(), r.getPrecursorMz()))).toList());
                        int queryIndex = i;
                        job.addJobProgressListener(evt -> {
                            if (evt.isDone()) {
                                List<SpectralSimilarity> similarities = job.result();

                                for (int j = 0; j < similarities.size(); j++) {
                                    SpectralSimilarity similarity = similarities.get(j);
                                    if (similarity.shardPeaks > 0) {
                                        Ms2ReferenceSpectrum reference = references.get(j);
                                        SpectralSearchResult.SearchResult res = SpectralSearchResult.SearchResult.builder()
                                                .dbName(reference.getLibraryName())
                                                .dbId(reference.getLibraryId())
                                                .querySpectrumIndex(queryIndex)
                                                .similarity(similarity)
                                                .referenceUUID(reference.getUuid())
                                                .referenceSplash(reference.getSplash())
                                                .build();
                                        results.add(res);
                                    }
                                }
                            }
                        });
                        job.addJobProgressListener(progressMonitor);
                        jobs.add(submitSubJob(job));
                    }
                });
            }
        });

        if (jobs.isEmpty())
            return null;

        jobs.forEach(JJob::takeResult);

        results.sort((a, b) -> Double.compare(b.getSimilarity().similarity, a.getSimilarity().similarity));

        return SpectralSearchResult.builder()
                .precursorDeviation(precursorDev)
                .peakDeviation(peakDev)
                .alignmentType(alignmentType)
                .results(Streams.mapWithIndex(results.stream(), (r, index) -> {
                    r.setRank((int) index + 1);
                    return r;
                }).toList())
                .build();
    }

    private List<Ms2ReferenceSpectrum> getReferenceSpectra(CosineQuerySpectrum query, SpectralLibrary db) {
        List<Ms2ReferenceSpectrum> result = new ArrayList<>();
        try {
            db.lookupSpectra(query.getPrecursorMz(), precursorDev, true).forEach(result::add);
        } catch (ChemicalDatabaseException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
