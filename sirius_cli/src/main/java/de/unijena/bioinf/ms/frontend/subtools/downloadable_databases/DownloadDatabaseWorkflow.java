package de.unijena.bioinf.ms.frontend.subtools.downloadable_databases;

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
public class DownloadDatabaseWorkflow extends BasicJJob<Boolean> implements Workflow {

    private final String dbId;
    private final String destination;
    private ProgressJJob<Void> downloadJob = null;

    public DownloadDatabaseWorkflow(String dbId, String destination) {
        super(JJob.JobType.SCHEDULER);
        this.dbId = dbId;
        this.destination = destination;
    }

    @Override
    protected Boolean compute() throws Exception {
        if (CustomDBPropertyUtils.getLocationByName(dbId).isPresent()) {
            throw new RuntimeException("Custom database with name " + dbId + " already exists. Please remove it first.");
        }

        Path path = Path.of(destination);
        if (Files.exists(path) & Files.isDirectory(path)) {
            String fileName = dbId + CUSTOM_DB_SUFFIX;
            path = path.resolve(fileName);
        }

        log.info("Downloading {} into {}", dbId, path);
        try {
            downloadJob = ApplicationCore.WEB_API().downloadDatabase(path, dbId);
            downloadJob.addJobProgressListener(new ProgressBarListener());
            downloadJob.addJobProgressListener(this::updateProgress);
            SiriusJobs.getGlobalJobManager().submitJob(downloadJob).takeResult();
        } catch (Exception e) {
            throw new RuntimeException("Error downloading " + dbId + ".", e);
        }

        log.info("Adding {} to SIRIUS.", dbId);
        updateProgress(100, 100, "Adding to SIRIUS");
        AddDbWorkflowUtil.addDb(path.toAbsolutePath().toString());
        log.info("Database {} successfully added.", dbId);
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
