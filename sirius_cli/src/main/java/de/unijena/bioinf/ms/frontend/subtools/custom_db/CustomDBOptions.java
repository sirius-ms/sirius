/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.chemdb.custom.*;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * This is for parameters needed to create a custom DB.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "custom-db", aliases = {"DB"}, description = "<STANDALONE> Generate a custom searchable structure/spectral database. Import multiple files with compounds into this DB. %n %n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class CustomDBOptions implements StandaloneTool<Workflow> {


    @CommandLine.ArgGroup(exclusive = false)
    ModeParas mode;

    public static class ModeParas {
        @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n List custom database(s): %n|@", order = 100)
        Show showParas;
        @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n Import custom database: %n|@", order = 200)
        Import importParas;
        @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n Remove custom database: %n|@", order = 400)
        Remove removeParas;

    }

    public static class Import {
        @Option(names = "--location", required = true,
                description = {"Location of the custom database to import into.",
                        "An absolute local path to a new database file file to be created (file name must end with .siriusdb)",
                        "If no input data is given (--input), the database will just be added to SIRIUS",
                        "The added db will also be available in the GUI."}, order = 201)
        private String location = null;


        @Option(names = "--name", order = 202,
                description = {"Name/Identifier of the custom database.",
                        "If not given filename from location will be used."})
        public void setName(String name) {
            this.name = CustomDatabases.sanitizeDbName(name);
        }
        private String name = null;

        @Option(names = "--displayName", order = 203,
                description = {"Displayable name of the custom database.",
                        "This is the preferred name to be shown in the GUI. Maximum Length: 15 characters.",
                        "If not given name will be used."})
        public void setDisplayName(String displayName) {
            if (displayName.length() > 15)
                throw new CommandLine.PicocliException("Maximum allowed length for display names is 15 characters.");
            this.displayName = displayName;
        }

        private String displayName = null;

        @Option(names = {"--buffer-size", "--buffer"}, defaultValue = "1000",
                description = {"Maximum number of downloaded/computed compounds to keep in memory before writing them to disk (into the db directory). Can be set higher when importing large files on a fast computer."},
                order = 210)
        private int writeBuffer;

        @Option(names = {"--input", "-i"}, split = ",", description = {
                "Files or directories to import into the database.",
                "Supported formats: " + MsExperimentParser.DESCRIPTION,
                "Structures without spectra can be passed as a tab-separated (.tsv) file with fields [SMILES, id (optional), name (optional)].",
                "Directories will be recursively expanded."
        }, order = 220)
        private List<Path> input;

        @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n Bio Transformations: %n|@", order = 150)
        private BioTransformerOptions bioTransformerOptions;
    }

    public static class Remove {
        @Option(names = "--remove", required = true,
                description = "Name (--show) or path of the custom database to remove from SIRIUS.",
                order = 401)
        String nameOrLocation = null;

        @Option(names = {"--delete", "-d"}, defaultValue = "false",
                description = "Delete removed custom database from filesystem/server.", order = 410)
        boolean delete;
    }

    public static class Show {
        @Option(names = "--show", required = true, order = 101)
        boolean showDBs;

        @Option(names = {"--db"}, description = "Show information only about the given custom database.", order = 110)
        String db = null;

        @Option(names = {"--details"}, description = "Show detailed (technical) information.",
                order = 120)
        public boolean details;
    }

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new CustomDBWorkflow(rootOptions.getInput());
    }

    public class CustomDBWorkflow extends BasicMasterJJob<Boolean> implements Workflow {
        final InputFilesOptions input;
        private JJob<Boolean> dbjob = null;

        public CustomDBWorkflow(InputFilesOptions input) {
            super(JJob.JobType.SCHEDULER);
            this.input = input;
        }

        @Override
        public void run() {
            SiriusJobs.getGlobalJobManager().submitJob(this).takeResult();
        }

        @Override
        protected Boolean compute() throws Exception {
            final CdkFingerprintVersion version = ApplicationCore.WEB_API.getCDKChemDBFingerprintVersion();

            if (mode.importParas != null) {
                return importIntoDB(version);
            } else if (mode.removeParas != null) {
                return removeDatabase(version);
            } else if (mode.showParas != null) {
                if (mode.showParas.db == null || mode.showParas.db.isBlank()) {
                    return showAllDBs(version);
                } else {
                    return showDB(version);
                }
            }
            throw new IllegalArgumentException("Either '--input', '--remove' or '--show' must be specified.");
        }

        private @NotNull Boolean importIntoDB(CdkFingerprintVersion version) throws InterruptedException, IOException, ExecutionException {
            String location = mode.importParas.location;
            String name = mode.importParas.name;
            if ((location == null || location.isBlank()) && (name == null || name.isBlank())) {
                logWarn("\n==> No location/name given! Exiting.\n");
                return false;
            }
            if (location != null && !Path.of(location).isAbsolute()) {
                location = Path.of(location).toAbsolutePath().toString();
            }

            if (!location.toLowerCase().endsWith(".siriusdb")) {
                logWarn("\n==> Adding file extension '.siriusdb' to location.\n");
                location = location + ".siriusdb";
            }

            checkForInterruption();

            boolean isNewDb = false;

            LinkedHashMap<String, String> existingDBs = CustomDBPropertyUtils.getCustomDBs();
            if (existingDBs.containsKey(location)) {
                name = existingDBs.get(location);
                if (mode.importParas.name != null && !mode.importParas.name.isBlank() && !name.equals(mode.importParas.name)) {
                    logWarn("\n==> Passed name " + mode.importParas.name + " does not match the current DB name " + name + " at this location and will be ignored.\n");
                }
            } else if (existingDBs.containsValue(name)) {
                location = CustomDBPropertyUtils.getLocationByName(existingDBs, name).orElseThrow();
                if (mode.importParas.location != null && !mode.importParas.location.isBlank()) {
                    logWarn("\n==> Passed location " + mode.importParas.location + " does not match the current location of database " + name + " and will be ignored.\n");
                }
            } else if (location == null || location.isBlank()) {
                logWarn("\n==> No location for the new database " + name + " given! Exiting.\n");
                return false;
            } else if (!Files.exists(Path.of(location))) {
                isNewDb = true;
            } else if (mode.importParas.name != null && !mode.importParas.name.isBlank()) {
                logWarn("\n==> Passed name " + mode.importParas.name + " will be ignored, the database name will be taken from the file " + location);
            }

            CustomDatabase db = isNewDb
                    ? createNewDB(location, name, version)
                    : openExistingDB(location, version);

            if (mode.importParas.input == null || mode.importParas.input.isEmpty()) {
                return true;
            }

            Map<Boolean, List<Path>> groups = mode.importParas.input.stream()
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
                    createImportProgressTracker(totalBytesToRead, mode.importParas.bioTransformerOptions != null),
                    (NoSQLCustomDatabase<?, ?>) db,
                    ApplicationCore.WEB_API,
                    ApplicationCore.IFP_CACHE(),
                    mode.importParas.writeBuffer,
                    mode.importParas.bioTransformerOptions != null ? mode.importParas.bioTransformerOptions.toBioTransformerSetting() : null
            );
            checkForInterruption();
            submitJob(dbjob).awaitResult();
            logInfo("...New structures imported to custom database '" + mode.importParas.location + "'. Database ID is: " + db.getSettings().getName());

            return true;
        }


        private CustomDatabase createNewDB(String location, String name, CdkFingerprintVersion version) throws IOException {
            if (name == null || name.isBlank())
                name = CustomDatabases.sanitizeDbName(CustomDBPropertyUtils.getDBName(location));

            CustomDatabaseSettings settings = CustomDatabaseSettings.builder()
                    .usedFingerprints(List.of(version.getUsedFingerprints()))
                    .schemaVersion(CustomDatabase.CUSTOM_DATABASE_SCHEMA)
                    .name(name)
                    .displayName(mode.importParas.displayName)
                    .matchRtOfReferenceSpectra(false)
                    .statistics(new CustomDatabaseSettings.Statistics())
                    .build();

            CustomDatabase db = CustomDatabases.create(location, settings, version);
            CustomDBPropertyUtils.addDB(location, name);

            logInfo("New database added to SIRIUS. Use 'structure --db=\"" + db.name() + "\"' to search in this database.");
            return db;
        }

        private CustomDatabase openExistingDB(String location, CdkFingerprintVersion version) throws IOException {
            CustomDatabase db = CustomDatabases.open(location, true, version);
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

        private boolean removeDatabase(CdkFingerprintVersion version) {
            String nameOrLocation = mode.removeParas.nameOrLocation;
            if (nameOrLocation == null || nameOrLocation.isBlank())
                throw new IllegalArgumentException("Database location to remove not specified!");

            String location = nameOrLocationToLocation(nameOrLocation);
            try {
                CustomDatabase db = CustomDatabases.open(location, version);
                CustomDatabases.remove(db, mode.removeParas.delete);
            } catch (Exception e) {
                logWarn("\n==> Error opening database " + nameOrLocation + ":\n" + e.getMessage() + "\nIt will be removed from custom databases.");
            }
            CustomDBPropertyUtils.removeDBbyLocation(location);

            return true;
        }

        private @NotNull Boolean showAllDBs(CdkFingerprintVersion version) {
            LinkedHashMap<String, String> customDBs = CustomDBPropertyUtils.getCustomDBs();

            if (customDBs.isEmpty()) {
                logWarn("\n==> No Custom database found!\n");
                return false;
            }

            for (Map.Entry<String, String> e : customDBs.entrySet()) {
                String location = e.getKey();
                String name = e.getValue();
                try {
                    CustomDatabase db = CustomDatabases.open(location, version);
                    printDBInfo(db);
                } catch (Exception ex) {
                    printDBError(location, name, ex.getMessage());
                }
            }

            return true;
        }

        private boolean showDB(CdkFingerprintVersion version) {
            String location = nameOrLocationToLocation(mode.showParas.db);
            try {
                CustomDatabase db = CustomDatabases.open(location, version);
                printDBInfo(db);
            } catch (Exception ex) {
                printDBError(location, CustomDBPropertyUtils.getCustomDBs().get(location), ex.getMessage());
            }
            return true;
        }


        @Override
        public void cancel() {
            cancel(false);
            if (dbjob != null)
                dbjob.cancel();
        }
    }

    private void printDBInfo(CustomDatabase db) {
        CustomDatabaseSettings s = db.getSettings();
        System.out.println("##########  BEGIN DB INFO  ##########");
        System.out.println("Name: " + db.name());
        System.out.println("Display Name: " + db.displayName());
        System.out.println("Location: " + db.storageLocation());
        System.out.println("Number of Formulas: " + s.getStatistics().getFormulas());
        System.out.println("Number of Structures: " + s.getStatistics().getCompounds());
        System.out.println("Number of Reference spectra: " + s.getStatistics().getSpectra());
        if (mode.showParas.details) {
            System.out.println("Version: " + db.getDatabaseVersion());
            System.out.println("Schema Version: " + s.getSchemaVersion());
            System.out.println("FilterFlag: " + db.getFilterFlag());
            System.out.println("Used Fingerprints: [ '" + s.getUsedFingerprints().stream().map(Enum::name).collect(Collectors.joining("','")) + "' ]");
        }
        System.out.println("###############  END  ###############");
        System.out.println();
        System.out.println();
    }

    private void printDBError(String location, String name, String error) {
        System.out.println("#####  Error Opening Database  #####");
        System.out.println("Name: " + name);
        System.out.println("Location: " + location);
        System.out.println("Error: " + error);
        System.out.println("###############  END  ###############");
        System.out.println();
        System.out.println();
    }

    private String nameOrLocationToLocation(String nameOrLocation) {
        LinkedHashMap<String, String> existingDBs = CustomDBPropertyUtils.getCustomDBs();
        if (existingDBs.containsKey(nameOrLocation)) {
            return nameOrLocation;
        } else if (existingDBs.containsValue(nameOrLocation)) {
            return CustomDBPropertyUtils.getLocationByName(existingDBs, nameOrLocation).orElseThrow();
        } else {
            throw new IllegalArgumentException("Database " + nameOrLocation + " is not found.");
        }
    }
}
