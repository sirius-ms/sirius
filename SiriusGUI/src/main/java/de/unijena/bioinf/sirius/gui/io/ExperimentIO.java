package de.unijena.bioinf.sirius.gui.io;

import java.io.File;

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

public interface ExperimentIO {

	public void save(ExperimentContainer ec, File file);
	
	public ExperimentContainer load(File file);

}
