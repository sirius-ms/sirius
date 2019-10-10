package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;

import java.io.File;
import java.util.List;

public interface RootOptions extends PreprocessingTool {

    Integer getMaxInstanceBuffer();

    Integer getInitialInstanceBuffer();

    ProjectSpaceManager getProjectSpace();

    List<File> getInput();
}
