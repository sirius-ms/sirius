package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.GibbsSampling.model.scorer.EdgeScorings;
import picocli.CommandLine.Option;

/**
 * Created by ge28quv on 22/05/17.
 */
public class ZodiacOptions {

    /////////////////////////////////////////////////

    // run Zodiac
    @Option(names = {"-Z", "--zodiac"}, description = "run zodiac on a given sirius workspace.", hidden = true)
    boolean zodiac = false;

    /////////////////////////////////////////////////
    @Option(
            names = {"--lowest-cosine"},
            description = "Below this cosine threshold a spectral library hit does not give any score bonus.",
            defaultValue = "0.3"
    )
    double lowestCosine;

    @Option(
            names = {"--lambda"},
            description = "Lambda used in the scoring function of spectral library hits. The higher the more important are library hits. 1 is default.",
            defaultValue = "1.0"
    )
    double libraryScoreLambda;

    //todo @Marcus for the future this should ne replaced with the sirius input property if possible
    @Option(
            names = {"--spectra"},
            description = "The file of spectra (.mgf) which was used to compute the trees"
    )
    String spectraFile = null;


    @Option(
            names = {"--spectral-hits"},
            description = "csv with spectral library hits"
    )
    String libraryHitsFile = null;

    @Option(
            names = {"--iterations", "-i"},
            description = "number of iterations",
            defaultValue = "100000"
    )
    int iterationSteps;

    @Option(
            names = {"-b", "--burn-in"},
            description = "number of steps to use to burn in gibbs sampler.",
            defaultValue = "10000"
    )
    int burnInSteps;

    @Option(
            names = {"--separateRuns"},
            description = "number of separate runs",
            defaultValue = "10"
    )
    int separateRuns;


    @Option(
            names = {"--minLocalCandidates"},
            description = "minimum number of candidates per compound which must have at least --minLocalConnections connections to other compounds",
            defaultValue = "-1"
    )
    int localFilter;

    @Option(
            names = {"--thresholdFilter", "--thresholdfilter"},
            description = "Defines the proportion of edges of the complete network which will be ignored. Default is 0.95 = 95%",
            defaultValue = "0.95"
    )
    double thresholdFilter;

    @Option(
            names = {"--minLocalConnections"},
            description = "",
            defaultValue = "-1"
    )
    int minLocalConnections;

    @Option(
            names = {"--distribution"},
            description = "which probability distribution to assume: lognormal, exponential",
            defaultValue = "lognormal"
    )
    EdgeScorings probabilityDistribution;

    @Option(names = {"--estimate-param"}, description = "parameters of distribution are estimated from the data. By default standard parameters are assumed.")
    boolean estimateDistribution = false;

    @Option(
            names = {"--cluster"},
            description = "cluster compounds with the same best molecular formula candidate before running ZODIAC."
    )
    boolean clusterCompounds = false;


    @Option(
            names = {"--isolation-window-width"},
            description = "width of the isolation window to measure MS2"
    )
    Double isolationWindowWidth = null;

    @Option(
            names = {"--isolation-window-shift"},
            description = "The shift applied to the isolation window to measure MS2 in relation to the precursormass",
            defaultValue = "0"
    )
    double isolationWindowShift;

    @Option(
            names = {"--compute-statistics-only"},
            description = "only compute the dataset statistics without running ZODIAC"
    )
    boolean onlyComputeStats = false;


    @Option(
            names = {"--ignore-spectra-quality"},
            description = "As default ZODIAC runs a 2-step approach. First running 'good quality compounds' only, and afterwards including the remaining."
    )
    boolean onlyOneStepZodiac = false;














    //todo redundant???
    @Option(
            names = {"--candidates"},
            description = "maximum number of candidate molecular formulas per compound to use in Zodiac. default: all"
    )
    Integer numberOfCandidates = null;

    @Option(
            names = {"--processors", "--cores"},
            description = "Number of cpu cores to use. If not specified Sirius uses all available cores.",
            defaultValue = "-1"
    )
    int numOfCores;

    @Option(
            names = {"--sirius", "-s"},
            description = "Sirius output directory or workspace. This is the input for Zodiac"
    )
    String sirius;

    //todo duplicate
    //naming
    @Option(names = "--naming-convention", description = "Specify a format for compounds' output directorys. Default %index_%filename_%compoundname")
    String namingConvention;


    //todo duplicate
    @Option(
            names = {"output", "-o"},
            description = "output directory"
    )
    String output = null;

}
