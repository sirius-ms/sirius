package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RootOptions<M extends ProjectSpaceManager, T extends PreprocessingJob<M>, P extends PostprocessingJob<?>> {
    InputFilesOptions getInput();

    OutputOptions getOutput();


    ProjectSpaceManager getProjectSpace();

    @NotNull
    T makeDefaultPreprocessingJob();

    @Nullable
    P makeDefaultPostprocessingJob();

    ProjectSpaceManagerFactory<M> getSpaceManagerFactory();
}
