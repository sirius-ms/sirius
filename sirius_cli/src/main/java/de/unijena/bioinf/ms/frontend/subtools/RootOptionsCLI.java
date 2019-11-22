package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.frontend.io.InputFiles;
import de.unijena.bioinf.ms.frontend.io.InstanceImporter;
import de.unijena.bioinf.ms.frontend.io.MS2ExpInputIterator;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceFactory;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is for not algorithm related parameters.
 * <p>
 * That means parameters that do not influence computation and do not
 * need to be Annotated to the MS2Experiment, e.g. standard commandline
 * stuff, technical parameters (cores) or input/output.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "night-sky", aliases = {"ns"/*, "sirius"*/}, defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false)
public class RootOptionsCLI implements RootOptions {
    public static final Logger LOG = LoggerFactory.getLogger(RootOptionsCLI.class);

    public enum InputType {PROJECT, SIRIUS}

    protected final DefaultParameterConfigLoader defaultConfigOptions;
    protected final InstanceFactory instacneFactory;

    public RootOptionsCLI(@NotNull DefaultParameterConfigLoader defaultConfigOptions, @NotNull InstanceFactory instanceFactory) {
        this.defaultConfigOptions = defaultConfigOptions;
        this.instacneFactory = instanceFactory;
    }


    // region Options: Quality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //todo think how to implement this into the cli???
    // I think a subtool that can be called multiple times could be cool???
    @Option(names = "--noise", description = "Median intensity of noise peaks", order = 10, hidden = true)
    public Double medianNoise;

    @Option(names = {"--assess-data-quality"}, description = "produce stats on quality of spectra and estimate isolation window. Needs to read all data at once.", order = 20, hidden = true)
    public boolean assessDataQuality;
    //endregion

    // region Options: Basic
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = "-q", description = "suppress shell output", order = 30)
    public boolean quiet;

    /*@Option(names = "--cite", description = "show citations", order = 40,)
    public boolean cite;*/

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // region Options: Technical
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--processors", "--cores"}, description = "Number of cpu cores to use. If not specified Sirius uses all available cores.", order = 50)
    public void setNumOfCores(int numOfCores) {
        PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(numOfCores));
    }


//    @Option(names = "--max-compound-buffer", description = "Maxmimal number of compounds that will be buffered in Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. For Infinite buffer size set it to 0. Default: 2 * --initial_intance_buffer", order = 60, hidden = true)
    @Option(names = "--max-compound-buffer", description = "Deprecated: This Option is deprecated and has no effect anymore.", order = 60, hidden = true)
    private Integer maxInstanceBuffer;


    //    @Option(names = "--initial-compound-buffer", description = "Number of compounds that will be loaded initially into the Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all compounds immediately set it to 0. Default: 2 * --cores", order = 60)
    @Option(names = {"--compound-buffer", "--initial-compound-buffer"}, description = "Number of compounds that will be loaded into the Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all compounds immediately set it to 0. Default: 2 * --cores", order = 60)
    public void setInitialInstanceBuffer(Integer initialInstanceBuffer) {
        if (initialInstanceBuffer == null)
            initialInstanceBuffer = SiriusJobs.getGlobalJobManager().getCPUThreads();

        PropertyManager.setProperty("de.unijena.bioinf.sirius.instanceBuffer", String.valueOf(initialInstanceBuffer));
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // region Options: INPUT/OUTPUT
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--workspace", "-w"}, description = "Specify sirius workspace location. This is the directory for storing Property files, logs, databases and caches.  This is NOT for the project-space that stores the results! Default is $USER_HOME/.sirius", order = 70)
    public Files workspace; //todo change in application core

    @Option(names = "--maxmz", description = "Just consider compounds with a precursor mz lower or equal this maximum mz. All other compounds in the input file are ignored.", order = 100)
    public Double maxMz = Double.POSITIVE_INFINITY;


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


    @Option(names = {"--output", "--project-space", "-o", "-p"}, description = "Specify project-space to read from and also write to if nothing else is specified. For compression use the File ending .zip or .sirius", order = 70)
    private Path projectSpaceLocation;


    @Option(names = {"--input", "-i"}, description = "Input for the analysis. Ths can be either preprocessed mass spectra in .ms or .mgf file format, " +
            "LC/MS runs in .mzML/.mzXml format or already existing SIRIUS project-space(s) (uncompressed/compressed).", order = 80)
    // we differentiate between contiunuing a project-space and starting from mzml or  already processed ms/mgf file.
    // If multiple files match the priobtrrity is project-space,  ms/mgf,  mzml
    public void setInputFiles(List<File> files) {
        this.inputFiles = InstanceImporter.expandInputFromFile(files);
    }

    private InputFiles inputFiles = null;

    @Override
    public InputFiles getInput() {
        return inputFiles;
    }

    @Option(names = {"--ignore-formula"}, description = "ignore given molecular formula in .ms or .mgf file format, ")
    private boolean ignoreFormula = false;
    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private ProjectSpaceManager projectSpaceToWriteOn = null;

    @Override
    public ProjectSpaceManager getProjectSpace() {
        if (projectSpaceToWriteOn == null)
            configureProjectSpace();

        return projectSpaceToWriteOn;
    }


    @Override
    public PreprocessingJob makePreprocessingJob(InputFiles input, ProjectSpaceManager space) {
        return new PreprocessingJob(input, space) {
            @Override
            protected Iterable<Instance> compute() {
                //todo handle compressed stuff
                //todo check if output space needs to be added to input

                if (space != null) {
                    new InstanceImporter(space).importMultipleSources(inputFiles);
                    if (space.size() > 0)
                        LOG.info("No Input given but output Project-Space is not empty and will be used as Input instead!");
                    else
                        LOG.info("No Input given and output Project-Space is also empty. Starting application without input data.");

                    return space;
                }
                throw new CommandLine.PicocliException("NO projectspace to write on: ");
            }
        };
    }


    protected void configureProjectSpace() {
        try {
            if (projectSpaceLocation == null) {
                if (inputFiles != null && inputFiles.projects.size() == 1) {
                    projectSpaceLocation = (inputFiles.projects.get(0));
                    LOG.info("No output location given. Writing output to input location: " + projectSpaceLocation.toString());
                } else {
                    projectSpaceLocation = ProjectSpaceIO.createTmpProjectSpaceLocation();
                    LOG.warn("No unique output location found. Writing output to Temporary folder: " + projectSpaceLocation.toString());
                }
            }

            final SiriusProjectSpace psTmp;
            if (Files.notExists(projectSpaceLocation)) {
                psTmp = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createNewProjectSpace(projectSpaceLocation);
            }else {
                psTmp = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(projectSpaceLocation);
            }

            //check for formatter
            if (projectSpaceFilenameFormatter == null) {
                try {
                    projectSpaceFilenameFormatter = psTmp.getProjectSpaceProperty(FilenameFormatter.PSProperty.class).map(it -> new StandardMSFilenameFormatter(it.formatExpression)).orElse(new StandardMSFilenameFormatter());
                } catch (Exception e) {
                    LOG.warn("Could not Parse filenameformatter -> Using default");
                    projectSpaceFilenameFormatter = new StandardMSFilenameFormatter();
                }
                //todo when do we write this?
                psTmp.setProjectSpaceProperty(FilenameFormatter.PSProperty.class, new FilenameFormatter.PSProperty(projectSpaceFilenameFormatter));
            }

            projectSpaceToWriteOn = new ProjectSpaceManager(psTmp, instacneFactory, projectSpaceFilenameFormatter, id -> {
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
}
