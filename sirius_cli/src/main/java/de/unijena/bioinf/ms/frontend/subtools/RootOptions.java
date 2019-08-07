package de.unijena.bioinf.ms.frontend.subtools;

//import de.unijena.bioinf.babelms.projectspace.SiriusProjectSpace;

import de.unijena.bioinf.babelms.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.ms.frontend.PreprocessingTool;

import java.io.File;
import java.util.List;

public interface RootOptions extends PreprocessingTool {

    Integer getMaxInstanceBuffer();

    Integer getInitialInstanceBuffer();

    SiriusProjectSpace getProjectSpace();

    List<File> getInput();
}
