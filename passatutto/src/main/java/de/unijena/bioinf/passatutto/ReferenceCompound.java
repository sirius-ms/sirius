package de.unijena.bioinf.passatutto;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

class ReferenceCompound {

    protected FTree tree;
    protected Ms2Experiment experiment;
    protected SimpleSpectrum cleanedUpSpectrum;

    public ReferenceCompound( Ms2Experiment experiment, FTree tree, SimpleSpectrum cleanedUpSpectrum) {
        this.tree = tree;
        this.experiment = experiment;
        this.cleanedUpSpectrum = cleanedUpSpectrum;
    }
}
