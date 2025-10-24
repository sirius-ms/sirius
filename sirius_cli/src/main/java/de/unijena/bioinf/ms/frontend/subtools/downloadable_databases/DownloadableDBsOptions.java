package de.unijena.bioinf.ms.frontend.subtools.downloadable_databases;

import de.unijena.bioinf.ms.frontend.subtools.ParentCommand;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import picocli.CommandLine;

@CommandLine.Command(name = "custom-db-downloader", description = "<STANDALONE> List and download SIRIUS databases.%n%n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false,
        subcommands = {
                ListDownloadableDBsOptions.class,
                DownloadDatabaseOptions.class})
public class DownloadableDBsOptions extends ParentCommand { }
