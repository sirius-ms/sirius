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

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.chemdb.annotations.SpectralMatchingScorer;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bionf.spectral_alignment.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SpectraMatchingJJob extends BasicMasterJJob<SpectralSearchResult> {

    private final Ms2Experiment experiment;

    private CosineQueryUtils queryUtils;
    private Deviation precursorDev;
    private Deviation peakDev;
    private double precursorMz;
    private List<Ms2ReferenceSpectrum> references;
    public SpectraMatchingJJob(List<Ms2ReferenceSpectrum> references, Ms2Experiment experiment) {
        super(JobType.CPU);
        this.experiment = experiment;
        this.references = references;
    }

    @Override
    protected SpectralSearchResult compute() throws Exception {
        peakDev = experiment.getAnnotationOrDefault(SpectralMatchingMassDeviation.class).allowedPeakDeviation;
        precursorDev = experiment.getAnnotationOrDefault(SpectralMatchingMassDeviation.class).allowedPrecursorDeviation;
        precursorMz = experiment.getIonMass();

        List<Ms2Spectrum<Peak>> queries = experiment.getMs2Spectra();

        SpectralMatchingType alignmentType = experiment.getAnnotationOrDefault(SpectralMatchingScorer.class).spectralMatchingType;

        queryUtils = new CosineQueryUtils(alignmentType.getScorer(peakDev));
        List<CosineQuerySpectrum> cosineQueries = getCosineQueries(queryUtils, peakDev, precursorMz, queries);

        List<SpectralMatchMasterJJob> jobs = new ArrayList<>();

        JobProgressMerger progressMonitor = new JobProgressMerger(this.pcs);

        List<SpectralMatchMasterJJob> dbJobs = getAlignmentJJobs(queryUtils, cosineQueries, references);

        dbJobs.forEach(job -> {
            job.setClearInput(false);
            job.addJobProgressListener(progressMonitor);
            jobs.add(submitSubJob(job));
        });

        if (jobs.isEmpty())
            return null;

        List<SpectralSearchResult.SearchResult> results = jobs.stream()
                .flatMap(j -> extractResults(j, references))
                .sorted((a, b) -> {
                    if (Math.abs(a.getSimilarity().similarity - b.getSimilarity().similarity) < 1E-3) {
                        return Integer.compare(b.getSimilarity().sharedPeaks, a.getSimilarity().sharedPeaks);
                    }
                    return Double.compare(b.getSimilarity().similarity, a.getSimilarity().similarity);
                }).toList();

        {
            int rank = 1;
            for (SpectralSearchResult.SearchResult r : results)
                r.setRank(rank++);
        }

        return SpectralSearchResult.builder()
                .precursorDeviation(precursorDev)
                .peakDeviation(peakDev)
                .alignmentType(alignmentType)
                .results(results)
                .build();
    }

    public static List<CosineQuerySpectrum> getCosineQueries(CosineQueryUtils utils, Deviation peakDev, double precursorMz, List<Ms2Spectrum<Peak>> queries) {
        List<CosineQuerySpectrum> r = new ArrayList<>(queries.size());
        int index = 0;
        for (Ms2Spectrum<Peak> q : queries) {
            CosineQuerySpectrum qs = utils.createQueryWithIntensityTransformation(
                    Spectrums.mergePeaksWithinSpectrum(Spectrums.getMassOrderedSpectrum(q), peakDev, true, false),
                    precursorMz, true);
            qs.setIndex(index++);
            r.add(qs);
        }
        return r;
    }

    public List<SpectralMatchMasterJJob> getAlignmentJJobs(CosineQueryUtils utils, List<CosineQuerySpectrum> queries, List<Ms2ReferenceSpectrum> references) {
        List<CosineQuerySpectrum> referenceQueries = new ArrayList<>(references.size());
        int index = 0;
        for (Ms2ReferenceSpectrum r : references) {
            CosineQuerySpectrum q = utils.createQueryWithIntensityTransformation(
                    Spectrums.mergePeaksWithinSpectrum(Spectrums.getMassOrderedSpectrum(r.getSpectrum()), peakDev, true, false), precursorMz, true);
            r.setSpectrum(null);
            q.setIndex(index++);
            referenceQueries.add(q);
        }

        return queries.stream().map(query -> {
            List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> pairs = referenceQueries.stream()
                    .map(rq -> Pair.of(query, rq)).toList();
            return new SpectralMatchMasterJJob(utils, pairs);
        }).toList();
    }

    public Stream<SpectralSearchResult.SearchResult> extractResults(SpectralMatchMasterJJob job, List<Ms2ReferenceSpectrum> references) {

        List<SpectralSimilarity> similarities = job.takeResult();

        if (similarities.isEmpty()) {
            return Stream.empty();
        }

        List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> input = job.getQueries();
        int queryIndex = input.get(0).getLeft().getIndex();

        return IntStream.range(0, similarities.size())
                .filter(i -> similarities.get(i).sharedPeaks > 0)
                .mapToObj(i -> {
                    if (similarities.get(i).similarity > 1)
                        logWarn("Modified Cosine above 1! This is likely a bug. Please submit bug report with example data.");
                    Ms2ReferenceSpectrum reference = references.get(input.get(i).getRight().getIndex());
                    return SpectralSearchResult.SearchResult.builder()
                            .dbName(reference.getLibraryName())
                            .dbId(reference.getLibraryId())
                            .querySpectrumIndex(queryIndex)
                            .similarity(similarities.get(i))
                            .uuid(reference.getUuid())
                            .splash(reference.getSplash())
                            .candidateInChiKey(reference.getCandidateInChiKey())
                            .smiles(reference.getSmiles())
                            .molecularFormula(reference.getFormula())
                            .adduct(reference.getPrecursorIonType())
                            .exactMass(reference.getExactMass())
                            .build();
                });
    }
}
