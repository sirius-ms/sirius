package de.unijena.bioinf.ms.cli;

import com.lexicalscope.jewel.cli.Option;
import de.unijena.bioinf.GibbsSampling.model.scorer.EdgeScorings;

/**
 * Created by ge28quv on 22/05/17.
 */
public interface ZodiacOptions extends SiriusOptions {

    /////////////////////////////////////////////////
    // run Zodiac

    @Option(longName = {"zodiac"}, description = "run zodiac on a given sirius workspace.", hidden = true)
    boolean isZodiac();

    /////////////////////////////////////////////////

    /*@Option(
            shortName = {"s"},
            longName = {"sirius"},
            description = "Sirius output directory or workspace. This is the input for Zodiac"
    )
    String getInput();*/

    //todo @Marcus for the future this should ne replaced with the sirius input property if possible
    @Option(
            longName = {"spectra"},
            description = "The file of spectra (.mgf) which was used to compute the trees",
            defaultToNull = true
    )
    String getSpectraFile();


  /*  @Option(
            shortName = {"o"},
            longName = {"output"},
            description = "output directory"
    )
    String getOutputPath();*/


    @Option(
//            shortName = {"h"},
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

    /*@Option(
            longName = {"candidates"},
            description = "maximum number of candidate molecular formulas per compound to use in Zodiac. default: all",
            defaultValue = "-1"
    )
    int getMaxNumOfCandidates();*/




}
