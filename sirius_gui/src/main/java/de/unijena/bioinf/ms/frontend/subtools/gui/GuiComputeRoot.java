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

@CommandLine.Command(name = "gui-background-computation", aliases = {"gbc"},  versionProvider = Provide.Versions.class, sortOptions = false)
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
    public GuiProjectSpaceManager getProjectSpace() {
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
//                instances.forEach(it -> it.setComputing(true));
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
//                instances.forEach(it -> it.setComputing(false));
                return true;
            }
        };
    }

    @Override
    public ProjectSpaceManagerFactory<GuiProjectSpaceManager> getSpaceManagerFactory() {
        return null;
    }
}
