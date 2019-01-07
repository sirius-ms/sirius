package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

public class CosineQueryUtils {
    private final AbstractSpectralAlignment spectralAlignmentMethod;

    public CosineQueryUtils(AbstractSpectralAlignment spectralAlignmentMethod) {
        this.spectralAlignmentMethod = spectralAlignmentMethod;
    }

    /**
     * create a query for cosine computation
     * @param spectrum
     * @param precursorMz
     * @return
     */
    public CosineQuerySpectrum createQuery(OrderedSpectrum<Peak> spectrum, double precursorMz){
        return CosineQuerySpectrum.newInstance(spectrum, precursorMz, spectralAlignmentMethod);
    }


    /**
     * compute cosine of 2 instances
     * @param query1
     * @param query2
     * @return
     */
    public SpectralSimilarity cosineProductWithLosses(CosineQuerySpectrum query1, CosineQuerySpectrum query2) {
        SpectralSimilarity similarity = cosineProduct(query1, query2);
        SpectralSimilarity similarityLosses = cosineProductOfInverse(query1, query2);

        return new SpectralSimilarity((similarity.similarity +similarityLosses.similarity)/2d, Math.max(similarity.shardPeaks, similarityLosses.shardPeaks));
    }

    private SpectralSimilarity cosineProduct(CosineQuerySpectrum query1, CosineQuerySpectrum query2) {
        SpectralSimilarity similarity = spectralAlignmentMethod.score(query1.spectrum, query2.spectrum);
        return new SpectralSimilarity(similarity.similarity /Math.sqrt(query1.selfSimilarity*query2.selfSimilarity), similarity.shardPeaks);
    }

    private SpectralSimilarity cosineProductOfInverse(CosineQuerySpectrum query, CosineQuerySpectrum query2) {
        SpectralSimilarity similarity = spectralAlignmentMethod.score(query.inverseSpectrum, query2.inverseSpectrum);
        return new SpectralSimilarity(similarity.similarity /(Math.sqrt(query.selfSimilarityLosses*query2.selfSimilarityLosses)), similarity.shardPeaks);
    }

}
