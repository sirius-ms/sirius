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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseFactory;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseImporter;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseSettings;
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
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.storage.blob.Compressible;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * This is for parameters needed to create a custom DB.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "custom-db", aliases = {"DB"}, description = "@|bold %n<STANDALONE> Generate a custom searchable structure/spectral database. Import multiple files with compounds into this DB. %n %n|@", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
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
        @Option(names = "--import", required = true,
                description = {"Location of the custom database to import into.",
                        "An absolute local path to a new database file file to be created (file name must end with .db)",
                        "If no input data is given (--input), the database will just be added to SIRIUS",
                        "The added db will also be available in the GUI."
                }, order = 201)
        String location = null;

        @Option(names = {"--buffer-size", "--buffer"}, defaultValue = "1000",
                description = {"Maximum number of downloaded/computed compounds to keep in memory before writing them to disk (into the db directory). Can be set higher when importing large files on a fast computer."},
                order = 210)
        public int writeBuffer;

        @Option(names = {"--input", "-i"}, split = ",", description = {
                "Files to import into the new database.",
                "TODO describe formats" // TODO add, tooltip
        }, order = 220)
        public List<Path> files;
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
    public Workflow makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
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
            try {
                SiriusJobs.getGlobalJobManager().submitJob(this).awaitResult();
            } catch (ExecutionException e) {
                LoggerFactory.getLogger(CustomDatabaseImporter.class).error("error when storing custom db", e);
            }
        }

        @Override
        protected Boolean compute() throws Exception {

            if (mode.importParas != null) {
                if (mode.importParas.location == null || mode.importParas.location.isBlank()) {
                    logWarn("\n==> No location data given! Nothing to do.\n");
                    return false;
                }

                checkForInterruption();

                CustomDatabaseSettings settings = new CustomDatabaseSettings(false, 0,
                        List.of(ApplicationCore.WEB_API.getCDKChemDBFingerprintVersion().getUsedFingerprints()), VersionsInfo.CUSTOM_DATABASE_SCHEMA, null);

                final CustomDatabase db = CustomDatabaseFactory.createOrOpen(mode.importParas.location, Compressible.Compression.NONE, settings);
                addDBToPropertiesIfNotExist(db);
                logInfo("Database added to SIRIUS. Use 'structure --db=\"" + db.storageLocation() + "\"' to search in this database."); // TODO check this

                if (mode.importParas.files == null || mode.importParas.files.isEmpty())
                    return true;

                Map<Boolean, List<Path>> groups = mode.importParas.files.stream().collect(Collectors.partitioningBy(p -> MsExperimentParser.isSupportedFileName(p.getFileName().toString())));
                List<Path> spectrumFiles = groups.get(true);
                List<Path> structureFiles = groups.get(false);

                logInfo("Importing new structures to custom database '" + mode.importParas.location + "'...");
                final AtomicLong lines = new AtomicLong(0);
                for (Path f : structureFiles)
                    lines.addAndGet(FileUtils.estimateNumOfLines(f));
                lines.addAndGet(spectrumFiles.size());

                final AtomicInteger count = new AtomicInteger(0);

                checkForInterruption();

                dbjob = db.importToDatabaseJob(
                        spectrumFiles.stream().map(Path::toFile).toList(),
                        structureFiles.stream().map(Path::toFile).toList(),
                        inChI -> updateProgress(0, Math.max(lines.intValue(), count.incrementAndGet() + 1), count.get(), "Importing '" + inChI.key2D() + "'"),
                        ApplicationCore.WEB_API, mode.importParas.writeBuffer
                );
                checkForInterruption();
                submitJob(dbjob).awaitResult();
                logInfo("...New structures imported to custom database '" + mode.importParas.location + "'.");

                return true;
            } else if (mode.removeParas != null) {
                if (mode.removeParas.location == null ||mode.removeParas.location.isBlank())
                    throw new IllegalArgumentException("Database location to remove not specified!");

                SearchableDatabases.getCustomDatabase(mode.removeParas.location).ifPresentOrElse(db -> {
                            removeDBFromProperties(db);
                            if (mode.removeParas.delete) {
                                try {
                                    CustomDatabaseFactory.delete(db);
                                } catch (IOException e) {
                                    logError("Error deleting database.", e);
                                }
                            }
                        }, () -> logWarn("\n==> No custom database with location '" + mode.removeParas.location + "' found.\n")
                );
                return true;
            } else if (mode.showParas != null) {
                if (mode.showParas.db == null) {
                    @NotNull List<CustomDatabase> dbs = SearchableDatabases.getCustomDatabases();
                    if (dbs.isEmpty()){
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
                    SearchableDatabases.getCustomDatabase(mode.showParas.db)
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
            if (dbjob != null)
                dbjob.cancel();
            if (sjob != null)
                sjob.cancel();
            cancel(false);
        }
    }

    private void printDBInfo(CustomDatabase db) {
        CustomDatabaseSettings s = db.getSettings();
        System.out.println("##########  BEGIN DB INFO  ##########");
        System.out.println("Name: " + db.name());
        System.out.println("Location: " + db.storageLocation());
        System.out.println("Number of Formulas: " + s.getStatistics().getFormulas());
        System.out.println("Number of Structures: " + s.getStatistics().getCompounds());
        System.out.println("Number of Reference spectra: " + s.getStatistics().getSpectra());
        if (mode.showParas.details) {
            System.out.println("Version: " + db.getDatabaseVersion());
            System.out.println("Schema Version: " + s.getSchemaVersion());
            System.out.println("FilterFlag: " + db.getFilterFlag());
            System.out.println("Used Fingerprints: [ '" + s.getFingerprintVersion().stream().map(Enum::name).collect(Collectors.joining("','")) + "' ]");
        }
        System.out.println("###############  END  ###############");
    }

    public static void addDBToPropertiesIfNotExist(@NotNull CustomDatabase db) {
        Set<CustomDatabase> customs = new HashSet<>(SearchableDatabases.getCustomDatabases());
        if (!customs.contains(db)) {
            customs.add(db);
            writeDBProperties(customs);
        }
    }

    public static void removeDBFromProperties(@NotNull CustomDatabase db) {
        Set<CustomDatabase> customs = new HashSet<>(SearchableDatabases.getCustomDatabases());
        customs.remove(db);
        writeDBProperties(customs);
    }

    private static void writeDBProperties(Collection<CustomDatabase> dbs) {
        SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(SearchableDatabases.PROP_KEY, dbs.stream()
                .sorted(Comparator.comparing(CustomDatabase::name))
                .map(CustomDatabase::storageLocation)
                .collect(Collectors.joining(",")));
    }


}
