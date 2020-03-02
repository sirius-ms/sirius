package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.projectspace.FilenameFormatter;
import de.unijena.bioinf.projectspace.StandardMSFilenameFormatter;
import picocli.CommandLine;

import java.nio.file.Path;
import java.text.ParseException;

public class OutputOptions {
    @CommandLine.Option(names = {"--output", "--project", "-o"}, description = "Specify the project-space to write into. If no [--input] is specified it is also used as input. For compression use the File ending .zip or .sirius.", order = 210)
    protected Path outputProjectLocation;


    @CommandLine.Option(names = "--naming-convention", description = "Specify a naming scheme for the  compound directories ins the project-space. Default %%index_%%filename_%%compoundname", order = 220)
    private void setProjectSpaceFilenameFormatter(String projectSpaceFilenameFormatter) throws ParseException {
        this.projectSpaceFilenameFormatter = new StandardMSFilenameFormatter(projectSpaceFilenameFormatter);
    }

    protected FilenameFormatter projectSpaceFilenameFormatter = null;


}
