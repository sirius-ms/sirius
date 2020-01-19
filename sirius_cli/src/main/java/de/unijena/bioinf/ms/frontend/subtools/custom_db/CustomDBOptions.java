package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseImporter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * This is for parameters needed to create a custom DB.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "custom-db", aliases = {"DB"}, description = "<STANDALONE> Generate a custom compound database. Import compounds from files containing SMARTS or InChi and generate a database from..", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class CustomDBOptions implements StandaloneTool<Workflow> {

    @Option(names = "--name", description = "Name of the custom database to be added to the default or specified workspace (--workspace).", required = true)
    public String dbName;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            final InputFilesOptions input = rootOptions.getInput();
            if (dbName == null || dbName.isEmpty() || input == null || input.msInput == null || input.msInput.unknownFiles.isEmpty()) {
                LoggerFactory.getLogger(CustomDBOptions.class).warn("No input data given. Do nothing");
                return;
            }
            try {
                Path loc = SearchableDatabases.getCustomDatabaseDirectory().toPath();
                Files.createDirectories(loc);
                CustomDatabaseImporter.importDatabase(loc.resolve(dbName).toString(),
                        input.msInput.unknownFiles.stream().map(Path::toFile).collect(Collectors.toList()),
                        ApplicationCore.WEB_API);
            } catch (IOException e) {
                LoggerFactory.getLogger(CustomDBOptions.class).warn("error when storing custom db");

            }
        };
    }
}
