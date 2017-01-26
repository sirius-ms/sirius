package de.unijena.bioinf.sirius.gui.msviewer.data;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class SiriusMergedMs2Annotated extends SiriusSingleSpectrumAnnotated {

    public SiriusMergedMs2Annotated(FTree tree, Ms2Experiment experiment) {
        super(tree, merge(experiment));
    }

    private static Spectrum<? extends Peak> merge(Ms2Experiment experiment) {
        return Spectrums.mergeSpectra(new Deviation(10, 0.1), true, false, experiment.<Ms2Spectrum<Peak>>getMs2Spectra());
    }
}
