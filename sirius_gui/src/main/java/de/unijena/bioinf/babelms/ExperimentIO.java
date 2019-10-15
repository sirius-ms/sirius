package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;

import java.io.File;
import java.util.List;

public interface ExperimentIO {

	void save(List<InstanceBean> ec, File file);
	
	List<InstanceBean> load(File file);

}
