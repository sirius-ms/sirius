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

package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * This is for not algorithm related parameters.
 * <p>
 * That means parameters that do not influence computation and do not
 * need to be Annotated to the MS2Experiment, e.g. standard commandline
 * stuff, technical parameters (cores) or input/output.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "sirius", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false, showDefaultValues = true)
public class CLIRootOptions implements RootOptions<PreprocessingJob<? extends ProjectSpaceManager>> {
    public static final Logger LOG = LoggerFactory.getLogger(CLIRootOptions.class);

    @Getter
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    @Getter
    @Setter
    protected ProjectSpaceManagerFactory<? extends ProjectSpaceManager> spaceManagerFactory;

    public CLIRootOptions(@NotNull DefaultParameterConfigLoader defaultConfigOptions, ProjectSpaceManagerFactory<? extends ProjectSpaceManager> spaceManagerFactory) {
        this.defaultConfigOptions = defaultConfigOptions;
        this.spaceManagerFactory = spaceManagerFactory;
    }

    public enum LogLevel {
        SEVERE(Level.SEVERE), WARNING(Level.WARNING), INFO(Level.INFO), FINER(Level.FINER), ALL(Level.ALL);
        public final Level level;

        LogLevel(Level level) {
            this.level = level;
        }
    }

    @Option(names = {"--log", "--loglevel"}, description = "Set logging level of the Jobs SIRIUS will execute. Valid values: ${COMPLETION-CANDIDATES}", order = 5, defaultValue = "WARNING")
    public void setLogLevel(final LogLevel loglevel) {
        Optional.ofNullable(LoggerFactory.getLogger(JJob.DEFAULT_LOGGER_KEY)).map(Logger::getName)
                .map(LogManager.getLogManager()::getLogger).map(java.util.logging.Logger::getHandlers)
                .map(Arrays::stream)
                .flatMap(s -> s.filter(h -> h instanceof ConsoleHandler).findFirst())
                .ifPresent(h -> h.setFilter(r -> {
                    if (r.getLoggerName().equals(JJob.DEFAULT_LOGGER_KEY))
                        return r.getLevel().intValue() >= loglevel.level.intValue();
                    return true;
                }));

    }

    @Option(names = {"--threads", "--cores", "--processors"}, description = "Number of simultaneous worker thread to be used for compute intense workload. If not specified SIRIUS chooses a reasonable number based you CPU specs.", order = 10)
    public void setNumOfCores(int numOfCores) {
        if(numOfCores < 3){
            LOG.warn("Number of Cores must be at least 3. Specified: {}. Using 3 instead.", numOfCores);
            numOfCores = 3;
        }

        PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(numOfCores));
        SiriusJobs.setGlobalJobManager(numOfCores);
        if (instanceBuffer == null)
            setInitialInstanceBuffer(0);
        LOG.info("Adjusted JobManager CPU threads to '{}' by command line.", SiriusJobs.getGlobalJobManager().getCPUThreads());
    }

    @Option(names = {"--buffer", "--instance-buffer"}, defaultValue = "0", description = "Number of instances that will be loaded into the Memory. A larger buffer ensures that there are enough instances available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all instances immediately set it to -1. Default (numeric value 0): 3 x --cores. Note that for <DATASET_TOOLS> the compound buffer may have no effect because this tools may have to load compounds simultaneously into the memory.", order = 20)
    public void setInitialInstanceBuffer(int initialInstanceBuffer) {
        this.instanceBuffer = initialInstanceBuffer;
        if (instanceBuffer == 0) {
            instanceBuffer = 5 * SiriusJobs.getGlobalJobManager().getCPUThreads();
        }

        PropertyManager.setProperty("de.unijena.bioinf.sirius.instanceBuffer", String.valueOf(instanceBuffer));
    }

    private Integer instanceBuffer = null;

    @Option(names = {"--workspace"}, description = "Specify sirius workspace location. This is the directory for storing Property files, logs, databases and caches.  This is NOT for the project-space that stores the results! Default is $USER_HOME/.sirius-<MINOR_VERSION>", order = 30)
    public void setWorkspace(File ws) {
        PropertyManager.setProperty("de.unijena.bioinf.sirius.ws.location", ws == null ? null : ws.getAbsolutePath());
    }

    public Files workspace; //todo change in application core

    @Option(names = "--recompute", descriptionKey = "RecomputeResults", description = "Recompute results of ALL tools where results are already present. Per default already present results will be preserved and the instance will be skipped for the corresponding Task/Tool", order = 100)
    public void setRecompute(boolean para) throws Exception {
        defaultConfigOptions.changeOption("RecomputeResults", para);
    }

    @Option(names = "--maxmz", description = "Only considers compounds with a precursor m/z lower or equal [--maxmz]. All other compounds in the input will be skipped.", defaultValue = "Infinity", order = 110)
    public double maxMz;


    @Option(names = {"--no-citations", "--noCitations", "--noCite"}, description = "Do not write summary files to the project-space", order = 299)
    private void setNoCitationInfo(boolean noCitations) throws Exception {
        PropertyManager.DEFAULTS.changeConfig("PrintCitations", String.valueOf(!noCitations)); //this is a bit hacky
    }

    @Option(names = {"--no-project-check"}, description = "Disable compatibility check for the project-space.", order = 300, hidden = true)
    private void setSkipProjectCheck(boolean noProjectCheck) throws Exception {
        PropertyManager.setProperty("de.unijena.bioinf.sirius.project-check", String.valueOf(noProjectCheck)); //this is a bit hacky
    }
    //endregion

    // region Options: INPUT/OUTPUT
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--zip-provider"}, description = "Specify the Provider for handling zip compressed resources (e.g. project-space). Valid values: ${COMPLETION-CANDIDATES}", hidden = true, order = 298)
    private void setZipProvider(ZipProvider provider) throws Exception {
        PropertyManager.setProperty("de.unijena.bioinf.sirius.project.zipProvider", provider.name());
    }

    @CommandLine.ArgGroup(exclusive = false, heading = "@|bold Specify OUTPUT Project-Space: %n|@", order = 200)
    private OutputOptions psOpts = new OutputOptions();

    @Override
    public OutputOptions getOutput() {
        return psOpts;
    }

    @CommandLine.ArgGroup(exclusive = false, order = 300)
    private InputFilesOptions inputFiles;

    @Override
    public InputFilesOptions getInput() {
        return inputFiles;
    }

    //endregion

    // region Options: Quality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = "--noise", description = "Median intensity of noise peaks", order = 500, hidden = true)
    public Double medianNoise;

    @Option(names = {"--assess-data-quality"}, description = "produce stats on quality of spectra and estimate isolation window. Needs to read all data at once.", order = 510, hidden = true)
    public boolean assessDataQuality;
    //endregion

    @Override
    public @NotNull PreprocessingJob<? extends ProjectSpaceManager> makeDefaultPreprocessingJob() {
        return new PreprocessingJob<>() {
            @Override
            protected ProjectSpaceManager compute() throws Exception {
                ProjectSpaceManager space = spaceManagerFactory.createOrOpen(psOpts.getOutputProjectLocation());;
                InputFilesOptions input = getInput();
                if (space != null) {
                    if (input != null)
                        SiriusJobs.getGlobalJobManager().submitJob(new InstanceImporter(space, (exp) -> exp.getIonMass() < maxMz).makeImportJJob(input)).awaitResult();
                    if (space.size() < 1)
                        logInfo("No Input has been imported to Project-Space. Starting application without input data.");
                    return space;
                }
                throw new CommandLine.PicocliException("No Project-Space for writing output!");
            }
        };
    }
}
