package de.unijena.bioinf.spectraldb;

import com.google.common.collect.Streams;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for matching multiple query spectra each against multiple reference spectra.
 * Intended usage:
 * <pre>
 * {@code matcher = new SpectralMatcher(...);}
 * {@code matcher.setQuery(0, querySpectrum, queryPrecursorMz);}
 * {@code matcher.addMatches(references0);}
 * {@code matcher.setQuery(1, anotherQuerySpectrum, anotherPrecursorMz);}
 * {@code matcher.addMatches(references1);}
 * {@code result = matcher.getResult();}
 * </pre>
 */
public class SpectralMatcher {


    private final SpectralAlignmentType alignmentType;
    private final Deviation maxPeakDeviation;
    private final Deviation precursorMzDeviation;
    private final String dbName;
    private final CosineQueryUtils cosineQueryUtils;
    private final List<SpectralSearchResult.SearchResult> results;

    private CosineQuerySpectrum currentQuerySpectrum;
    private int currentQueryIndex;


    public SpectralMatcher(SpectralAlignmentType alignmentType, Deviation maxPeakDeviation, Deviation precursorMzDeviation, String dbName) {
        this.alignmentType = alignmentType;
        this.maxPeakDeviation = maxPeakDeviation;
        this.precursorMzDeviation = precursorMzDeviation;
        this.dbName = dbName;

        cosineQueryUtils = new CosineQueryUtils(alignmentType.getScorer(maxPeakDeviation));
        results = new ArrayList<>();
    }

    /**
     * Sets the query for the following calls to {@link #addMatch(Ms2ReferenceSpectrum)} or {@link #addMatches(Iterable)}
     */
    public void setQuery(int index, OrderedSpectrum<Peak> querySpectrum, double queryPrecursorMz) {
        currentQuerySpectrum = cosineQueryUtils.createQueryWithoutLoss(querySpectrum, queryPrecursorMz);
        currentQueryIndex = index;
    }


    /**
     * Match current query against the passed reference, and if there are shared peaks,
     * add the corresponding {@code SpectralSearchResult.SearchResult} to the results
     */
    public void addMatch(Ms2ReferenceSpectrum reference) {
        CosineQuerySpectrum cosineReferenceSpectrum = cosineQueryUtils.createQueryWithoutLoss(reference.getSpectrum(), reference.getPrecursorMz());
        SpectralSimilarity similarity = cosineQueryUtils.cosineProduct(currentQuerySpectrum, cosineReferenceSpectrum);

        if (similarity.shardPeaks > 0) {
            SpectralSearchResult.SearchResult res = SpectralSearchResult.SearchResult.builder()
                    .dbName(dbName)
                    .querySpectrumIndex(currentQueryIndex)
                    .similarity(similarity)
                    .referenceUUID(reference.getUuid())
                    .referenceSplash(reference.getSplash())
                    .build();
            results.add(res);
        }
    }

    public void addMatches(Iterable<Ms2ReferenceSpectrum> references) {
        references.forEach(this::addMatch);
    }

    /**
     * @return {@code SpectralSearchResult} with results sorted by decreasing similarity over all queries
     */
    public SpectralSearchResult getResults() {
        results.sort((a, b) -> Double.compare(b.getSimilarity().similarity, a.getSimilarity().similarity));

        return SpectralSearchResult.builder()
                .precursorDeviation(precursorMzDeviation)
                .peakDeviation(maxPeakDeviation)
                .alignmentType(alignmentType)
                .results(Streams.mapWithIndex(results.stream(), (r, index) -> {
                    r.setRank((int) index + 1);
                    return r;
                }).toList())
                .build();
    }
}
