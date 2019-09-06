package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.babelms.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;

import java.io.File;
import java.util.List;

public interface PreprocessingTool {
    PreprocessingJob makePreprocessingJob(List<File> input, ProjectSpaceManager space);
}
