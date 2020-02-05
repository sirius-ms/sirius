package de.unijena.bioinf.ms.frontend.subtools.gui;

import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.io.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "gui-background-computation", aliases = {"gbc"}, defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, sortOptions = false)
public class GuiComputeRoot implements RootOptions<PreprocessingJob<List<InstanceBean>>> {

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

    /**
     * here we provide an iterator about the Instances we want to compute with this configured workflow
     * */
    @Override
    public PreprocessingJob<List<InstanceBean>> makeDefaultPreprocessingJob() {
        return new PreprocessingJob<>() {
            @Override
            protected List<InstanceBean> compute() throws Exception {
                return instances;
            }
        };
    }


}
