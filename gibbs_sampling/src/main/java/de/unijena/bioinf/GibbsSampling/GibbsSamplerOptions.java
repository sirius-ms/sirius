package de.unijena.bioinf.GibbsSampling;

import com.lexicalscope.jewel.cli.Option;

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
            longName = {"normalize"},
            description = "normalize edge scores"
    )
    boolean isNormalize();

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
            longName = {"align"},
            description = "align fragmentation trees"
    )
    boolean isFTAlign();

    @Option(
            longName = {"pcp"},
            description = "file with score and 1-PEP for CommonFragmentAndLossScorer",
            defaultToNull = true
    )
    String getPCPScoreFile();

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
            description = "which probability distribution to assume: exponential, pareto",
            defaultValue = {"exponential"}
    )
    String getProbabilityDistribution();

    @Option(
            longName = {"lambda"},
            description = "lambda for exponential distribution. If not set, it is estimated from the distribution",
            defaultValue = {"-1"}
    )
    double getLambda();



    @Option(
            longName = {"median"},
            description = "estimate distribution by median"
    )
    boolean isMedian();


    @Option(longName = "twophase", description = "do 2 rounds of gibbs sampling. First one with good quality compounds, second one with all.")
    boolean isTwoPhase();

//    @Option(
//            longName = {"treescoring"},
//            description = ""
//    )
//    boolean useFTScoring();


}
