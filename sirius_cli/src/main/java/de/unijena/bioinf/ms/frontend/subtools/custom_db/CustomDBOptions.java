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
@CommandLine.Command(name = "custom-db", aliases = {"DB"}, description = "<STANDALONE> Generate a custom searchable structure database. Import multiple files with compounds as SMILES or InChi into this DB.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class CustomDBOptions implements StandaloneTool<Workflow> {

    @Option(names = "--name", description = "Name of the custom database that will be stored at the default ('$USER_HOME/.sirius') or the specified sirius workspace (--workspace).", required = true)
    public String dbName;

    @Option(names = "--output", description = "Alternative output directory of the custom database. The db will be a sub directory with the given name (--name).")
    public Path outputDir = null;

    @Option(names = {"--buffer-size", "--buffer"}, description = "Maximum number of downloaded/computed compounds to keep in memory before writing them to disk (into the db directory).", defaultValue = "1000", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    public int writeBuffer;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {

            final InputFilesOptions input = rootOptions.getInput();
            if (dbName == null || dbName.isEmpty() || input == null || input.msInput == null || input.msInput.unknownFiles.isEmpty()) {
                LoggerFactory.getLogger(CustomDatabaseImporter.class).error("No input data given. Do nothing");
                return;
            }
            try {
                Path loc = outputDir != null ? outputDir : SearchableDatabases.getCustomDatabaseDirectory().toPath();
                Files.createDirectories(loc);
                CustomDatabaseImporter.importDatabase(loc.resolve(dbName).toFile(),
                        input.msInput.unknownFiles.stream().map(Path::toFile).collect(Collectors.toList()),
                        ApplicationCore.WEB_API, writeBuffer);
                LoggerFactory.getLogger(CustomDatabaseImporter.class).info("Database imported. Use 'structure --db=\"" + loc.toString() + "\"' to search in this database.");
            } catch (IOException e) {
                LoggerFactory.getLogger(CustomDatabaseImporter.class).error("error when storing custom db");
            }
        };
    }
}
