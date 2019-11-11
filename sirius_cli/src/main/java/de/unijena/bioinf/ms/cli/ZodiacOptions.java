package de.unijena.bioinf.ms.cli;

import com.lexicalscope.jewel.cli.Option;
import de.unijena.bioinf.GibbsSampling.model.scorer.EdgeScorings;

/**
 * Created by ge28quv on 22/05/17.
 */
public interface ZodiacOptions {

    @Option(
            longName = {"lowest-cosine"},
            description = "Below this cosine threshold a spectral library hit does not give any score bonus.",
            defaultValue = "0.3"
    )
    double getLowestCosine();

    @Option(
            longName = {"lambda"},
            description = "Lambda used in the scoring function of spectral library hits. The higher the more important are library hits. 1 is default.",
            defaultValue = {"1"}
    )
    double getLibraryScoreLambda();


    @Option(
            shortName = {"s"},
            longName = {"sirius"},
            description = "Sirius output directory or workspace. This is the input for Zodiac",
            defaultToNull = true
    )
    String getSirius();

    //todo @Marcus for the future this should ne replaced with the sirius input property if possible
    @Deprecated
    @Option(
            longName = {"spectra"},
            description = "The file of spectra (.mgf) which was used to compute the trees",
            defaultToNull = true
    )
    String getSpectraFile();

    //todo duplicate
    //naming
    @Option(longName = "naming-convention", description = "Specify a format for compounds' output directorys. Default %index_%filename_%compoundname",  defaultToNull = true)
    String getNamingConvention();


    //todo duplicate
    @Option(
            shortName = {"o"},
            longName = {"output"},
            description = "output directory",
            defaultToNull = true
    )
    String getOutput();


    @Option(
            longName = {"spectral-hits"},
            description = "csv with spectral library hits",
            defaultToNull = true
    )
    String getLibraryHitsFile();

    @Option(
            shortName = {"i"},
            longName = {"iterations"},
            description = "number of iterations",
            defaultValue = {"100000"}
    )
    int getIterationSteps();

    @Option(
            shortName = {"b"},
            longName = {"burn-in"},
            description = "number of steps to use to burn in gibbs sampler.",
                defaultValue = {"10000"}
    )
    int getBurnInSteps();

    @Option(
            longName = {"separateRuns"},
            description = "number of separate runs",
            defaultValue = {"10"}
    )
    int getSeparateRuns();


    @Option(
            longName = {"minLocalCandidates"},
            description = "minimum number of candidates per compound which must have at least --minLocalConnections connections to other compounds",
            defaultValue = {"-1"}
    )
    int getLocalFilter();

    @Option(
            longName = {"thresholdFilter", "thresholdfilter"},
            description = "Defines the proportion of edges of the complete network which will be ignored. Default is 0.95 = 95%",
            defaultValue = {"0.95"}
    )
    double getThresholdFilter();

    @Option(
            longName = {"minLocalConnections"},
            description = "",
            defaultValue = {"-1"}
    )
    int getMinLocalConnections();

    @Option(
            longName = {"distribution"},
            description = "which probability distribution to assume: lognormal, exponential",
            defaultValue = {"lognormal"}
    )
    EdgeScorings getProbabilityDistribution();

    @Option(longName={"estimate-param"}, description = "parameters of distribution are estimated from the data. By default standard parameters are assumed.")
    boolean isEstimateDistribution();

    @Option(
            longName = {"candidates"},
            description = "maximum number of candidate molecular formulas per compound to use in Zodiac. default: all",
            defaultToNull = true
    )
    Integer getNumberOfCandidates();

    @Option(
            longName = {"cluster"},
            description = "cluster compounds with the same best molecular formula candidate before running ZODIAC."
    )
    boolean isClusterCompounds();

    @Option(
            longName = {"processors", "cores"},
            description = "Number of cpu cores to use. If not specified Sirius uses all available cores.",
            defaultValue = "-1"
    )
    int getNumOfCores();


    @Option(
            longName = {"isolation-window-width"},
            description = "width of the isolation window to measure MS2",
            defaultToNull = true
    )
    Double getIsolationWindowWidth();

    @Option(
            longName = {"isolation-window-shift"},
            description = "The shift applied to the isolation window to measure MS2 in relation to the precursormass",
            defaultValue = "0"
    )
    double getIsolationWindowShift();

    @Option(
            longName = {"compute-statistics-only"},
            description = "only compute the dataset statistics without running ZODIAC"
    )
    boolean isOnlyComputeStats();

    @Option(
            longName = {"ms2-median-noise"},
            description = "Set MS2 median noise intensity - else it is estimated. This is used to count the number of MS2 peaks to gauge spectrum quality.",
            defaultToNull = true
    )
    Double getMedianNoiseIntensity();

    @Option(
            longName = {"ignore-spectra-quality"},
            description = "As default ZODIAC runs a 2-step approach. First running 'good quality compounds' only, and afterwards including the remaining."
    )
    boolean isOnlyOneStepZodiac();

//    @Option(
//            longName = {"tree-scores"},
//            description = "Use the probabilistic scores from fragmentation trees instead of count scoring."
//    )
//    boolean isUseTreeScoresForScoring();
//
    @Option(
            longName = {"intensity-scores"},
            description = "Use the probabilistic peak is no noise scores."
    )
    boolean isUsePeakIntensityForScoring();


    @Option(
            longName = {"cross-validation"},
            description = "Doing a cross validation on the library anchors. Expects the number of folds as input.",
            defaultToNull = true,
            hidden = true
    )
    Integer getCrossvalidationNumberOfFolds();

//some evaluation parameters
    @Option(
            longName = {"compound-disjoint-cv"},
            description = "Doing a compound structure disjoint cross validation and not a library structure disjoint evaluation.",
            hidden = true
    )
    boolean isCompoundDisjointCV();

    @Option(
            longName = {"random-cv"},
            description = "Randomly put library hits into bins.",
            hidden = true
    )
    boolean isRandomCV();

    @Option(
            longName = {"mf-disjoint-cv"},
            description = "Cross validation on the estimated molecular formulas of compounds with anchors.",
            hidden = true
    )
    boolean isMFDisjointCV();


    @Option(
            longName = {"all-anchors-good-quality"},
            description = "Treat all compounds with library anchor as 'Good quality' which means they are always used for ZODIAC optimization",
            hidden = true
    )
    boolean isAllAnchorsHaveGoodQuality();

    @Option(
            longName = {"exclude-bad-quality-anchors"},
            description = "Exclude all compounds with library anchors with 'Bad quality'. This has influence on the cross-validation bins.",
            hidden = true
    )
    boolean isExcludeBadQualityAnchors();

    @Option(
            longName = {"fix-anchors"},
            description = "For anchors: remove all other candidates and do not additionally score agreeing MF.",
            hidden = true
    )
    boolean isFixAnchors();

    @Option(
            longName = {"run-good-quality-only"},
            description = "do not run another round of Gibbs sampling for the bad quality spectra."
    )
    boolean isRunGoodQualityOnly();


    @Option(
            longName = {"compute-compounds-without-library-hits"},
            description = "In cross-validation: also compute and output results for compounds without library hit."
    )
    boolean isComputeResultsForCompoundsWithoutLibraryHits();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();


}
