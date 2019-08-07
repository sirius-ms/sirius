package de.unijena.bioinf.ms.frontend.subtools.lcms_align;

import de.unijena.bioinf.babelms.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.ms.frontend.PreprocessingTool;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "lcms-align", aliases = {}, description = "Align and merge compounds of multiple LCMS Runs", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class LcmsAlignOptions implements PreprocessingTool {

    @Override
    public PreprocessingJob makePreprocessingJob(List<File> input, SiriusProjectSpace space) {
        return new LcmsAlignSubToolJob(input,space);
    }
}

