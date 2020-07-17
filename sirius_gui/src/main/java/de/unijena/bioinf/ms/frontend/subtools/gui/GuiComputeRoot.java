package de.unijena.bioinf.ms.frontend.subtools.gui;

import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "gui-background-computation", aliases = {"gbc"},  versionProvider = Provide.Versions.class, sortOptions = false)
public class GuiComputeRoot implements RootOptions<GuiProjectSpaceManager, PreprocessingJob<List<InstanceBean>>, PostprocessingJob<?>> {

    protected final GuiProjectSpaceManager guiProjectSpace;
    protected final List<InstanceBean> instances;
    protected InputFilesOptions inputFiles = null;

    public GuiComputeRoot(GuiProjectSpaceManager guiProjectSpace, List<InstanceBean> instances) {
        this.guiProjectSpace = guiProjectSpace;
        this.instances = instances;
    }

    /**
     * here we need to provide the PP to write on.
     * */
    @Override
    public GuiProjectSpaceManager getProjectSpace() {
        return guiProjectSpace;
    }

    @Override
    public InputFilesOptions getInput() {
        return inputFiles;
    }

    public void setNonCompoundInput(Path... genericFiles) {
        setNonCompoundInput(List.of(genericFiles));
    }

    public void setNonCompoundInput(List<Path> genericFiles) {
        inputFiles = new InputFilesOptions();
        inputFiles.msInput = new InputFilesOptions.MsInput(null, null, new ArrayList<>(genericFiles));
    }

    @Override
    public OutputOptions getOutput() {
        throw new UnsupportedOperationException("Output parameters are not supported by this cli entry point.");
    }

    /**
     * here we provide an iterator about the Instances we want to compute with this configured workflow
     *
     * @return PreprocessingJob that provides the iterable of the Instances to be computed
     */
    @Override
    public @NotNull PreprocessingJob<List<InstanceBean>> makeDefaultPreprocessingJob() {
        return new PreprocessingJob<>() {
            @Override
            protected List<InstanceBean> compute() {
                return instances;
            }
        };
    }

    @Nullable
    @Override
    public PostprocessingJob<?> makeDefaultPostprocessingJob() {

        return new PostprocessingJob<Boolean>() {
            @Override
            protected Boolean compute() throws Exception {
                return true;
            }
        };
    }

    @Override
    public ProjectSpaceManagerFactory<GuiProjectSpaceManager> getSpaceManagerFactory() {
        return null;
    }
}
