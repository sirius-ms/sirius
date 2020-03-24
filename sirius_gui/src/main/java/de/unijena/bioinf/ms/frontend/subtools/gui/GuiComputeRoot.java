package de.unijena.bioinf.ms.frontend.subtools.gui;

import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "gui-background-computation", aliases = {"gbc"}, defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, sortOptions = false)
public class GuiComputeRoot implements RootOptions<GuiProjectSpaceManager, PreprocessingJob<List<InstanceBean>>, PostprocessingJob<?>> {

    protected final GuiProjectSpaceManager guiProjectSpace;
    protected final List<InstanceBean> instances;

    public GuiComputeRoot(GuiProjectSpaceManager guiProjectSpace, List<InstanceBean> instances) {
        this.guiProjectSpace = guiProjectSpace;
        this.instances = instances;
    }

    /**
     * here we need to provide the PP to write on.
     * */
    @Override
    public ProjectSpaceManager getProjectSpace() {
        return guiProjectSpace;
    }

    @Override
    public InputFilesOptions getInput() {
        return null;
    }

    @Override
    public OutputOptions getOutput() {
        return null;
    }

    /**
     * here we provide an iterator about the Instances we want to compute with this configured workflow
     *
     * @return PreprocessingJob that provides the iterable of the Instances to be computed*/
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
        // currently no preprocessing needed for gui computations
        // maybe we can do some notifications here.
        //todo handle summary writing here?
        return null;
    }

    @Override
    public ProjectSpaceManagerFactory<GuiProjectSpaceManager> getSpaceManagerFactory() {
        return null;
    }
}
