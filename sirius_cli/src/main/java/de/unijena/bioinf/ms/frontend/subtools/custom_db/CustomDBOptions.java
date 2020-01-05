package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.SingletonTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.List;

/**
 * This is for parameters needed to create a custom DB.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "custom-db", aliases = {"DB"}, description = "<STANDALONE> Generate a custom compound database. Import compounds from all given files.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class CustomDBOptions implements SingletonTool<CustomDBWorkflow> {

    @Option(names = "--name", description = "Name of the custom database to be added to the default or specified workspace (--workspace).")
    public String dbName;

    @CommandLine.Parameters(description = "files containing SMARTS or InChi generate a database from.", type = File.class)
    public List<File> input = null;


    @Override
    public CustomDBWorkflow makeSingletonWorkflow(PreprocessingJob preproJob, ProjectSpaceManager projectSpace, ParameterConfig config) {
        //todo configure it
        return new CustomDBWorkflow();
    }
}
