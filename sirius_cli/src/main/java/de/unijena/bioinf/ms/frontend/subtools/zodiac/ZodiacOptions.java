package de.unijena.bioinf.ms.frontend.subtools.zodiac;

import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import picocli.CommandLine;
import picocli.CommandLine.Option;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * This is for Zodiac specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "zodiac", aliases = {"Z"}, description = "<DATASET_TOOL> Identify Molecular formulas of all compounds in a dataset together using ZODIAC.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true)
public class ZodiacOptions implements Callable<DataSetJob.Factory<ZodiacSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public ZodiacOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }


    @Option(names = "--considered-candidates", description = "Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC. default: all")
    public void setNumberOfConsideredCandidates(String value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacNumberOfConsideredCandidates", value);
    }
    ///////////////////////
    //library hits     ///
    /////////////////////
     @Option(names = "--min-cosine", description = "Spectral library hits must have at least this cosine or higher to be considered in scoring. Value must be in [0,1].")
    public void setMinCosine(String value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacLibraryScoring.minCosine", value);
    }

    @Option(names = "--lambda", description = "Lambda used in the scoring function of spectral library hits. The higher this value the higher are librar hits weighted in ZODIAC scoring.")
    public void setLambda(String value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacLibraryScoring.lambda", value);
    }

    public Path libraryHitsFile;
    @Option(names = "--library-hits", description = "CSV file containing spectral library hits. Library hits are used as anchors to improve ZODIAC scoring.")
    public void setLibraryHits(String filePath) throws Exception {
        libraryHitsFile = Paths.get(filePath);
    }

    ///////////////////////
    //number of epochs///
    /////////////////////
    @Option(names = "--iterations", description = "Number of epochs to run the Gibbs sampling. When multiple Markov chains are computed, all chains' iterations sum up to this value.")
    public void setIterationSteps(String value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.iterations", value);
    }

    @Option(names = "--burn-in", description = "Number of epochs considered as 'burn-in period'.")
    public void setBurnInSteps(String value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.burnInPeriod", value);
    }

    @Option(names = "--separateRuns", description = "Number of separate Gibbs sampling runs.", hidden = true)
    public void setSeparateRuns(String value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.numberOfMarkovChains", value);
    }



    ///////////////////////////
    //edge filter parameters//
    /////////////////////////
    //
    @Option(names = "--thresholdFilter", description = " Defines the proportion of edges of the complete network which will be ignored.")
    public void setThresholdFilter(String value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.thresholdFilter", value);
    }

    //0d for filtering on the fly
    @Option(names = "--minLocalConnections", description = "Minimum number of compounds to which at least one candidate per compound must be connected to.")
    public void setMinLocalConnections(String value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.minLocalConnections", value);
    }


    ///////////////////////////
    // others               //
    /////////////////////////

    @Option(names = "--ignore-spectra-quality", description = "As default ZODIAC runs a 2-step approach. First running 'good quality compounds' only, and afterwards including the remaining.")
    public void disableZodiacTwoStepApproach(boolean disable) throws Exception {
        if (disable){
            defaultConfigOptions.changeOption("ZodiacRunInTwoSteps", "false");
        }
    }

    public Path summaryFile;
    @Option(names = "--summary", description = "Write a ZODIAC summary CSV file.", hidden = true)
    public void setSummaryFile(String filePath) throws Exception {
        summaryFile = Paths.get(filePath);
    }

    @Override
    public DataSetJob.Factory<ZodiacSubToolJob> call() throws Exception {
        return () -> new ZodiacSubToolJob(this);
    }
}
