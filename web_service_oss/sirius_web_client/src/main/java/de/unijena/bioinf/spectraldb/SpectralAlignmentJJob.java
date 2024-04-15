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
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.CandidateFormulas;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.chemdb.annotations.SpectralAlignmentScorer;
import de.unijena.bioinf.chemdb.annotations.SpectralSearchDB;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bionf.spectral_alignment.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SpectralAlignmentJJob extends BasicMasterJJob<SpectralSearchResult> {

    private final WebAPI<?> api;

    private final Ms2Experiment experiment;

    private CosineQueryUtils queryUtils;
    private Deviation precursorDev;
    private Deviation peakDev;
    private double precursorMz;

    public SpectralAlignmentJJob(WebAPI<?> api, Ms2Experiment experiment) {
        super(JobType.SCHEDULER);
        this.experiment = experiment;
        this.api = api;
    }

    @Override
    protected SpectralSearchResult compute() throws Exception {
        peakDev = experiment.getAnnotationOrDefault(SpectralMatchingMassDeviation.class).allowedPeakDeviation;
        precursorDev = experiment.getAnnotationOrDefault(SpectralMatchingMassDeviation.class).allowedPrecursorDeviation;
        precursorMz = experiment.getIonMass();

        List<Ms2Spectrum<Peak>> queries = experiment.getMs2Spectra();

        SpectralAlignmentType alignmentType = experiment.getAnnotationOrDefault(SpectralAlignmentScorer.class).spectralAlignmentType;

        queryUtils = new CosineQueryUtils(alignmentType.getScorer(peakDev));
        List<CosineQuerySpectrum> cosineQueries = getCosineQueries(queryUtils, queries);

        List<SpectralMatchMasterJJob> jobs = new ArrayList<>();

        JobProgressMerger progressMonitor = new JobProgressMerger(this.pcs);

        final List<Ms2ReferenceSpectrum> references = api.getChemDB().lookupSpectra(precursorMz, precursorDev, true, experiment.getAnnotationOrDefault(SpectralSearchDB.class).searchDBs);

        List<SpectralMatchMasterJJob> dbJobs = getAlignmentJJobs(queryUtils, cosineQueries, references);

        dbJobs.forEach(job -> {
            job.setClearInput(false);
            job.addJobProgressListener(progressMonitor);
            jobs.add(submitSubJob(job));
        });

        if (jobs.isEmpty())
            return null;

        Stream<SpectralSearchResult.SearchResult> results = jobs.stream()
                .flatMap(j -> extractResults(j, references))
                .sorted((a, b) -> Double.compare(b.getSimilarity().similarity, a.getSimilarity().similarity));

        SpectralSearchResult searchResults = SpectralSearchResult.builder()
                .precursorDeviation(precursorDev)
                .peakDeviation(peakDev)
                .alignmentType(alignmentType)
                .results(Streams.mapWithIndex(results, (r, index) -> {
                    r.setRank((int) index + 1);
                    return r;
                }).toList())
                .build();

        //add adduct and formula from high-scoring library hits to detected adducts and formula candiadates list
        InjectSpectralLibraryMatchFormulas injectFormulas = experiment.getAnnotationOrDefault(InjectSpectralLibraryMatchFormulas.class);
        addAdductsAndFormulasFromHighScoringLibraryMatches(experiment, searchResults, injectFormulas.getMinScoreToInject(), injectFormulas.getMinPeakMatchesToInject());

        return searchResults;
    }

    public List<CosineQuerySpectrum> getCosineQueries(CosineQueryUtils utils, List<Ms2Spectrum<Peak>> queries) {
        return Streams.mapWithIndex(queries.stream(), (q, index) -> {
            CosineQuerySpectrum qs = utils.createQueryWithIntensityTransformation(
                    Spectrums.mergePeaksWithinSpectrum(Spectrums.getMassOrderedSpectrum(q), peakDev, true, false),
                    precursorMz, true);
            qs.setIndex((int) index);
            return qs;
        }).toList();
    }

    public List<SpectralMatchMasterJJob> getAlignmentJJobs(CosineQueryUtils utils, List<CosineQuerySpectrum> queries, List<Ms2ReferenceSpectrum> references) {
        List<CosineQuerySpectrum> referenceQueries = Streams.mapWithIndex(references.stream(), (r, index) -> {
            CosineQuerySpectrum q = utils.createQueryWithIntensityTransformation(
                    Spectrums.mergePeaksWithinSpectrum(Spectrums.getMassOrderedSpectrum(r.getSpectrum()), peakDev, true, false), precursorMz, true);
            r.setSpectrum(null);
            q.setIndex((int) index);
            return q;
        }).toList();

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
                        System.out.println("OVER 100%  " + similarities.get(i).similarity);
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

    private void addAdductsAndFormulasFromHighScoringLibraryMatches(Ms2Experiment exp, SpectralSearchResult result, double minSimilarity, int minSharedPeaks) {
        final DetectedAdducts detAdds = exp.computeAnnotationIfAbsent(DetectedAdducts.class, DetectedAdducts::new);
        Set<PrecursorIonType> adducts = SpectralSearchResults.deriveDistinctAdductsSetWithThreshold(result.getResults(), exp.getIonMass(), minSimilarity, minSharedPeaks);
        if (adducts.isEmpty()) return;

        PossibleAdducts possibleAdducts = new PossibleAdducts(adducts);
        //overrides any detected addcuts from previous spectral library searches for consistency reasons. alternatively, we could use union.
        detAdds.put(DetectedAdducts.Source.SPECTRAL_LIBRARY_SEARCH, possibleAdducts);

        //set high-scoring formulas
        Set<MolecularFormula> formulas = SpectralSearchResults.deriveDistinctFormulaSetWithThreshold(result.getResults(), exp.getIonMass(), minSimilarity, minSharedPeaks);
        if (formulas.isEmpty()) return;

        CandidateFormulas candidateFormulas = exp.computeAnnotationIfAbsent(CandidateFormulas.class);
        candidateFormulas.addAndMergeSpectralLibrarySearchFormulas(formulas, SpectralAlignmentJJob.class);
    }
}
