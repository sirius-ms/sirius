package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RootOptions<T extends PreprocessingJob<?>, P extends PostprocessingJob<?>> {
    InputFilesOptions getInput();

    OutputOptions getOutput();


    ProjectSpaceManager getProjectSpace();

    @NotNull
    T makeDefaultPreprocessingJob();

    @Nullable
    P makeDefaultPostprocessingJob();
}
