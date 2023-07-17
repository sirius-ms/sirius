package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public interface SpectrumPredictor<P extends Peak,S extends Spectrum<P>> {

    S predictSpectrum();

}
