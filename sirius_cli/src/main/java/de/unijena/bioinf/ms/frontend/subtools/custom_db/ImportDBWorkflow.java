package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseImporter;
import de.unijena.bioinf.chemdb.custom.CustomDatabases;
import de.unijena.bioinf.chemdb.custom.NoSQLCustomDatabase;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ImportDBWorkflow  extends BasicMasterJJob<Boolean> implements Workflow {
    ImportDBOptions importOptions;
    private JJob<Boolean> dbjob = null;

    public ImportDBWorkflow(ImportDBOptions importOptions) {
        super(JJob.JobType.SCHEDULER);
        this.importOptions = importOptions;
    }

    @Override
    public void run() {
        SiriusJobs.getGlobalJobManager().submitJob(this).takeResult();
    }

    @Override
    protected Boolean compute() throws Exception {
        final CdkFingerprintVersion version = ApplicationCore.WEB_API().getCDKChemDBFingerprintVersion();
        return importIntoDB(version);
    }

    private @NotNull Boolean importIntoDB(CdkFingerprintVersion version) throws InterruptedException, IOException, ExecutionException {
        final String name = importOptions.name;
        checkForInterruption();

        String location = CustomDBPropertyUtils.getLocationByName(name).orElseThrow(() -> new RuntimeException("Database " + name + " not found."));

        CustomDatabase db = openExistingDB(location, version);

        Map<Boolean, List<Path>> groups = importOptions.input.stream()
                .flatMap(FileUtils::sneakyWalk)
                .filter(Files::isRegularFile)
                .distinct()
                .collect(Collectors.partitioningBy(p -> MsExperimentParser.isSupportedFileName(p.getFileName().toString())));

        List<Path> spectrumFiles = groups.get(true);
        List<Path> structureFiles = groups.get(false);

        logInfo("Importing new structures to custom database '" + db.name() + "'...");

        long totalBytesToRead = 0;
        for (Path file : structureFiles)
            totalBytesToRead += Files.size(file);
        for (Path file : spectrumFiles)
            totalBytesToRead += Files.size(file);

        checkForInterruption();

        dbjob = CustomDatabaseImporter.makeImportToDatabaseJob(
                spectrumFiles.stream().map(PathInputResource::new).collect(Collectors.toList()),
                structureFiles.stream().map(PathInputResource::new).collect(Collectors.toList()),
                createImportProgressTracker(totalBytesToRead, importOptions.bioTransformerOptions != null),
                (NoSQLCustomDatabase<?, ?>) db,
                ApplicationCore.WEB_API(),
                ApplicationCore.IFP_CACHE(),
                importOptions.writeBuffer,
                importOptions.bioTransformerOptions != null ? importOptions.bioTransformerOptions.toBioTransformerSetting() : null
        );
        checkForInterruption();
        submitJob(dbjob).awaitResult();
        logInfo("...New structures imported to custom database '" + location + "'. Database ID is: " + db.getSettings().getName());

        logInfo("Reopening database in read-only mode");
        CustomDatabases.remove(db, false);
        CustomDatabases.open(location, true, version, true);

        return true;
    }

    private CustomDatabase openExistingDB(String location, CdkFingerprintVersion version) throws IOException {
        CustomDatabases.getCustomDatabaseByPath(location).ifPresent(existingDb -> CustomDatabases.remove(existingDb, false));  // Close read-only DB
        CustomDatabase db = CustomDatabases.open(location, true, version, false);
        if (!CustomDBPropertyUtils.getCustomDBs().containsKey(location)) {
            CustomDBPropertyUtils.addDB(location, db.name());
        }
        logInfo("Opened existing database" + db.name() + ".");
        return db;
    }

    private CustomDatabaseImporter.Listener createImportProgressTracker(final long totalBytes, boolean biotransformation) {
        return new CustomDatabaseImporter.Listener() {
            private final AtomicLong readBytes = new AtomicLong(0);
            private final AtomicLong progressDone = new AtomicLong(0);
            private volatile long progressBeforeCurrentBatch = 0;  // before the current batch of bts or fps

            private final AtomicLong fileCompoundsRemaining = new AtomicLong(0);  // after the current batch

            private final AtomicLong btSourcesProcessed = new AtomicLong(0);
            private final AtomicLong btExpanded = new AtomicLong(0);
            final static double DEFAULT_BT_EXPANSION_RATIO = 7d;  // average bt products per source molecule, used only for the initial estimation before import statistics is available
            private volatile long btBatchSize = 0;
            private volatile long fpBatchSize = 0;

            private final AtomicLong btDoneInBatch = new AtomicLong(0);
            private final AtomicLong fpDoneInBatch = new AtomicLong(0);

            final static int PROGRESS_PER_FP = 1;
            final static int PROGRESS_PER_BT = 5;

            private volatile long remainingProgressInBatch = 0;
            private volatile long remainingProgressInFile = 0;
            private volatile long progressInRemainingFiles = 0;

            @Override
            public void bytesRead(String filename, long bytesRead) {
                readBytes.addAndGet(bytesRead);
                reportUndefined("Reading " + filename);
            }

            @Override
            public void compoundsImported(String filename, int count) {
                fileCompoundsRemaining.addAndGet(count);
                estimateProgressInRemainingFiles();
            }

            @Override
            public void startFingerprints(int total) {
                if (biotransformation) {
                    btSourcesProcessed.addAndGet(btBatchSize);
                    btBatchSize = 0;
                    btExpanded.addAndGet(total);
                } else {
                    fileCompoundsRemaining.addAndGet(-total);
                    estimateRemainingProgressInFile();
                }
                fpBatchSize = total;
                estimateRemainingProgressInBatch();
                progressBeforeCurrentBatch = progressDone.get();
            }

            @Override
            public void newFingerprint(InChI inChI) {
                progressDone.addAndGet(PROGRESS_PER_FP);
                reportProgress("Fingerprints " + fpDoneInBatch.incrementAndGet() + "/" + (fpBatchSize == 0 ? "?" : fpBatchSize));
            }

            @Override
            public void startBioTransformations(int total) {
                btBatchSize = total;
                btDoneInBatch.set(0);
                fileCompoundsRemaining.addAndGet(-total);
                estimateRemainingProgressInFile();
                estimateRemainingProgressInBatch();
                progressBeforeCurrentBatch = progressDone.get();
                reportProgress("Starting biotransformations...");
            }

            @Override
            public void bioTransformation() {
                progressDone.addAndGet(PROGRESS_PER_BT);
                reportProgress("Biotransformations " + btDoneInBatch.incrementAndGet() + "/" + btBatchSize);
            }

            @Override
            public void newInChI(List<InChI> inchis) {
                fpBatchSize = 0;
                fpDoneInBatch.set(0);
                remainingProgressInBatch = 0;
                reportUndefined("Finalizing and saving...");
            }

            /**
             * Should be called before BT expansion and before FP calculation
             */
            private void estimateRemainingProgressInBatch() {
                if (btBatchSize > 0) {
                    long estimatedExpanded = Math.round(btBatchSize * getBtExpansionRatio());
                    remainingProgressInBatch = getProgress(btBatchSize, estimatedExpanded);
                } else {
                    remainingProgressInBatch = getProgress(0, fpBatchSize);
                }
//                    System.out.println("Estimated remaining progress in batch: " + remainingProgressInBatch + ", BT: " + btBatchSize + ", FP: " + fpBatchSize);
            }

            private long estimateRemainingProgressInFile() {
                if (biotransformation) {
                    long estimatedExpanded = Math.round(fileCompoundsRemaining.get() * getBtExpansionRatio());
                    remainingProgressInFile = getProgress(fileCompoundsRemaining.get(), estimatedExpanded);
                } else {
                    remainingProgressInFile = getProgress(0, fileCompoundsRemaining.get());
                }
//                    System.out.println("Estimated remaining progress in file: " + remainingProgressInFile + ", remaining compounds: " + fileCompoundsRemaining.get());
                return remainingProgressInFile;
            }

            private void estimateProgressInRemainingFiles() {
                long remainingBytes = totalBytes - readBytes.get();
                long progressUpToCurrentFile = progressDone.get() + estimateRemainingProgressInFile();
                progressInRemainingFiles = remainingBytes * progressUpToCurrentFile / readBytes.get();
//                    System.out.println("Estimated remaining files progress: " + progressInRemainingFiles + ", remaining bytes: " + remainingBytes);
            }

            private double getBtExpansionRatio() {
                if (btExpanded.get() == 0) {
                    return DEFAULT_BT_EXPANSION_RATIO;
                } else {
                    return btSourcesProcessed.doubleValue() / btExpanded.doubleValue();
                }
            }

            private long getProgress(long bts, long fps) {
                return bts * PROGRESS_PER_BT + fps * PROGRESS_PER_FP;
            }

            private void reportProgress(String message) {
                long maxProgress = progressBeforeCurrentBatch + remainingProgressInBatch + remainingProgressInFile + progressInRemainingFiles;
                updateProgress(0, maxProgress, progressDone.get(), message);
            }

            private void reportUndefined(String message) {
                updateProgress(0, 1, 0, message);
            }
        };
    }
    
    @Override
    public void cancel() {
        cancel(false);
        if (dbjob != null)
            dbjob.cancel();
    }
}
