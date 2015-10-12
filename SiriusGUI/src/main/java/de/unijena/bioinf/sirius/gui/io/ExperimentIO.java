package de.unijena.bioinf.sirius.gui.io;

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import java.io.File;

public interface ExperimentIO {

    public void save(ExperimentContainer ec, File file);

    public ExperimentContainer load(File file);

}
