package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

import java.io.File;

public class Instance {

    final Ms2Experiment experiment;
    final File file;

    FTree optTree;

    public Instance(Ms2Experiment experiment, File file) {
        this.experiment = experiment;
        this.file = file;
    }

    public String fileNameWithoutExtension() {
        final String name = file.getName();
        final int i = name.lastIndexOf('.');
        if (i>=0) return name.substring(i);
        else return name;
    }
}
