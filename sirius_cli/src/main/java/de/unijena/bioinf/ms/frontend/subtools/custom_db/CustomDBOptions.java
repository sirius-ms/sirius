/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseImporter;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
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

    @Option(names = "--name", required = true,
            description = {"Name of the custom database. It will be stored in '$SIRIUS_WORKSPACE/csi_fingerid_cache/custom'. The Location of the SIRIUS workspace can be set by (--workspace)."})
    public String dbName;

    @Option(names = "--output",
            description = {"Alternative output directory of the custom database. The db will be a sub directory with the given name (--name).", "Default: '$USER_HOME/.sirius/csi_fingerid_cache/custom'"})
    public Path outputDir = null;

    @Option(names = {"--buffer-size", "--buffer"}, defaultValue = "1000",
            description = {"Maximum number of downloaded/computed compounds to keep in memory before writing them to disk (into the db directory)."})
    public int writeBuffer;

    @Option(names = {"--derive-from"}, split = ",",
            description = {"The resulting custom-db will be the Union of the given parent database and the imported structures."})
    public EnumSet<DataSource> parentDBs = null;

    @Override
    public Workflow makeWorkflow(RootOptions<?, ?, ?> rootOptions, ParameterConfig config) {
        return new CustomDBWorkflow(rootOptions.getInput());
    }


    public class CustomDBWorkflow extends BasicJJob<Boolean> implements Workflow{
        final InputFilesOptions input;
        public CustomDBWorkflow(InputFilesOptions input) {
            super(JJob.JobType.SCHEDULER, "CustomDatabaseImporter");
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

            if (dbName == null || dbName.isEmpty() || input == null || input.msInput == null || input.msInput.unknownFiles.isEmpty()) {
                logError("No input data given. Do nothing");
                return false;
            }

            final AtomicLong lines = new AtomicLong(0);
            for (Path f : input.msInput.unknownFiles)
                lines.addAndGet(FileUtils.estimateNumOfLines(f));

            final AtomicInteger count = new AtomicInteger(0);

            Path loc = outputDir != null ? outputDir : SearchableDatabases.getCustomDatabaseDirectory().toPath();
            Files.createDirectories(loc);
            CustomDatabaseImporter.importDatabase(loc.resolve(dbName).toFile(),
                    input.msInput.unknownFiles.stream().map(Path::toFile).collect(Collectors.toList()),
                    parentDBs,
                    ApplicationCore.WEB_API, writeBuffer,
                    inChI -> updateProgress(0, Math.max(lines.intValue(), count.incrementAndGet() + 1), count.get(), "Importing '" + inChI.key2D() + "'")
            );
            logInfo("Database imported. Use 'structure --db=\"" + loc.resolve(dbName).toString() + "\"' to search in this database.");
            return true;
        }

        @Override
        public void cancel() {
            cancel(true);
        }
    }
}
