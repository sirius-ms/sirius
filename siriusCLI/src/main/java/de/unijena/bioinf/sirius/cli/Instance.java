package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

import java.io.File;
import java.util.List;

public class Instance {

    final Ms2Experiment experiment;
    final File file;

    FTree optTree;

    public Instance(Ms2Experiment experiment, File file) {
        this.experiment = experiment;
        this.file = file;
    }
}
