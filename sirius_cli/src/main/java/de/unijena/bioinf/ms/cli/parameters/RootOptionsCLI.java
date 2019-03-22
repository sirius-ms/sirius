package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ms.cli.InputIterator;
import de.unijena.bioinf.ms.io.projectspace.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * This is for not algorithm related parameters.
 *
 * That means parameters that do not influence computation and do not
 * need to be Annotated to the MS2Experiment, e.g. standard commandline
 * stuff, technical parameters (cores) or input/output.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 * */
@CommandLine.Command(name = "night-sky", aliases = {"ns"/*, "sirius"*/}, defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false)
public class RootOptionsCLI implements RootOptions {

    // region Options: Quality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //todo think how to implement this into the cli???
    // I think a subtool that can be called multiple times could be cool???
    @Option(names = "--noise", description = "Median intensity of noise peaks", order = 10,  hidden = true)
    public Double medianNoise;

    @Option(names = {"--assess-data-quality"}, description = "produce stats on quality of spectra and estimate isolation window. Needs to read all data at once.", order = 20, hidden = true)
    public boolean assessDataQuality;
    //endregion

    // region Options: Basic
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = "-q", description = "suppress shell output", order = 30)
    public boolean quiet;

    @Option(names = "--cite", description = "show citations", order = 40)
    public boolean cite;
    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // region Options: Technical
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--processors", "--cores"}, description = "Number of cpu cores to use. If not specified Sirius uses all available cores.", order =50)
    public void setNumOfCores(int numOfCores) {
        PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(numOfCores));
    }


    @Option(names = "--max-compound-buffer", description = "Maxmimal number of compounds that will be buffered in Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. For Infinite buffer size set it to 0. Default: 2 * --initial_intance_buffer", order = 60)
    public Integer maxInstanceBuffer;

    @Option(names = "--initial-compound-buffer", description = "Number of compounds that will be loaded initially into the Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all compounds immediately set it to 0. Default: 2 * --cores", order = 60)
    public Integer initialInstanceBuffer;

    private void initBuffers(){
        if (initialInstanceBuffer == null)
            initialInstanceBuffer = PropertyManager.getNumberOfCores() * 5;
        if (maxInstanceBuffer == null)
            maxInstanceBuffer = initialInstanceBuffer * 2;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // region Options: INPUT/OUTPUT
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--workspace", "-w"}, description = "Specify sirius workspace location. This is the directory for storing Property files, logs and caches.  This is NOT for the project-space that stores the results! Default is $USER_HOME/.sirius", order = 70)
    public String workspace;

    @Option(names = {"--project-space", "-p"}, description = "Specify project-space to read from and also write to if nothing else is specified. For compression use the File ending .zip or .sirius", required = true, order = 70)
    public File projectSpaceLocation;

    @Option(names = {"--input-project-space"}, description = "Specify different project-space(s) for reading than for writing. For compression use the File ending .zip or .sirius", order = 80)
    public List<File> inputProjectSpaceLocations;

    @Option(names = "--naming-convention", description = "Specify a format for compounds' output directories. Default %%index_%%filename_%%compoundname", order = 90)
    public void setProjectSpaceFilenameFormatter(String projectSpaceFilenameFormatter) throws ParseException {
        this.projectSpaceFilenameFormatter = new StandardMSFilenameFormatter(projectSpaceFilenameFormatter);
    }

    public FilenameFormatter projectSpaceFilenameFormatter = new StandardMSFilenameFormatter();

    @Option(names = "--maxmz", description = "Just consider compounds with a precursor mz lower or equal this maximum mz. All other compounds in the input file are ignored.", order = 100)
    public Double maxMz = Double.POSITIVE_INFINITY;


    @Parameters(description = "Input spectra in .ms or .mgf file format", type = File.class)
    public List<File> input = null;



    // endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // region Options: SINGLE_COMPOUND_MODE
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //todo hidden?
    @Option(names = {"-1", "--ms1"}, description = "MS1 spectrum file name", order = 110)
    public List<File> ms1;

    @Option(names = {"-2", "--ms2"}, description = "MS2 spectra file names", order = 120)
    public List<File> ms2;

    @Option(names = {"-z", "--parentmass", "precursor", "mz"}, description = "the mass of the parent ion", order = 130)
    public Double parentMz;
    //endregion
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public RootOptions.IO call() throws Exception {
        //init buffer sizes
        initBuffers();

        //make a project space
        final SiriusProjectSpace projectSpace = configureProjectSpace();
        //read new input if available


        //todo how to handle merge of new input and already existing workspace???
        final List<File> input = expandInput(this.input);
        final Iterator<ExperimentResult> inputIterator = (input == null || input.isEmpty())
                ? projectSpace.parseExperimentIterator()
                : new InputIterator(input, maxMz).asExpResultIterator();


        return new IO(projectSpace, inputIterator);
    }

    protected SiriusProjectSpace configureProjectSpace() throws IOException {
        SiriusProjectSpace space;
        if (inputProjectSpaceLocations == null || inputProjectSpaceLocations.isEmpty()) {
            space = SiriusProjectSpace.create(projectSpaceFilenameFormatter, projectSpaceLocation,
                    (currentProgress, maxProgress, Message) -> {
                        System.out.println("Creating Project Space: " + (((((double) currentProgress) / (double) maxProgress)) * 100d) + "%");
                    }
                    , makeSerializerArray());
        } else {
            space = SiriusProjectSpace.create(projectSpaceLocation, inputProjectSpaceLocations, projectSpaceFilenameFormatter,
                    (currentProgress, maxProgress, Message) -> {
                        System.out.println("Creating Project Space: " + (((((double) currentProgress) / (double) maxProgress)) * 100d) + "%");
                    }
                    , makeSerializerArray());
        }
        space.registerSummaryWriter(new MztabSummaryWriter());

        return space;
    }

    protected List<File> expandInput(List<File> files) {
        if (files == null) return null;

        final List<File> infiles = new ArrayList<>();
        for (File g : files) {
            if (g.isDirectory()) {
                File[] ins = g.listFiles(pathname -> pathname.isFile());
                if (ins != null) {
                    Arrays.sort(ins, Comparator.comparing(File::getName));
                    infiles.addAll(Arrays.asList(ins));
                }
            } else {
                infiles.add(g);
            }
        }
        return infiles;
    }

    protected MetaDataSerializer[] makeSerializerArray() {
        //todo check weather Canopus and WebService is available
        // this should be collected from the different subtool
        return new MetaDataSerializer[]{
                new IdentificationResultSerializer()
                , new FingerIdResultSerializer(ApplicationCore.WEB_API)
                , new CanopusResultSerializer(ApplicationCore.CANOPUS)
        };
    }


}
