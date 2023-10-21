package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.IntStream;

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

    List<SpectralSimilarity> matchParallel(CosineQuerySpectrum query, List<CosineQuerySpectrum> references) {
        SpectralMatchMasterJJob job = matchParallelJob(query, references);
        SiriusJobs.getGlobalJobManager().submitJob(job);
        return job.takeResult();
    }

    public SpectralMatchMasterJJob matchParallelJob(CosineQuerySpectrum query, List<CosineQuerySpectrum> references) {
        return new SpectralMatchMasterJJob(queryUtils, references.stream().map(r -> Pair.of(query, r)).toList());
    }

    List<List<SpectralSimilarity>> matchAllParallel(List<CosineQuerySpectrum> spectra) {
        SpectralMatchMasterJJob job = matchAllParallelJob(spectra);
        SiriusJobs.getGlobalJobManager().submitJob(job);
        List<SpectralSimilarity> flatResult = job.takeResult();
        return unflattenMatchAllResult(flatResult);
    }

    SpectralMatchMasterJJob matchAllParallelJob(List<CosineQuerySpectrum> spectra) {
        List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> flatPairs = spectra.stream()
                .flatMap(s1 -> spectra.stream().map(s2 -> Pair.of(s1, s2))).toList();
        return new SpectralMatchMasterJJob(queryUtils, flatPairs);
    }

    public List<List<SpectralSimilarity>> unflattenMatchAllResult(List<SpectralSimilarity> flatResult) {
        int n = (int) Math.round(Math.sqrt(flatResult.size()));
        if (n * n != flatResult.size()) {
            throw new IllegalArgumentException("Expected a list with a square number of elements, got a list with " + flatResult.size() + " elements.");
        }

        return IntStream.range(0, n).mapToObj(i -> flatResult.subList(i*n, (i+1)*n)).toList();
    }
}
