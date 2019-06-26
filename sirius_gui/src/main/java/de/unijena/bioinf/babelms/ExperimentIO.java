package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;

import java.io.File;
import java.util.List;

public interface ExperimentIO {

	void save(List<ExperimentResultBean> ec, File file);
	
	List<ExperimentResultBean> load(File file);

}
