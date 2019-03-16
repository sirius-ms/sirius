package de.unijena.bioinf.ms.cli.parameters.zodiac;

import de.unijena.bioinf.GibbsSampling.model.scorer.EdgeScorings;
import de.unijena.bioinf.ms.cli.parameters.config.DefaultParameterOptionLoader;
import de.unijena.bioinf.ms.cli.parameters.Provide;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * This is for Zodiac specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "zodiac", aliases = {"Z"}, description = "Identify Molecular formulas of all compounds in a dataset together using ZODIAC.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true)
public class ZodiacOptions {
    protected final DefaultParameterOptionLoader defaultConfigOptions;

    public ZodiacOptions(DefaultParameterOptionLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }
    @Option(
            names = {"--lowest-cosine"},
            description = "Below this cosine threshold a spectral library hit does not give any score bonus."
    )
    public double lowestCosine;


    @Option(
            names = {"--lambda"},
            description = "Lambda used in the scoring function of spectral library hits. The higher the more important are library hits. 1 is default."
    )
    public double libraryScoreLambda;


    @Option(
            names = {"--spectral-hits"},
            description = "csv with spectral library hits"
    )
    public String libraryHitsFile;

    @Option(
            names = {"--iterations", "-i"},
            description = "number of iterations"
    )
    public int iterationSteps;

    @Option(
            names = {"-b", "--burn-in"},
            description = "number of steps to use to burn in gibbs sampler."
    )
    public int burnInSteps;

    @Option(
            names = {"--separateRuns"},
            description = "number of separate runs"
    )
    public int separateRuns;


    @Option(
            names = {"--minLocalCandidates"},
            description = "minimum number of candidates per compound which must have at least --minLocalConnections connections to other compounds"
    )
    public int localFilter;

    @Option(
            names = {"--thresholdFilter", "--thresholdfilter"},
            description = "Defines the proportion of edges of the complete network which will be ignored. Default is 0.95 = 95%"
    )
    public double thresholdFilter;

    @Option(
            names = {"--minLocalConnections"},
            description = ""
    )
    public int minLocalConnections;

    @Option(
            names = {"--distribution"},
            description = "which probability distribution to assume: lognormal, exponential"
    )
    public EdgeScorings probabilityDistribution;

    @Option(
            names = {"--estimate-param"},
            description = "parameters of distribution are estimated from the data. By default standard parameters are assumed."
    )
    public boolean estimateDistribution;

    @Option(
            names = {"--cluster"},
            description = "cluster compounds with the same best molecular formula candidate before running ZODIAC."
    )
    public boolean clusterCompounds;

    @Option(
            names = {"--compute-statistics-only"},
            description = "only compute the dataset statistics without running ZODIAC"
    )
    public boolean onlyComputeStats;


    @Option(
            names = {"--ignore-spectra-quality"},
            description = "As default ZODIAC runs a 2-step approach. First running 'good quality compounds' only, and afterwards including the remaining."
    )
    public boolean onlyOneStepZodiac;

    @Option(
            names = {"--ms2-median-noise"},
            description = "Set MS2 median noise intensity - else it is estimated. This is used to count the number of MS2 peaks to gauge spectrum quality."
    )
    public Double medianNoiseIntensity;

    /*@Override
    public void setParamatersToExperiment(MutableMs2Experiment experiment) {
        //todo fill me
    }*/
}
