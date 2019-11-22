package de.unijena.bioinf.ms.frontend.subtools.gui;

import de.unijena.bioinf.ms.frontend.io.InputFiles;
import de.unijena.bioinf.ms.frontend.io.projectspace.GuiProjectSpace;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import picocli.CommandLine;

import java.io.File;
import java.util.List;

@CommandLine.Command(name = "gui-background-computation", aliases = {"gbc"}, defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, sortOptions = false)
public class GuiComputeRoot implements RootOptions {

    protected final GuiProjectSpace guiProjectSpace;
    protected final List<InstanceBean> instances;

    public GuiComputeRoot(GuiProjectSpace guiProjectSpace, List<InstanceBean> instances) {
        this.guiProjectSpace = guiProjectSpace;
        this.instances = instances;
    }

    /**
     * here we need to provide the PP to write on.
     * */
    @Override
    public ProjectSpaceManager getProjectSpace() {
        return guiProjectSpace.getProjectSpace();
    }

    @Override
    public InputFiles getInput() {
        return null;
    }

    /**
     * here we provide an iterator about the Instances we want to compute with this configured workflow
     * */
    @Override
    public PreprocessingJob makePreprocessingJob(InputFiles input, ProjectSpaceManager space) {
        return new PreprocessingJob() {
            @Override
            protected Iterable<InstanceBean> compute() throws Exception {
                return instances;
            }
        };
    }
}
