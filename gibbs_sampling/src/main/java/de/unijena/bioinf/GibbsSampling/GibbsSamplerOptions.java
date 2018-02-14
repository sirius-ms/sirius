package de.unijena.bioinf.GibbsSampling;

import com.lexicalscope.jewel.cli.Option;

import java.util.List;

public interface GibbsSamplerOptions {



    @Option(
            shortName = {"mgf"},
            longName = {"mgf"},
            description = "spectrum file"
    )
    String getSpectrumsFile();

    @Option(
            shortName = {"c"},
            longName = {"correct"},
            description = "csv with correct hits"
    )
    String getCorrectHitsFile();

    @Option(
            shortName = {"t"},
            longName = {"trees"},
            description = "tree dir"
    )
    String getTreeDir();

    @Option(
            shortName = {"o"},
            longName = {"output"},
            description = "output file"
    )
    String getOutputPath();

    @Option(
            shortName = {"g"},
            longName = {"graphOutput"},
            description = "output dir to write graph",
            defaultToNull = true
    )
    String getOutputDirPath();

    @Option(
            shortName = {"i"},
            longName = {"iterations"},
            description = "number of iterations",
            defaultValue = {"100000"}
    )
    int getIterationSteps();

    @Option(
            shortName = {"b"},
            longName = {"burnIn"},
            description = "number of steps to use to burn in gibbs sampler.",
            defaultValue = {"20000"}
    )
    int getBurnInSteps();

    @Option(
            longName = {"minLocalCandidates"},
            description = "minimum number of candidates per compound which must have at least --minLocalConnections connections to other compounds",
            defaultValue = {"-1"}
    )
    int getLocalFilter();

    @Option(
            longName = {"thresholdFilter"},
            description = "",
            defaultValue = {"-1"}
    )
    double getThresholdFilter();

    @Option(
            longName = {"minLocalConnections"},
            description = "",
            defaultValue = {"-1"}
    )
    int getMinLocalConnections();

    @Option(
            longName = {"top"},
            description = "maximum number of MF candidates per compound",
            defaultValue = {"50"}
    )
    int getMaxCandidates();

    @Option(
            longName = {"librarySearch"},
            description = "use spectral library hits for scoring",
            defaultValue = {"-1"}
    )
    double getLibrarySearchScore();

    @Option(
            longName = {"cv"},
            description = "crossvalidation"
    )
    boolean isCrossvalidation();

    @Option(
            longName = {"robustnesstest"},
            description = "robustness test"
    )
    boolean isRobustnessTest();

    @Option(
            longName = {"align"},
            description = "align fragmentation trees"
    )
    boolean isFTAlign();

//    @Option(
//            longName = {"pcp"},
//            description = "file with score and 1-PEP for CommonFragmentAndLossScorer",
//            defaultToNull = true
//    )
//    String getPCPScoreFile();

    @Option(
            longName = {"correct"},
            description = "file with score and probability density function of correct hit distribution",
            defaultToNull = true
    )
    String getCorrectPdfFile();

    @Option(
            longName = {"sampleScores"},
            description = "sample scores"
    )
    boolean isSampleScores();

    @Option(
            longName = {"makeStats"},
            description = "make stats"
    )
    boolean isMakeStats();

    @Option(
            longName = {"distribution"},
            description = "which probability distribution to assume: exponential, lognormal, pareto. default is lognormal",
            defaultValue = {"exponential"}
    )
    String getProbabilityDistribution();

    @Option(
            longName = {"parameters"},
            description = "parameter for distribution, comma separated. If not set, it is estimated from the distribution. For lognormal give mean and variance. For exponential give lambda.",
            defaultToNull = true
    )
    String getParameters();



    @Option(
            longName = {"median"},
            description = "estimate distribution by median"
    )
    boolean isMedian();


    @Option(longName = "twophase", description = "do 2 rounds of gibbs sampling. First one with good quality compounds, second one with all.")
    boolean isTwoPhase();

    @Option(longName = "threephase")
    boolean isThreePhase();

    @Option(
            longName = {"eval"},
            description = "evaluate zodiac cli output",
            defaultToNull = true
    )
    String getEvalCliOutput();

    @Option(
            longName = {"clusters"},
            description = "cluster.csv file; only used for eval",
            defaultToNull = true
    )
    String getClusterSummary();

//    @Option(longName = "test", description = "do some tests on graph generation", defaultToNull = true)
//    boolean isTestGraphGeneration();

}
