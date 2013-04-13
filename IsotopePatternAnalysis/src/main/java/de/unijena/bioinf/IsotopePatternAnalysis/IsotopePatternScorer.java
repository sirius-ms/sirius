package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public interface IsotopePatternScorer<P extends Peak, S extends Spectrum<P>> {

    /**
     * computes the log likelihood that the isotopic pattern spectrum explains the measured data. Both input spectra
     * should have the same normalization mode!
     * @param measuredSpectrum normalized measured spectrum
     * @param theoreticalSpectrum normalized theoretical spectrum
     * @param usedNormalization normalization mode which was applied to the spectra, or null, if both spectra is not normalized
     * @return
     */
    public double score(S measuredSpectrum, S theoreticalSpectrum, Normalization usedNormalization);
	
	
}
