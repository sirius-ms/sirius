/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.spectra_db;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDBs;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.spectraldb.io.SpectralDbMsExperimentParser;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

@CommandLine.Command(name = "spectral-db", aliases = {"SDB"}, description = "<STANDALONE> Generate a custom spectral database. Import multiple MSn reference spectra into this DB. At the moment, supports only the MassBank file format. Required file fields are MS$FOCUSED_ION: PRECURSOR_M/Z, CH$LINK: INCHIKEY, and PK$PEAK:.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class SpectralDBOptions implements StandaloneTool<Workflow> {

    protected final static Logger logger = LoggerFactory.getLogger(SpectralDBOptions.class);

    @CommandLine.ArgGroup(exclusive = false)
    ModeParas mode;

    public static class ModeParas {
        @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n List spectral database(s): %n|@", order = 100)
        Show showParas;
        @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n Import spectral database: %n|@", order = 200)
        public Import importParas;
        @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n Remove spectral database: %n|@", order = 300)
        Remove removeParas;

    }

    public static class Import {

        @CommandLine.Option(names = "--import", required = true,
                description = {"Location of the custom database to import into.",
                        "If no input data is given (global --input) the database will just be added to SIRIUS"
                }, order = 201)
        public String location = null;

        @CommandLine.Option(names = {"--buffer-size", "--buffer"}, defaultValue = "1000",
                description = {"Maximum number of imported spectra to keep in memory before writing them to disk (into the db)."},
                order = 210)
        public int writeBuffer;

    }

    public static class Remove {
        @CommandLine.Option(names = "--remove", required = true,
                description = "Name (--show) or path of the custom database to remove from SIRIUS.",
                order = 301)
        String location = null;

        @CommandLine.Option(names = {"--delete", "-d"}, required = false, defaultValue = "false",
                description = "Delete removed custom database from filesystem/server.", order = 310)
        boolean delete;
    }

    public static class Show {
        @CommandLine.Option(names = "--show", required = true, order = 101)
        boolean showDBs;

        @CommandLine.Option(names = {"--db"}, description = "Show information only about the given custom database.", order = 110)
        String db = null;

    }

    @Override
    public Workflow makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return new SpectralDBWorkflow(rootOptions.getInput(), mode);
    }

    private static class ParsingIterator implements Iterator<Ms2Experiment> {

        private final Iterator<File> files;

        private final SpectralDbMsExperimentParser parser;
        private final Map<String, GenericParser<Ms2Experiment>> parsers;

        private final Queue<Ms2Experiment> buffer = new ArrayDeque<>();

        public ParsingIterator(Iterator<File> files) {
            this.files = files;
            this.parsers = new HashMap<>();
            this.parser = new SpectralDbMsExperimentParser();
        }

        @Override
        public boolean hasNext() {
            if (buffer.isEmpty()) {
                while (files.hasNext()) {
                    try {
                        File next = files.next();
                        String ext = FilenameUtils.getExtension(next.getName());
                        if (!parsers.containsKey(ext)) {
                            parsers.put(ext, parser.getParserByExt(ext));
                        }
                        buffer.addAll(parsers.get(ext).parseFromFile(next));
                        break;
                    } catch (Exception ignored) {
                    }

                }
            }
            return buffer.size() > 0;
        }

        @Override
        public Ms2Experiment next() {
            return buffer.poll();
        }

    }

    public static class SpectralDBWorkflow extends BasicJJob<Boolean> implements Workflow {

        final InputFilesOptions input;

        final ModeParas mode;

        public SpectralDBWorkflow(InputFilesOptions input, ModeParas mode) {
            super(JJob.JobType.SCHEDULER);
            this.input = input;
            this.mode = mode;
        }

        @Override
        protected Boolean compute() throws Exception {
            if (mode.importParas != null) {
                if (mode.importParas.location == null || mode.importParas.location.isBlank()) {
                    logger.warn("\n==> No location data given! Nothing to do.\n");
                    return false;
                }

                checkForInterruption();

                final SpectralLibrary db = SpectralDatabases.createAndAddSpectralLibrary(Path.of(mode.importParas.location));
                logger.info("Database added to SIRIUS. Use 'spectra-search --db=\"" + db.location() + "\"' to search in this database.");

                if (input != null && input.msInput != null && !input.msInput.msParserfiles.isEmpty()) {
                    logger.info("Importing new spectra to database '" + mode.importParas.location + "'...");

                    Set<Path> files = input.msInput.msParserfiles.keySet();

                    Iterator<Ms2Experiment> iterator = new ParsingIterator(files.stream().map(Path::toFile).iterator());
                    try {
                        int count = SpectralNoSQLDBs.importSpectraFromMs2Experiments((SpectralNoSQLDatabase<?>) db, () -> iterator, mode.importParas.writeBuffer);

                        checkForInterruption();
                        logger.info(count + " new spectra successfully imported to database '" + mode.importParas.location + "'.");
                        logger.info("Total number of spectra in database '" + mode.importParas.location + "': " + db.countAllSpectra());
                    } catch (ChemicalDatabaseException e) {
                        throw new RuntimeException(e);
                    }
                }
                return true;
            }
            else if (mode.removeParas != null) {
                if (mode.removeParas.location == null ||mode.removeParas.location.isBlank())
                    throw new IllegalArgumentException("Database location to remove not specified!");

                SpectralDatabases.getSpectralLibrary(Path.of(mode.removeParas.location)).ifPresentOrElse(db -> {
                        try {
                            logger.info("Removing database '" + db.location() + "' from project space.\n");
                            SpectralDatabases.removeDB(db);

                            if (mode.removeParas.delete) {
                                logger.info("Deleting file '" + db.location() + "'.\n");
                                Files.delete(Path.of(mode.removeParas.location));
                            }
                        } catch (IOException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }, () -> logger.warn("\n==> No spectral database with location '" + mode.removeParas.location + "' found.")
                );
                return true;
            } else if (mode.showParas != null) {
                if (mode.showParas.db == null) {
                    @NotNull Collection<SpectralLibrary> dbs = SpectralDatabases.listSpectralLibraries();
                    if (dbs.isEmpty()){
                        logger.warn("\n==> No spectral database found!");
                        return false;
                    }

                    for (SpectralLibrary db : dbs) {
                        printDBInfo(db);
                    }
                    return true;
                } else {
                    SpectralDatabases.getSpectralLibrary(Path.of(mode.showParas.db))
                            .ifPresentOrElse(db -> {
                                        try {
                                            printDBInfo(db);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    },
                                    () -> logger.warn("\n==> No spectral database with location '" + mode.showParas.db + "' found."));
                    return false;
                }
            }
            throw new IllegalArgumentException("Either '--import', '--remove' or '--show' must be specified.");
        }

        @Override
        public void run() {
            try {
                SiriusJobs.getGlobalJobManager().submitJob(this).awaitResult();
            } catch (ExecutionException e) {
                logger.error("error when storing spectral db", e);
            }
        }

        @Override
        public void cancel() {
            cancel(false);
        }

        private void printDBInfo(SpectralLibrary db) throws IOException {
            logger.info(
                "\n##########  BEGIN DB INFO  ##########\n" +
                "Name: " + db.name() + "\n" +
                "Location: " + db.location() + "\n" +
                "Number of Spectra: " + db.countAllSpectra() + "\n" +
                "###############  END  ###############");
        }

    }



}
