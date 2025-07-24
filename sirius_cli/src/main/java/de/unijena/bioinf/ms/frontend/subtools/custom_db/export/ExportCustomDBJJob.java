package de.unijena.bioinf.ms.frontend.subtools.custom_db.export;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabases;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBPropertyUtils;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ExportCustomDBJJob extends BasicJJob<Boolean> implements Workflow {

    private final String dbName;
    private Path location;
    private final Format format;

    public ExportCustomDBJJob(String dbName, Path location, Format format) {
        this.dbName = dbName;
        this.location = location;
        this.format = format;
    }

    @Override
    protected Boolean compute() throws Exception {
        final CdkFingerprintVersion version = ApplicationCore.WEB_API.getCDKChemDBFingerprintVersion();
        String dbLocation = CustomDBPropertyUtils.getLocationByName(dbName).orElseThrow(() -> new RuntimeException("Database " + dbName + " not found."));
        CustomDatabase db = CustomDatabases.open(dbLocation, version, true);

        long maxProgress = db.getStatistics().getCompounds();
        updateProgress(0, maxProgress, 0);

        if (location == null) {
            location = Path.of(""); // current directory
        }
        if (Files.isDirectory(location)) {
            location = location.resolve(dbName + "." + format.name().toLowerCase());
        }
        BufferedWriter fileWriter = Files.newBufferedWriter(location);
        log.info("Exporting DB {} to {}", dbName, location.toAbsolutePath());

        DbExporter exporter = switch (format) {
            case TSV -> new TsvExporter(fileWriter);
            case SDF -> new SdfExporter(fileWriter);
        };

        AtomicLong progress = new AtomicLong(0);
        CustomDatabases.getContents(db).forEach(cw -> {
            try {
                exporter.write(cw);
            } catch (IOException e) {
                log.error("Error exporting molecule {}", cw.getFormula(), e);
            }
            updateProgress(0, maxProgress, progress.incrementAndGet());
        });
        exporter.close();
        log.info("Exported {} compounds.", progress.get());

        return true;
    }

    @Override
    public void run() {
        SiriusJobs.getGlobalJobManager().submitJob(this).takeResult();
    }

    @Override
    public void cancel() {
        super.cancel();
    }
}
