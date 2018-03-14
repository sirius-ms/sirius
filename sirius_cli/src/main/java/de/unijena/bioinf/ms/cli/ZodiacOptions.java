package de.unijena.bioinf.ms.cli;

import com.lexicalscope.jewel.cli.Option;
import de.unijena.bioinf.GibbsSampling.model.scorer.EdgeScorings;

/**
 * Created by ge28quv on 22/05/17.
 */
public interface ZodiacOptions {



    @Option(
            shortName = {"s"},
            longName = {"sirius"},
            description = "Sirius output directory or workspace. This is the input for Zodiac",
            defaultToNull = true
    )
    String getSirius();

    //todo @Marcus for the future this should ne replaced with the sirius input property if possible
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
            longName = {"processors", "cores"},
            description = "Number of cpu cores to use. If not specified Sirius uses all available cores.",
            defaultValue = "-1"
    )
    int getNumOfCores();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();


}
