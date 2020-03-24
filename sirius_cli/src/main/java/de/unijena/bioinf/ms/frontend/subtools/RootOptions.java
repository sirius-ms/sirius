package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RootOptions<M extends ProjectSpaceManager, T extends PreprocessingJob<? extends Iterable<? extends Instance>>, P extends PostprocessingJob<?>> {
    InputFilesOptions getInput();

    OutputOptions getOutput();


    ProjectSpaceManager getProjectSpace();

    @NotNull
    T makeDefaultPreprocessingJob();

    @Nullable
    P makeDefaultPostprocessingJob();

    ProjectSpaceManagerFactory<M> getSpaceManagerFactory();
}
