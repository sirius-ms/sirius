package de.unijena.bioinf.ms.frontend.parameters.custom_db;

import de.unijena.bioinf.ms.frontend.parameters.Provide;
import de.unijena.bioinf.ms.frontend.parameters.SingeltonTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.List;

/**
 * This is for parameters needed to create a custom DB.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "custom-db", aliases = {"DB"}, description = "EXPERIMENTAL FEATURE: generate a custom compound database. Import compounds from all given files.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class CustomDBOptions implements SingeltonTool {

    @Option(names = "--name", description = "Name of the custom database to be added to the default or specified workspace (--workspace).")
    public String dbName;

    @CommandLine.Parameters(description = "files containing SMARTS or InChi generate a database from.", type = File.class)
    public List<File> input = null;


    @Override
    public Workflow getSingeltonWorkflow() {
        //todo configure it
        return new CustomDBWorkflow();
    }
}
