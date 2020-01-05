package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.SingletonTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "project-space", aliases = {"PS"}, description = "STANDALONE - Modify a given project Space", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class ProjecSpaceOptions implements SingletonTool<ProjectSpaceWorkflow> {

    @Override
    public ProjectSpaceWorkflow makeSingletonWorkflow(PreprocessingJob preproJob, ProjectSpaceManager projectSpace, ParameterConfig config) {
        return null;
    }
}

