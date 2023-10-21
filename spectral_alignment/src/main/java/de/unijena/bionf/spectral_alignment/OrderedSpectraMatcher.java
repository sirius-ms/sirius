package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

import java.util.List;

/**
 * A class that provides an API for spectral alignment over {@link OrderedSpectrum}
 */
public class OrderedSpectraMatcher {

    private final CosineQueryUtils queryUtils;
    private final CosineSpectraMatcher cosineMatcher;

    public OrderedSpectraMatcher(SpectralAlignmentType alignmentType, Deviation maxPeakDeviation) {
        if (alignmentType == SpectralAlignmentType.MODIFIED_COSINE) {
            throw new IllegalArgumentException("Modified cosine scoring needs precursor mass, use CosineSpectraMatcher.");
        }
        queryUtils = new CosineQueryUtils(alignmentType.getScorer(maxPeakDeviation));
        cosineMatcher = new CosineSpectraMatcher(queryUtils);
    }

    SpectralSimilarity match(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        return queryUtils.cosineProduct(cosineSpectrum(left), cosineSpectrum(right));
    }

    List<SpectralSimilarity> matchParallel(OrderedSpectrum<Peak> query, List<OrderedSpectrum<Peak>> references) {
        return cosineMatcher.matchParallel(cosineSpectrum(query), references.stream().map(this::cosineSpectrum).toList());
    }

    List<List<SpectralSimilarity>> matchAllParallel(List<OrderedSpectrum<Peak>> spectra) {
        return cosineMatcher.matchAllParallel(spectra.stream().map(this::cosineSpectrum).toList());
    }

    private CosineQuerySpectrum cosineSpectrum(OrderedSpectrum<Peak> spectrum) {
        return queryUtils.createQueryWithoutLoss(spectrum, Double.NaN);
    }
}