package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;

public interface RootOptions extends PreprocessingTool {

    ProjectSpaceManager getProjectSpace();

    InputFilesOptions getInput();
}
