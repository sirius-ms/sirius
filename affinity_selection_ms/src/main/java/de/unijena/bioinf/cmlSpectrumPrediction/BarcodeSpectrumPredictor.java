package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.sirius.MS2Peak;

public class BarcodeSpectrumPredictor extends SpectrumPredictor<MS2Peak>{

    public BarcodeSpectrumPredictor(){
        super(null, null);
    }

    @Override
    public Spectrum<MS2Peak> predictSpectrum() {
        return null;
    }
}
