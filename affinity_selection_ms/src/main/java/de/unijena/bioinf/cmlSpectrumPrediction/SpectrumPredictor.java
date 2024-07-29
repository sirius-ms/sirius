package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

/**
 * An interface which defines the general functionality of an object for predicting mass spectra (MS1, MS2,...).<br>
 *
 * @param <P> the type of the peak in the predicted spectrum
 * @param <S> the type of the predicted spectrum
 */
public interface SpectrumPredictor<P extends Peak,S extends Spectrum<P>> {

    /**
     * This method predicts a mass spectrum of type {@code S} for a given molecular structure.
     *
     * @return the predicted mass spectrum
     */
    S predictSpectrum();

}
