package de.unijena.bioinf.ms.cli.parameters;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
/**
 * This is for not algorithm related parameters.
 *
 * That means parameters that do not influence computation and do not
 * need to be Annotated to the MS2Experiment, e.g. standard commandline
 * stuff, technical parameters (cores) or input/output.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 * */
@CommandLine.Command(name = "night-sky", aliases = {"ns"/*, "sirius"*/}, defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class BasicOptions {
    // region Options: Basic
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = "-q", description = "suppress shell output")
    public boolean quiet;

    @Option(names = "--cite")
    public boolean cite;
    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // region Options: Technical
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--processors", "--cores"}, description = "Number of cpu cores to use. If not specified Sirius uses all available cores.")
    public int numOfCores;

    @Option(names = "--max-compound-buffer", description = "Maxmimal number of compounds that will be buffered in Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. For Infinite buffer size set it to 0. Default: 2 * --initial_intance_buffer")
    public Integer maxInstanceBuffer;

    @Option(names = "--initial-compound-buffer", description = "Number of compounds that will be loaded initially into the Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all compounds immediately set it to 0. Default: 2 * --cores")
    public Integer minInstanceBuffer;
    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // region Options: INPUT/OUTPUT
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--workspace", "-w"}, description = "Specify sirius workspace location. This is the directory for storing Property files, logs and caches.  This is NOT for the project-space that stores the results! Default is $USER_HOME/.sirius")
    public String workspace;

    @Option(names = {"--project-space", "-p"}, description = "Specify project-space to read from and also write to if nothing else is specified. For compression use the File ending .zip or .sirius")
    public String projectSpace;

    @Option(names = {"--output-project-space", "-o"}, description = "Specify a different project-space for writing than for reading. For compression use the File ending .zip or .sirius")
    public String output_project_space;

    @Option(names = "--naming-convention", description = "Specify a format for compounds' output directories. Default %%index_%%filename_%%compoundname")
    public String projectSpaceNamingConvention;

    @Option(names = "--maxmz", description = "Just consider compounds with a precursor mz lower or equal this maximum mz. All other compounds in the input file are ignored.")
    public Double maxMz;

    @Option(names = {"-1", "--ms1"}, description = "MS1 spectrum file name") //min 0
    public List<File> ms1;

    @Option(names = {"-2", "--ms2"}, description = "MS2 spectra file names")//min 0
    public List<File> ms2;

    @Parameters(hidden = true)
    public List<String> input = new ArrayList<>();
    // endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
