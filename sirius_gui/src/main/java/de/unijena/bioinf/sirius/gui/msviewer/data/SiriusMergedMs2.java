package de.unijena.bioinf.sirius.gui.msviewer.data;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class SiriusMergedMs2 extends SiriusSingleSpectrumModel{

    public SiriusMergedMs2(Ms2Experiment experiment, double minMz, double maxMz) {
        super(Spectrums.mergeSpectra(new Deviation(10, 0.1), true, false, experiment.getMs2Spectra()), minMz, maxMz);
    }

    public SiriusMergedMs2(Ms2Experiment experiment) {
        super(Spectrums.mergeSpectra(new Deviation(10, 0.1), true, false, experiment.getMs2Spectra()));
    }
}
