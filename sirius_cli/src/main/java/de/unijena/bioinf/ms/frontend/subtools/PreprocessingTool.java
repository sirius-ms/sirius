package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;

public interface PreprocessingTool<T extends PreprocessingJob> {
    T makePreprocessingJob(InputFilesOptions input, ProjectSpaceManager space);
}
