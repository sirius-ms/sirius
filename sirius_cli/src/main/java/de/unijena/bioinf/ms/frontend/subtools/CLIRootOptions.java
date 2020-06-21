package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.annotations.WriteSummaries;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;

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
public class CLIRootOptions<M extends ProjectSpaceManager> implements RootOptions<M, PreprocessingJob<M>, PostprocessingJob<Boolean>> {
    public static final Logger LOG = LoggerFactory.getLogger(CLIRootOptions.class);

    protected final DefaultParameterConfigLoader defaultConfigOptions;
    protected final ProjectSpaceManagerFactory<M> spaceManagerFactory;

    public CLIRootOptions(@NotNull DefaultParameterConfigLoader defaultConfigOptions, @NotNull ProjectSpaceManagerFactory<M> spaceManagerFactory) {
        this.defaultConfigOptions = defaultConfigOptions;
        this.spaceManagerFactory = spaceManagerFactory;
    }

    // region Options: Basic
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--cores", "--processors"}, description = "Number of cpu cores to use. If not specified Sirius uses all available cores.", order = 10)
    public void setNumOfCores(int numOfCores) {
        PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(numOfCores));
        SiriusJobs.setGlobalJobManager(numOfCores);
        if (instanceBuffer < 0)
            setInitialInstanceBuffer(-1);
    }

    @Option(names = {"--compound-buffer", "--initial-compound-buffer"}, defaultValue = "-1", description = "Number of compounds that will be loaded into the Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all compounds immediately set it to 0. Default: 3 x --cores. Note that for <DATASET_TOOLS> the compound buffer may have no effect because this tools may have to load compounds simultaneously into the memory.", order = 20)
    public void setInitialInstanceBuffer(int initialInstanceBuffer) {
        this.instanceBuffer = initialInstanceBuffer;
        if (instanceBuffer < 0)
            instanceBuffer = 3 * SiriusJobs.getGlobalJobManager().getCPUThreads();

        PropertyManager.setProperty("de.unijena.bioinf.sirius.instanceBuffer", String.valueOf(instanceBuffer));
    }

    private int instanceBuffer = -1;

    @Option(names = {"--workspace", "-w"}, description = "Specify sirius workspace location. This is the directory for storing Property files, logs, databases and caches.  This is NOT for the project-space that stores the results! Default is $USER_HOME/.sirius", order = 30, hidden = true)
    public Files workspace; //todo change in application core

    @Option(names = "--recompute", descriptionKey = "RecomputeResults", description = "Recompute results of ALL tools where results are already present. Per default already present results will be preserved and the instance will be skipped for the corresponding Task/Tool", order = 100)
    public void setRecompute(boolean para) throws Exception {
        defaultConfigOptions.changeOption("RecomputeResults", para);
    }

    @Option(names = "--maxmz", description = "Only considers compounds with a precursor m/z lower or equal [--maxmz]. All other compounds in the input will be skipped.", defaultValue = "Infinity", order = 110)
    public double maxMz;

    //endregion


    // region Options: INPUT/OUTPUT
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--no-summaries", "--noSummaries"}, description = "Do not write summary files to the project-space", order = 299)
    private void setNoSummaries(boolean noSummaries) throws Exception {
        defaultConfigOptions.changeOption("WriteSummaries", !noSummaries);
    }


    @CommandLine.ArgGroup(exclusive = false, heading = "@|bold Specify OUTPUT Project-Space: %n|@", order = 200)
    private OutputOptions psOpts = new OutputOptions();

    @Override
    public OutputOptions getOutput() {
        return psOpts;
    }

    private M projectSpaceToWriteOn = null;

    public ProjectSpaceManagerFactory<M> getSpaceManagerFactory() {
        return spaceManagerFactory;
    }

    @Override
    public M getProjectSpace() {
        if (projectSpaceToWriteOn == null)
            projectSpaceToWriteOn = configureProjectSpace();

        return projectSpaceToWriteOn;
    }

    protected M configureProjectSpace() {
        try {
            if (psOpts.outputProjectLocation == null) {
                if (inputFiles != null && inputFiles.msInput.projects.size() == 1) {
                    psOpts.outputProjectLocation = (inputFiles.msInput.projects.get(0));
                    LOG.info("No output location given. Writing output to input location: " + psOpts.outputProjectLocation.toString());
                } else {
                    psOpts.outputProjectLocation = ProjectSpaceIO.createTmpProjectSpaceLocation();
                    LOG.warn("No unique output location found. Writing output to Temporary folder: " + psOpts.outputProjectLocation.toString());
                }
            }

            final SiriusProjectSpace psTmp;
            if (Files.notExists(psOpts.outputProjectLocation)) {
                psTmp = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createNewProjectSpace(psOpts.outputProjectLocation);
            } else {
                psTmp = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(psOpts.outputProjectLocation);
            }

            //check for formatter
            if (psOpts.projectSpaceFilenameFormatter == null) {
                try {
                    psOpts.projectSpaceFilenameFormatter = psTmp.getProjectSpaceProperty(FilenameFormatter.PSProperty.class).map(it -> new StandardMSFilenameFormatter(it.formatExpression)).orElse(new StandardMSFilenameFormatter());
                } catch (Exception e) {
                    LOG.warn("Could not Parse 'FilenameFormatter' -> Using default");
                    psOpts.projectSpaceFilenameFormatter = new StandardMSFilenameFormatter();
                }

                psTmp.setProjectSpaceProperty(FilenameFormatter.PSProperty.class, new FilenameFormatter.PSProperty(psOpts.projectSpaceFilenameFormatter));
            }

            final M space = spaceManagerFactory.create(psTmp, psOpts.projectSpaceFilenameFormatter);
            space.setCompoundIdFilter(cid -> {
                if (cid.getIonMass().orElse(Double.NaN) <= maxMz)
                    return true;
                else {
                    LOG.info("Skipping instance " + cid.toString() + " with mass: " + cid.getIonMass().orElse(Double.NaN) + " > " + maxMz);
                    return false;
                }
            });

            InstanceImporter.checkAndFixNegativeDataFiles(space.projectSpace(), ApplicationCore.WEB_API);
            return space;
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Could not initialize workspace!", e);
        }
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


    @NotNull
    @Override
    public PreprocessingJob<M> makeDefaultPreprocessingJob() {
        return new PreprocessingJob<>() {
            @Override
            protected M compute() throws Exception {
                M space = getProjectSpace();
                InputFilesOptions input = getInput();
                if (space != null) {
                    if (input != null)
                        SiriusJobs.getGlobalJobManager().submitJob(new InstanceImporter(space, (exp) -> exp.getIonMass() < maxMz, (c) -> true).makeImportJJob(input)).awaitResult();
                    if (space.size() < 1)
                        logInfo("No Input has been imported to Project-Space. Starting application without input data.");
                    return space;
                }
                throw new CommandLine.PicocliException("No Project-Space for writing output!");
            }
        };
    }

    @NotNull
    @Override
    public PostprocessingJob<Boolean> makeDefaultPostprocessingJob() {
        return new PostprocessingJob<>() {
            @Override
            protected Boolean compute() throws Exception {
                M project = getProjectSpace();
                try {
                    //remove recompute annotation since it should be cli only option
//                iteratorSource.forEach(it -> it.getExperiment().setAnnotation(RecomputeResults.class,null)); //todo fix needed?
                    //use all experiments in workspace to create summaries
                    if (defaultConfigOptions.config.createInstanceWithDefaults(WriteSummaries.class).value) {
                        LOG.info("Writing summary files...");
                        project.updateSummaries(ProjectSpaceManager.defaultSummarizer());
                        LOG.info("Project-Space summaries successfully written!");
                    }
                    return true;
                } catch (IOException e) {
                    LOG.error("Error when summarizing project. Project summaries may be incomplete!", e);
                    return false;
                } finally {
                    project.close();
                }
            }
        };
    }
}
