package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.frontend.io.InstanceImporter;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.FilenameFormatter;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.StandardMSFilenameFormatter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

/**
 * This is for not algorithm related parameters.
 * <p>
 * That means parameters that do not influence computation and do not
 * need to be Annotated to the MS2Experiment, e.g. standard commandline
 * stuff, technical parameters (cores) or input/output.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "sirius", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false)
public class CLIRootOptions<M extends ProjectSpaceManager> implements RootOptions<PreprocessingJob<ProjectSpaceManager>> {
    public static final Logger LOG = LoggerFactory.getLogger(CLIRootOptions.class);

    protected final DefaultParameterConfigLoader defaultConfigOptions;
    protected final ProjectSpaceManagerFactory<M> spaceManagerFactory;

    public CLIRootOptions(@NotNull DefaultParameterConfigLoader defaultConfigOptions, @NotNull ProjectSpaceManagerFactory<M> spaceManagerFactory) {
        this.defaultConfigOptions = defaultConfigOptions;
        this.spaceManagerFactory = spaceManagerFactory;
    }


    // region Options: Quality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = "--noise", description = "Median intensity of noise peaks", order = 10, hidden = true)
    public Double medianNoise;

    @Option(names = {"--assess-data-quality"}, description = "produce stats on quality of spectra and estimate isolation window. Needs to read all data at once.", order = 20, hidden = true)
    public boolean assessDataQuality;
    //endregion

    // region Options: Technical
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--processors", "--cores"}, description = "Number of cpu cores to use. If not specified Sirius uses all available cores.", order = 50)
    public void setNumOfCores(int numOfCores) {
        PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(numOfCores));
    }

    @Option(names = {"--compound-buffer", "--initial-compound-buffer"}, description = "Number of compounds that will be loaded into the Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all compounds immediately set it to 0. Default: 2 * --cores. Note that for DATASET_TOOLS the compound buffer may have no effect because this tools may need all compounds in memory for computation.", order = 60)
    public void setInitialInstanceBuffer(Integer initialInstanceBuffer) {
        if (initialInstanceBuffer == null)
            initialInstanceBuffer = SiriusJobs.getGlobalJobManager().getCPUThreads();

        PropertyManager.setProperty("de.unijena.bioinf.sirius.instanceBuffer", String.valueOf(initialInstanceBuffer));
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // region Options: INPUT/OUTPUT
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--workspace", "-w"}, description = "Specify sirius workspace location. This is the directory for storing Property files, logs, databases and caches.  This is NOT for the project-space that stores the results! Default is $USER_HOME/.sirius", order = 70, hidden = true)
    public Files workspace; //todo change in application core

    @Option(names = {"--output", "-o"}, description = "Specify output project-space to write to. If no [--input] is specified it is also used as input to read from. For compression use the File ending .zip or .sirius.", order = 70)
    private Path outputProjectLocation;

    @Option(names = "--naming-convention", description = "Specify a format for compounds' output directories. Default %%index_%%filename_%%compoundname", order = 90)
    public void setProjectSpaceFilenameFormatter(String projectSpaceFilenameFormatter) throws ParseException {
        this.projectSpaceFilenameFormatter = new StandardMSFilenameFormatter(projectSpaceFilenameFormatter);
    }

    public FilenameFormatter projectSpaceFilenameFormatter = null;

    @Option(names = "--recompute", description = "Recompute ALL results of ALL SubTools that are already present. By defaults already present results of an instance will be preserved and the instance will be skipped for the corresponding Task/Tool", order = 95, defaultValue = "FALSE")
    public void setRecompute(boolean recompute) throws Exception {
        try {
            defaultConfigOptions.changeOption("RecomputeResults", String.valueOf((recompute)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Option(names = "--maxmz", description = "Just consider compounds with a precursor mz lower or equal this maximum mz. All other compounds in the input file are ignored.", defaultValue = "Infinity", order = 100)
    public double maxMz;


    @CommandLine.ArgGroup(exclusive = false, /*heading = "Specify INPUTS:",*//* multiplicity = "1",*/order = 110)
    private InputFilesOptions inputFiles;

    @Override
    public InputFilesOptions getInput() {
        return inputFiles;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private M projectSpaceToWriteOn = null;
    @Override
    public M getProjectSpace() {
        if (projectSpaceToWriteOn == null)
            projectSpaceToWriteOn = configureProjectSpace();

        return projectSpaceToWriteOn;
    }

    protected M configureProjectSpace() {
        try {
            if (outputProjectLocation == null) {
                if (inputFiles != null && inputFiles.msInput.projects.size() == 1) {
                    outputProjectLocation = (inputFiles.msInput.projects.get(0));
                    LOG.info("No output location given. Writing output to input location: " + outputProjectLocation.toString());
                } else {
                    outputProjectLocation = ProjectSpaceIO.createTmpProjectSpaceLocation();
                    LOG.warn("No unique output location found. Writing output to Temporary folder: " + outputProjectLocation.toString());
                }
            }

            final SiriusProjectSpace psTmp;
            if (Files.notExists(outputProjectLocation)) {
                psTmp = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createNewProjectSpace(outputProjectLocation);
            } else {
                psTmp = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(outputProjectLocation);
            }

            //check for formatter
            if (projectSpaceFilenameFormatter == null) {
                try {
                    projectSpaceFilenameFormatter = psTmp.getProjectSpaceProperty(FilenameFormatter.PSProperty.class).map(it -> new StandardMSFilenameFormatter(it.formatExpression)).orElse(new StandardMSFilenameFormatter());
                } catch (Exception e) {
                    LOG.warn("Could not Parse 'FilenameFormatter' -> Using default");
                    projectSpaceFilenameFormatter = new StandardMSFilenameFormatter();
                }
                //todo when do we write this?
                psTmp.setProjectSpaceProperty(FilenameFormatter.PSProperty.class, new FilenameFormatter.PSProperty(projectSpaceFilenameFormatter));
            }

            return spaceManagerFactory.create(psTmp, projectSpaceFilenameFormatter, id -> {
                if (id.getIonMass().orElse(Double.NaN) <= maxMz)
                    return true;
                else {
                    LOG.info("Skipping instance " + id.toString() + " with mass: " + id.getIonMass().orElse(Double.NaN) + " > " + maxMz);
                    return false;
                }
            });
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Could not initialize workspace!", e);
        }
    }

    @Override
    public PreprocessingJob<ProjectSpaceManager> makeDefaultPreprocessingJob() {
        return new PreprocessingJob<ProjectSpaceManager>() {
            @Override
            protected ProjectSpaceManager compute() throws Exception {
                M space = getProjectSpace();
                InputFilesOptions input = getInput();
                if (space != null) {
                    if (input != null)
                        SiriusJobs.getGlobalJobManager().submitJob(new InstanceImporter(space, maxMz).makeImportJJob(input)).awaitResult();
                    if (space.size() < 1)
                        logInfo("No Input has been imported to Project-Space. Starting application without input data.");
                    return space;
                }
                throw new CommandLine.PicocliException("No Project-Space for writing output!");
            }
        };
    }
}
