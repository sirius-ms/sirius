package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingTool;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignSubToolJob;
import picocli.CommandLine;

import java.io.File;
import java.util.List;

@CommandLine.Command(name = "project-space", aliases = {"PS"}, description = "Modify a given project Space", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class ProjecSpaceOptions implements PreprocessingTool {

    @Override
    public PreprocessingJob makePreprocessingJob(List<File> input, ProjectSpaceManager space) {
        return new ProjectSpaceSubToolJob(input,space);
    }
}

