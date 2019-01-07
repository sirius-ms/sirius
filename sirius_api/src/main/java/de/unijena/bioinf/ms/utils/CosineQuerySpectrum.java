package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class CosineQuerySpectrum {
    final OrderedSpectrum<Peak> spectrum;
    final SimpleSpectrum inverseSpectrum;
    final double selfSimilarity;
    final double selfSimilarityLosses;
    final double precursorMz;

    private CosineQuerySpectrum(OrderedSpectrum<Peak> spectrum, double precursorMz, SimpleSpectrum inverseSpectrum, double selfSimilarity, double selfSimilarityLosses) {
        this.spectrum = spectrum;
        this.precursorMz = precursorMz;
        this.inverseSpectrum = inverseSpectrum;
        this.selfSimilarity = selfSimilarity;
        this.selfSimilarityLosses = selfSimilarityLosses;

    }


    protected static CosineQuerySpectrum newInstance(OrderedSpectrum<Peak> spectrum, double precursorMz, AbstractSpectralAlignment spectralAlignment) {
        SimpleSpectrum inverseSpectrum = Spectrums.getInversedSpectrum(spectrum, precursorMz);
        double selfSimilarity = spectralAlignment.score(spectrum, spectrum).similarity;
        double selfSimilarityLosses = spectralAlignment.score(inverseSpectrum, inverseSpectrum).similarity;
        return new CosineQuerySpectrum(spectrum, precursorMz, inverseSpectrum, selfSimilarity, selfSimilarityLosses);
    }
}
