package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.MS2ExpInputIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.ProjectSpaceManager;
import de.unijena.bioinf.babelms.projectspace.PassatuttoSerializer;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.fingerid.*;
import de.unijena.bioinf.projectspace.sirius.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.*;

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

    public RootOptionsCLI(@NotNull DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
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


    @Option(names = "--max-compound-buffer", description = "Maxmimal number of compounds that will be buffered in Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. For Infinite buffer size set it to 0. Default: 2 * --initial_intance_buffer", order = 60)
    private Integer maxInstanceBuffer;

    @Override
    public Integer getMaxInstanceBuffer() {
        initBuffers();
        return maxInstanceBuffer;
    }

    @Option(names = "--initial-compound-buffer", description = "Number of compounds that will be loaded initially into the Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all compounds immediately set it to 0. Default: 2 * --cores", order = 60)
    private Integer initialInstanceBuffer;

    @Override
    public Integer getInitialInstanceBuffer() {
        initBuffers();
        return initialInstanceBuffer;
    }

    private void initBuffers() {
        if (initialInstanceBuffer == null)
            initialInstanceBuffer = SiriusJobs.getGlobalJobManager().getCPUThreads();

        if (maxInstanceBuffer == null) {
            maxInstanceBuffer = initialInstanceBuffer * 2;
        } else {
            if (initialInstanceBuffer <= 0) {
                maxInstanceBuffer = initialInstanceBuffer; //this means infinity
            } else {
                maxInstanceBuffer = Math.max(initialInstanceBuffer, maxInstanceBuffer);
            }
        }
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
    public File projectSpaceLocation;

    @Option(names = {"--input", "-i"}, description = "Input for the analysis. Ths can be either preprocessed mass spectra in .ms or .mgf file format, " +
            "LC/MS runs in .mzML/.mzXml format or already existing SIRIUS project-space(s) (uncompressed/compressed).", order = 80)
    // we differentiate between contiunuing a project-space and starting from mzml or  already processed ms/mgf file.
    // If multiple files match the priobtrrity is project-space,  ms/mgf,  mzml
    public void setInput(List<File> files) {
        if (files == null || files.isEmpty()) return;

        final List<File> projectSpaces = new ArrayList<>();
        final List<File> siriusInfiles = new ArrayList<>();

        expandInput(files, siriusInfiles, projectSpaces);

        if (!projectSpaces.isEmpty()) {
            if (!siriusInfiles.isEmpty())
                LOG.warn("Multiple input types found: Only the project-space data ist used as input.");
            input = projectSpaces;
            inputType = InputType.PROJECT;
        } else if (!siriusInfiles.isEmpty()) {
            input = siriusInfiles;
            inputType = InputType.SIRIUS;
        } else {
            throw new CommandLine.PicocliException("No valid input data is found. Please give you input in a supported format.");
        }
    }


    private void expandInput(@NotNull List<File> files, @NotNull List<File> siriusInfiles, @NotNull List<File> projectSpaces) {
        for (File g : files) {
            if (g.isDirectory()) {
                // check whether it is a workspace or a gerneric directory with some other input
                if (ProjectSpaceIO.isExistingProjectspaceDirectory(g)) {
                    projectSpaces.add(g);
                } else {
                    File[] ins = g.listFiles(pathname -> pathname.isFile());
                    if (ins != null) {
                        Arrays.sort(ins, Comparator.comparing(File::getName));
                        expandInput(Arrays.asList(ins), siriusInfiles, projectSpaces);
                    }
                }
            } else {
                //check whether files are lcms runs copressed project-spaces or stadard ms/mgf files
                final String name = g.getName();
                if (MsExperimentParser.isSupportedFileName(name)) {
                    siriusInfiles.add(g);
                } else if (ProjectSpaceIO.isCompressedProjectSpace(g)) {
                    //compressed spaces are read only and can be handled as simple input
                    projectSpaces.add(g);
                } else {
                    LOG.warn("File with the name \"" + name + "\" is not in a supported format or has a wrong file extension. File is skipped");
                }
            }
        }
    }

    List<File> input = null;

    @Override
    public List<File> getInput() {
        return input;
    }

    InputType inputType = null;


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
    public PreprocessingJob makePreprocessingJob(List<File> input, ProjectSpaceManager space) {
        return new PreprocessingJob(getInput(), getProjectSpace()) {
            @Override
            protected Iterable<Instance> compute() {
                //todo handle compressed stuff
                //todo check if output space needs to be added to input
                if (inputType != null && input != null) {
                    switch (inputType) {
                        case PROJECT:
                            return space;
                        case SIRIUS:
                            //we decided to do maxMZ filtering after writing data to the project space
                            final Iterator<Instance> msit = new MS2ExpInputIterator(input, Integer.MAX_VALUE, ignoreFormula).asInstanceIterator(space);
                            while (msit.hasNext())
                                msit.next(); //writes new instances to projectspace
                            return space;
                    }
                } else if (space != null && space.projectSpace().size() > 0) {
                    LOG.info("No Input given but output Project-Space is not empty and will be used as Input instead!");
                    return space;
                }
                throw new CommandLine.PicocliException("Illegal Input type: " + inputType);
            }
        };
    }


    protected void configureProjectSpace() {
        try {
            if (projectSpaceLocation == null) {
                if (inputType == InputType.PROJECT && input.size() == 1 && !ProjectSpaceIO.isCompressedProjectSpace(input.get(0))) {
                    projectSpaceLocation = input.get(0);
                } else
                    throw new CommandLine.PicocliException("No output location given. Can only be avoided if a single (non compressed) project-space is the input");
            }

            if (!projectSpaceLocation.exists()) {
                if (!projectSpaceLocation.mkdir())
                    throw new IOException("Could not create new directory for project-space'" + projectSpaceLocation + "'");
            }

            final SiriusProjectSpace psTmp = new ProjectSpaceIO(makeProjectSpaceConfig()).openExistingProjectSpace(projectSpaceLocation);

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

            projectSpaceToWriteOn = new ProjectSpaceManager(psTmp, projectSpaceFilenameFormatter, id -> {
                if (id.getIonMass() <= maxMz)
                    return true;
                else {
                    LOG.info("Skipping instance " + id.toString() + " with mass: " + id.getIonMass() + " > " + maxMz);
                    return false;
                }
            });
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Could not initialize workspace!", e);
        }
    }


    protected ProjectSpaceConfiguration makeProjectSpaceConfig() {
        final ProjectSpaceConfiguration config = new ProjectSpaceConfiguration();
        //configure ProjectspaceProperties
        config.defineProjectSpaceProperty(FilenameFormatter.PSProperty.class, new FilenameFormatter.PSPropertySerializer());
        //configure compound container
        config.registerContainer(CompoundContainer.class, new CompoundContainerSerializer());
        config.registerComponent(CompoundContainer.class, ProjectSpaceConfig.class, new ProjectSpaceConfigSerializer());
        config.registerComponent(CompoundContainer.class, Ms2Experiment.class, new MsExperimentSerializer());
        //configure formula result
        config.registerContainer(FormulaResult.class, new FormulaResultSerializer());
        config.registerComponent(FormulaResult.class, FTree.class, new TreeSerializer());
        config.registerComponent(FormulaResult.class, FormulaScoring.class, new FormulaScoringSerializer());
        //pssatuto components
        config.registerComponent(FormulaResult.class, Decoy.class, new PassatuttoSerializer());
        //fingerid components
        config.defineProjectSpaceProperty(CSIClientData.class, new CsiClientSerializer());
        config.registerComponent(FormulaResult.class, FingerprintResult.class, new FingerprintSerializer());
        config.registerComponent(FormulaResult.class, FingerblastResult.class, new FingerblastResultSerializer());
        //canopus
        config.defineProjectSpaceProperty(CanopusClientData.class, new CanopusClientSerializer());
        config.registerComponent(FormulaResult.class, CanopusResult.class, new CanopusSerializer());
        return config;
    }

    /*protected MetaDataSerializer[] makeSerializerArray() {

        //TODO this should be collected from the different subtool clis
        // we should be able to import and export the data even if
        // the calculators are not available -> e.g. web connection!
        List<MetaDataSerializer> al = new ArrayList<>();
        al.add(new IdentificationResultSerializer());
        al.add(new ZodiacResultSerializer());
        al.add(new FingerIdResultSerializer(ApplicationCore.WEB_API));
        if (ApplicationCore.CANOPUS != null)
            al.add(new CanopusResultSerializer(ApplicationCore.CANOPUS));
        al.add(new PassatuttoSerializer());

        return al.toArray(new MetaDataSerializer[0]);
    }*/
}
