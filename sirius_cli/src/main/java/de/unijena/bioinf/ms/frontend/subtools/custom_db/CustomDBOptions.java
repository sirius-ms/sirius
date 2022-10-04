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
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.DataSources;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
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
@CommandLine.Command(name = "custom-db", aliases = {"DB"}, description = "<STANDALONE> Generate a custom searchable structure database. Import multiple files with compounds as SMILES or InChi into this DB.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class CustomDBOptions implements StandaloneTool<Workflow> {

    @Option(names = "--location", required = true,
            description = {"Name or Location of the custom database.",
                    "If just a name is given the db will be stored locally in '$SIRIUS_WORKSPACE/csi_fingerid_cache/custom'.",
                    "A location can either be a absolute local path or a cloud storage location e.g. 's3://my-bucket",
                    "The Location of the SIRIUS workspace can be set by (--workspace)."})
    public String location;


    @Option(names = {"--buffer-size", "--buffer"}, defaultValue = "1000",
            description = {"Maximum number of downloaded/computed compounds to keep in memory before writing them to disk (into the db directory)."})
    public int writeBuffer;

    @Option(names = {"--derive-from"}, split = ",",
            description = {"The resulting custom-db will be the Union of the given parent database and the imported structures."})
    public EnumSet<DataSource> parentDBs = EnumSet.noneOf(DataSource.class);


    @Option(names = {"--compression", "-c"}, description = {"Specify compression mode."}, defaultValue = "GZIP")
    public Compressible.Compression compression;

    @Override
    public Workflow makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
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
            try {
                SiriusJobs.getGlobalJobManager().submitJob(this).awaitResult();
            } catch (ExecutionException e) {
                LoggerFactory.getLogger(CustomDatabaseImporter.class).error("error when storing custom db", e);
            }
        }

        @Override
        protected Boolean compute() throws Exception {

            if (location == null || location.isBlank() || input == null || input.msInput == null || input.msInput.unknownFiles.isEmpty()) {
                logWarn("No input data given. Do nothing");
                return false;
            }

            checkForInterruption();

            final AtomicLong lines = new AtomicLong(0);
            final List<Path> unknown = input.msInput.unknownFiles.keySet().stream().sorted().collect(Collectors.toList());
            for (Path f : unknown)
                lines.addAndGet(FileUtils.estimateNumOfLines(f));

            final AtomicInteger count = new AtomicInteger(0);

            checkForInterruption();

            CustomDatabaseSettings settings = new CustomDatabaseSettings(!parentDBs.isEmpty(), DataSources.getDBFlag(parentDBs),
                    List.of(ApplicationCore.WEB_API.getCDKChemDBFingerprintVersion().getUsedFingerprints()), VersionsInfo.CUSTOM_DATABASE_SCHEMA, null);

            CustomDatabase<?> db = CustomDatabase.createOrOpen(location, compression, settings);
            addDBToPropertiesIfNotExist(db);

            dbjob = db.importToDatabaseJob(
                    unknown.stream().map(Path::toFile).collect(Collectors.toList()),
                    inChI -> updateProgress(0, Math.max(lines.intValue(), count.incrementAndGet() + 1), count.get(), "Importing '" + inChI.key2D() + "'"),
                    ApplicationCore.WEB_API, writeBuffer

            );
            checkForInterruption();
            submitJob(dbjob).awaitResult();
            logInfo("Database imported. Use 'structure --db=\"" + db.storageLocation() + "\"' to search in this database.");
            return true;
        }

        @Override
        public void cancel() {
            if (dbjob != null)
                dbjob.cancel();
            cancel(false);
        }
    }

    public static void addDBToPropertiesIfNotExist(@NotNull CustomDatabase<?> db) {
        Set<CustomDatabase<?>> customs = new HashSet<>(SearchableDatabases.getCustomDatabases());
        if (!customs.contains(db)) {
            customs.add(db);
            SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(SearchableDatabases.PROP_KEY, customs.stream()
                    .sorted(Comparator.comparing(CustomDatabase::name))
                    .map(CustomDatabase::storageLocation)
                    .collect(Collectors.joining(",")));
        }
    }
}
