package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "project-space", aliases = {"PS"}, description = "<STANDALONE> Modify a given project Space", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class ProjecSpaceOptions implements StandaloneTool<ProjectSpaceWorkflow> {

    @Override
    public ProjectSpaceWorkflow makeWorkflow(RootOptions<?,?> rootOptions, ParameterConfig config) {
        return new ProjectSpaceWorkflow();
    }
}

