package de.unijena.bioinf.ms.frontend.subtools.custom_db_downloader;

import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
@CommandLine.Command(name = "get", description = "Download and add a database.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class DownloadDatabaseOptions implements StandaloneTool<Workflow> {

    @CommandLine.Option(names = {"--destination", "-d"}, required = true,
            description = "Path to the newly added custom database.")
    String destination = null;

    @CommandLine.Option(names = {"--db"}, required = true,
            description = "Downloadable database id.")
    String dbId = null;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new DownloadDatabaseWorkflow(dbId, destination);
    }
}
