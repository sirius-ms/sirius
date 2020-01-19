package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseImporter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.SingletonTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * This is for parameters needed to create a custom DB.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "custom-db", aliases = {"DB"}, description = "<STANDALONE> Generate a custom compound database. Import compounds from all given files.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class CustomDBOptions implements SingletonTool<Workflow> {

    @Option(names = "--name", description = "Name of the custom database to be added to the default or specified workspace (--workspace).", required = true)
    public String dbName;

    @CommandLine.Parameters(description = "files containing SMARTS or InChi generate a database from.", type = File.class, arity = "1..*")
    public List<File> input = null;


    @Override
    public Workflow makeSingletonWorkflow(PreprocessingJob<?> preproJob, ParameterConfig config) {
        return () -> {
            if (dbName == null || dbName.isEmpty() || input == null || input.isEmpty()) {
                LoggerFactory.getLogger(CustomDBOptions.class).warn("No input data given. Do nothing");
                return;
            }
            try {
                Path loc = SearchableDatabases.getCustomDatabaseDirectory().toPath();
                Files.createDirectories(loc);
                CustomDatabaseImporter.importDatabase(loc.resolve(dbName).toString(), input, ApplicationCore.WEB_API);
            } catch (IOException e) {
                LoggerFactory.getLogger(CustomDBOptions.class).warn("error when storing custom db");

            }
        };
    }
}
