package de.unijena.bioinf.ms.frontend.subtools.libraries;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.AddDbWorkflowUtil;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.unijena.bioinf.chemdb.custom.CustomDatabases.CUSTOM_DB_SUFFIX;

@Slf4j
@CommandLine.Command(name = "get", description = "Download and add a library.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class DownloadLibrary implements StandaloneTool<Workflow> {

    @CommandLine.Option(names = {"--destination", "-d"}, required = true,
            description = "Path to the newly added library custom database.")
    String destination = null;

    @CommandLine.Option(names = {"--library", "-L"}, required = true,
            description = "Remote library id.")
    String libId = null;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            Path path = Path.of(destination);
            if (Files.exists(path) & Files.isDirectory(path)) {
                String fileName = libId + CUSTOM_DB_SUFFIX;
                path = path.resolve(fileName);
            }

            log.info("Downloading {} into {}", libId, path);

            try {
                ApplicationCore.WEB_API.downloadLibrary(path, libId);
            } catch (IOException e) {
                log.error("Error downloading library.", e);
            }

            log.info("Adding {} to SIRIUS.", libId);

            AddDbWorkflowUtil.addDb(path.toAbsolutePath().toString());
        };
    }
}
