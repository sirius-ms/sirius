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
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n Remove custom database: %n|@", order = 300)
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
    }

    public static class Remove {
        @Option(names = "--remove", required = true,
                description = "Name (--show) or path of the custom database to remove from SIRIUS.",
                order = 301)
        String location = null;

        @Option(names = {"--delete", "-d"}, defaultValue = "false",
                description = "Delete removed custom database from filesystem/server.", order = 310)
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

        private JJob<Boolean> sjob = null;

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
            //loads all current available dbs
            final @NotNull List<CustomDatabase> dbs = CustomDatabases.getCustomDatabases(version);

            if (mode.importParas != null) {
                if (mode.importParas.location == null || mode.importParas.location.isBlank()) {
                    logWarn("\n==> No location data given! Nothing to do.\n");
                    return false;
                }

                checkForInterruption();

                if (mode.importParas.name == null || mode.importParas.name.isBlank())
                    mode.importParas.name = CustomDatabases.sanitizeDbName(Path.of(mode.importParas.location.substring(0, mode.importParas.location.lastIndexOf('.'))).getFileName().toString());

                checkConflictingName(mode.importParas.location, mode.importParas.name);

                CustomDatabaseSettings settings = CustomDatabaseSettings.builder()
                        .usedFingerprints(List.of(version.getUsedFingerprints()))
                        .schemaVersion(CustomDatabase.CUSTOM_DATABASE_SCHEMA)
                        .name(mode.importParas.name)
                        .displayName(mode.importParas.displayName)
                        .matchRtOfReferenceSpectra(false)
                        .statistics(new CustomDatabaseSettings.Statistics())
                        .build();

                final CustomDatabase db = CustomDatabases.createOrOpen(mode.importParas.location, settings, version);
                writeDBProperties();

                logInfo("Database added to SIRIUS. Use 'structure --db=\"" + db.storageLocation() + "\"' to search in this database.");

                if (mode.importParas.input == null || mode.importParas.input.isEmpty())
                    return true;

                Map<Boolean, List<Path>> groups = mode.importParas.input.stream()
                        .flatMap(FileUtils::sneakyWalk)
                        .filter(Files::isRegularFile)
                        .distinct()
                        .collect(Collectors.partitioningBy(p -> MsExperimentParser.isSupportedFileName(p.getFileName().toString())));

                List<Path> spectrumFiles = groups.get(true);
                List<Path> structureFiles = groups.get(false);

                logInfo("Importing new structures to custom database '" + mode.importParas.location + "'...");

                final AtomicLong totalBytesToRead = new AtomicLong(0);
                for (Path file : structureFiles)
                    totalBytesToRead.addAndGet(Files.size(file));
                for (Path file : spectrumFiles)
                    totalBytesToRead.addAndGet(Files.size(file));

                totalBytesToRead.set((long) Math.ceil(totalBytesToRead.get() * 1.6)); //just some preloading to do last inserts


                final AtomicLong bytesRead = new AtomicLong(0);
                CustomDatabaseImporter.Listener listener = new CustomDatabaseImporter.Listener() {
                    @Override
                    public void newFingerprint(InChI inChI, int numOfBytes) {
                        updateProgress(0, totalBytesToRead.addAndGet(numOfBytes), bytesRead.addAndGet(numOfBytes), "Added FP for " + inChI.key2D());
                    }

                    @Override
                    public void bytesRead(int numOfBytes) {
                        updateProgress(0, totalBytesToRead.get(), bytesRead.addAndGet(numOfBytes), "Reading Data...");
                    }

                    @Override
                    public void newInChI(List<InChI> inChIs) {
                        progressInfo("Imported " + inChIs.size() + " Compounds.");
//                        updateProgress(0, totalBytesToRead.addAndGet(inChIs.size()), bytesRead.addAndGet(inChIs.size()), "Imported: " + inChIs.stream().map(InChI::key2D).collect(Collectors.joining(", ")));
                    }
                };

                checkForInterruption();

                dbjob = CustomDatabaseImporter.makeImportToDatabaseJob(
                        spectrumFiles.stream().map(PathInputResource::new).collect(Collectors.toList()),
                        structureFiles.stream().map(PathInputResource::new).collect(Collectors.toList()),
                        listener,(NoSQLCustomDatabase<?, ?>) db, ApplicationCore.WEB_API,
                        ApplicationCore.IFP_CACHE(),
                        mode.importParas.writeBuffer
                );
                checkForInterruption();
                submitJob(dbjob).awaitResult();
                logInfo("...New structures imported to custom database '" + mode.importParas.location + "'. Database ID is: " + db.getSettings().getName());
                return true;
            } else if (mode.removeParas != null) {
                if (mode.removeParas.location == null || mode.removeParas.location.isBlank())
                    throw new IllegalArgumentException("Database location to remove not specified!");

                CustomDatabases.getCustomDatabase(mode.removeParas.location, version)
                        .ifPresentOrElse(db -> {
                                    CustomDatabases.remove(db, mode.removeParas.delete);
                                    writeDBProperties();
                                }, () -> logWarn("\n==> No custom database with location '" + mode.removeParas.location + "' found.\n")
                        );
                return true;
            } else if (mode.showParas != null) {
                if (mode.showParas.db == null) {
                    if (dbs.isEmpty()) {
                        logWarn("\n==> No Custom database found!\n");
                        return false;
                    }

                    dbs.forEach(db -> {
                        printDBInfo(db);
                        System.out.println();
                        System.out.println();
                    });
                    return true;
                } else {
                    CustomDatabases.getCustomDatabase(mode.showParas.db, version)
                            .ifPresentOrElse(
                                    CustomDBOptions.this::printDBInfo,
                                    () -> logWarn("\n==> No custom database with location '" + mode.showParas.db + "' found.\n"));
                    return false;
                }
            }
            throw new IllegalArgumentException("Either '--import', '--remove' or '--show' must be specified.");
        }

        @Override
        public void cancel() {
            cancel(false);
            if (dbjob != null)
                dbjob.cancel();
            if (sjob != null)
                sjob.cancel();
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
    }

    public static void writeDBProperties() {
        SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(CustomDataSources.PROP_KEY, CustomDataSources.sourcesStream()
                .filter(CustomDataSources.Source::isCustomSource)
                .map(c -> (CustomDataSources.CustomSource) c)
                .sorted(Comparator.comparing(CustomDataSources.Source::name))
                .map(CustomDataSources.CustomSource::location)
                .collect(Collectors.joining(",")));
    }

    private static void checkConflictingName(@NotNull String location, @NotNull String dbName) {
        CustomDataSources.sourcesStream().filter(CustomDataSources.Source::isCustomSource)
                .filter(db -> db.name().equals(dbName) && !location.equals(db.isCustomSource() ? ((CustomDataSources.CustomSource) db).location() : null))
                .findAny()
                .ifPresent(db -> {
                    throw new RuntimeException("Database with name " + dbName + " already exists in " + db.URI());
                });
    }

}
