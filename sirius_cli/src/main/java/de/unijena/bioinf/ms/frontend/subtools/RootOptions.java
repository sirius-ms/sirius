package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;

public interface RootOptions<T extends PreprocessingJob<?>> {
    InputFilesOptions getInput();

    ProjectSpaceManager getProjectSpace();

    T makeDefaultPreprocessingJob();
}
