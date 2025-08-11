package de.unijena.bioinf.ms.frontend.subtools.libraries;

import de.unijena.bioinf.ms.frontend.subtools.ParentCommand;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import picocli.CommandLine;

@CommandLine.Command(name = "libraries", aliases = {"libs"}, description = "<STANDALONE> List and download default libraries.%n%n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false,
        subcommands = {
                ListLibraries.class,
                DownloadLibrary.class})
public class LibrariesOptions extends ParentCommand { }
