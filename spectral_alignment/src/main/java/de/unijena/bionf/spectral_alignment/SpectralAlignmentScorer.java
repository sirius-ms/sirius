package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

public interface SpectralAlignmentScorer {

    /**
     * @return similarity score of the two passed spectra
     */
    SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right, double precursorLeft, double precursorRight, Deviation deviation);
}
