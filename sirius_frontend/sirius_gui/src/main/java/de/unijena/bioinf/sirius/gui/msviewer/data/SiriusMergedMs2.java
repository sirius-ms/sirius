package de.unijena.bioinf.sirius.gui.msviewer.data;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class SiriusMergedMs2 extends SiriusSingleSpectrumModel{

    public SiriusMergedMs2(Ms2Experiment experiment) {
        super(Spectrums.mergeSpectra(new Deviation(10, 0.1), true, false, experiment.<Ms2Spectrum<Peak>>getMs2Spectra()));
    }
}
