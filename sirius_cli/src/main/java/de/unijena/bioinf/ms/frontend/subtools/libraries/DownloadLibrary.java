package de.unijena.bioinf.ms.frontend.subtools.libraries;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.AddDbWorkflowUtil;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBPropertyUtils;
import de.unijena.bioinf.ms.frontend.utils.Progressbar.ProgressBarListener;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

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

            if (CustomDBPropertyUtils.getLocationByName(libId).isPresent()) {
                log.error("Custom database with name {} already exists. Please remove it first with command `DB remove --db {}`", libId, libId);
            }

            Path path = Path.of(destination);
            if (Files.exists(path) & Files.isDirectory(path)) {
                String fileName = libId + CUSTOM_DB_SUFFIX;
                path = path.resolve(fileName);
            }

            log.info("Downloading {} into {}", libId, path);

            try {
                ProgressJJob<Void> downloadJob = ApplicationCore.WEB_API.downloadLibrary(path, libId);
                downloadJob.addJobProgressListener(new ProgressBarListener());
                SiriusJobs.getGlobalJobManager().submitJob(downloadJob).takeResult();
            } catch (Exception e) {
                log.error("Error downloading library.", e);
            }

            log.info("Adding {} to SIRIUS.", libId);

            AddDbWorkflowUtil.addDb(path.toAbsolutePath().toString());
        };
    }
}
