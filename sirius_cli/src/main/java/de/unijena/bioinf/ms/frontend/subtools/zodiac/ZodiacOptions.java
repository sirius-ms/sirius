package de.unijena.bioinf.ms.frontend.subtools.zodiac;

import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainJob;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.FingerIdOptions;
import de.unijena.bioinf.ms.frontend.subtools.passatutto.PassatuttoOptions;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * This is for Zodiac specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "zodiac", aliases = {"Z"}, description = "<DATASET_TOOL> Identify Molecular formulas of all compounds in a dataset together using ZODIAC.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class ZodiacOptions implements ToolChainOptions<ZodiacSubToolJob, DataSetJob.Factory<ZodiacSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public ZodiacOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }


    @Option(names = "--considered-candidates", descriptionKey = "ZodiacNumberOfConsideredCandidates",
            description = {"Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC."})
    public void setNumberOfConsideredCandidates(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacNumberOfConsideredCandidates", value);
    }

    ///////////////////////
    //library hits     ///
    /////////////////////
    @Option(names = "--min-cosine", descriptionKey = "ZodiacLibraryScoring.minCosine",
            description = {"Spectral library hits must have at least this cosine or higher to be considered in scoring.","Value must be in [0,1]."})
    public void setMinCosine(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacLibraryScoring.minCosine", value);
    }

    @Option(names = "--lambda", descriptionKey = "ZodiacLibraryScoring.lambda",
            description = {"Lambda used in the scoring function of spectral library hits. The higher this value the higher are library hits weighted in ZODIAC scoring."})
    public void setLambda(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacLibraryScoring.lambda", value);
    }

    public Path libraryHitsFile;

    @Option(names = "--library-hits",
            description = {"CSV file containing spectral library hits. Library hits are used as anchors to improve ZODIAC scoring."})
    public void setLibraryHits(String filePath) throws Exception {
        libraryHitsFile = Paths.get(filePath);
    }

    ///////////////////////
    //number of epochs///
    /////////////////////
    @Option(names = "--iterations", descriptionKey = "ZodiacEpochs.iterations",
            description = {"Number of epochs to run the Gibbs sampling. When multiple Markov chains are computed, all chains' iterations sum up to this value."})
    public void setIterationSteps(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEpochs.iterations", value);
    }

    @Option(names = "--burn-in", descriptionKey = "ZodiacEpochs.burnInPeriod",
            description = {"Number of epochs considered as 'burn-in period'."})
    public void setBurnInSteps(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEpochs.burnInPeriod", value);
    }

    @Option(names = "--separateRuns", hidden = true, descriptionKey = "ZodiacEdgeFilterThresholds.numberOfMarkovChains",
            description = {"Number of separate Gibbs sampling runs."})
    public void setSeparateRuns(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.numberOfMarkovChains", value);
    }


    ///////////////////////////
    //edge filter parameters//
    /////////////////////////
    //
    @Option(names = "--thresholdFilter", descriptionKey = "ZodiacEdgeFilterThresholds.thresholdFilter",
            description = {"Defines the proportion of edges of the complete network which will be ignored."})
    public void setThresholdFilter(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.thresholdFilter", value);
    }

    //0d for filtering on the fly
    @Option(names = "--minLocalConnections", descriptionKey = "ZodiacEdgeFilterThresholds.minLocalConnections",
            description = {"Minimum number of compounds to which at least one candidate per compound must be connected to."})
    public void setMinLocalConnections(DefaultParameter value) throws Exception {
        defaultConfigOptions.changeOption("ZodiacEdgeFilterThresholds.minLocalConnections", value);
    }


    ///////////////////////////
    // others               //
    /////////////////////////

    @Option(names = "--ignore-spectra-quality",
            description = {"As default ZODIAC runs a 2-step approach. First running 'good quality compounds' only, and afterwards including the remaining."})
    public void disableZodiacTwoStepApproach(boolean disable) throws Exception {
        if (disable)
            defaultConfigOptions.changeOption("ZodiacRunInTwoSteps", "false");
    }

    public Path summaryFile;

    @Option(names = "--summary", hidden = true, description = {"Write a ZODIAC summary CSV file."})
    public void setSummaryFile(String filePath) throws Exception {
        summaryFile = Paths.get(filePath);
    }

    public Path bestMFSimilarityGraphFile;
    @Option(names = "--graph", hidden = true,
            description = {"Writes the similarity graph for based on the top molecular formula annotations of each compound."})
    public void setSimilarityGraphFile(String filePath) throws Exception {
        bestMFSimilarityGraphFile = Paths.get(filePath);
    }

    @Override
    public DataSetJob.Factory<ZodiacSubToolJob> call() throws Exception {
        return (sub) -> new ZodiacSubToolJob(this, sub);
    }

    @Override
    public ToolChainJob.Invalidator getInvalidator() {
        return null;
    }

    @Override
    public List<Class<? extends ToolChainOptions<?, ?>>> getSubCommands() {
        return List.of(PassatuttoOptions.class, FingerIdOptions.class);
    }
}
