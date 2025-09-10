package de.unijena.bioinf.ms.frontend.subtools.libraries;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.AddDbWorkflowUtil;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBPropertyUtils;
import de.unijena.bioinf.ms.frontend.utils.Progressbar.ProgressBarListener;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;

import static de.unijena.bioinf.chemdb.custom.CustomDatabases.CUSTOM_DB_SUFFIX;

@Slf4j
public class DownloadLibraryWorkflow extends BasicJJob<Boolean> implements Workflow {

    private final String libId;
    private final String destination;
    private ProgressJJob<Void> downloadJob = null;

    public DownloadLibraryWorkflow(String libId, String destination) {
        super(JJob.JobType.SCHEDULER);
        this.libId = libId;
        this.destination = destination;
    }

    @Override
    protected Boolean compute() throws Exception {
        if (CustomDBPropertyUtils.getLocationByName(libId).isPresent()) {
            throw new RuntimeException("Custom database with name " + libId + " already exists. Please remove it first.");
        }

        Path path = Path.of(destination);
        if (Files.exists(path) & Files.isDirectory(path)) {
            String fileName = libId + CUSTOM_DB_SUFFIX;
            path = path.resolve(fileName);
        }

        log.info("Downloading {} into {}", libId, path);
        try {
            downloadJob = ApplicationCore.WEB_API().downloadLibrary(path, libId);
            downloadJob.addJobProgressListener(new ProgressBarListener());
            downloadJob.addJobProgressListener(this::updateProgress);
            SiriusJobs.getGlobalJobManager().submitJob(downloadJob).takeResult();
        } catch (Exception e) {
            throw new RuntimeException("Error downloading " + libId + ".", e);
        }

        log.info("Adding {} to SIRIUS.", libId);
        updateProgress(100, 100, "Adding to SIRIUS");
        AddDbWorkflowUtil.addDb(path.toAbsolutePath().toString());
        log.info("Library {} successfully added.", libId);
        return true;
    }

    @Override
    public void run() {
        SiriusJobs.getGlobalJobManager().submitJob(this).takeResult();
    }

    @Override
    public void cancel() {
        cancel(false);
        if (downloadJob != null)
            downloadJob.cancel();
    }
}
