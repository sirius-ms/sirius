package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;

import java.nio.file.Path;

public interface RootOptions<T extends PreprocessingJob<?>> {

    ProjectSpaceManager getProjectSpace();

    InputFilesOptions getInput();

    Path getOutputLocation();

    T makePreprocessingJob();
}
