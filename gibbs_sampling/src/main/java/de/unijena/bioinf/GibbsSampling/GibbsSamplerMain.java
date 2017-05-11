package de.unijena.bioinf.GibbsSampling;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.GibbsSampling.GibbsSamplerOptions;
import de.unijena.bioinf.GibbsSampling.LibraryHitQuality;
import de.unijena.bioinf.GibbsSampling.model.ReactionStepSizeScorer.ConstantReactionStepSizeScorer;
import de.unijena.bioinf.GibbsSampling.model.ReactionStepSizeScorer.ExponentialReactionStepSizeScorer;
import de.unijena.bioinf.GibbsSampling.model.distributions.DummyScoreProbabilityDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.EmpiricalScoreProbabilityDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ExponentialDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ParetoDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonRootLossScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.ReactionScorer;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.fingerid.SpectralPreprocessor;
import de.unijena.bioinf.sirius.Sirius;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GibbsSamplerMain {
    private static Path formulaFile = Paths.get("/home/ge28quv/Data/gibbsSample/gnpsData/computedTrees/self-training-output_louis_trees_all.csv", new String[0]);
    private static Path outputFile = Paths.get("/home/ge28quv/Data/gibbsSample/gnpsData/computedTrees/gibbs_sample_X.csv", new String[0]);
    private static Path treeDir = Paths.get("/home/ge28quv/Data/gibbsSample/gnpsData/computedTrees/louis_trees_all", new String[0]);
    private static Path mgfFile = Paths.get("/home/ge28quv/Data/gibbsSample/gnpsData/spectral_data_ms1-2.mgf", new String[0]);
    private static Path libraryHits = Paths.get("/home/ge28quv/Data/gibbsSample/gnpsData/libraryHitParsing/cleanHitsTable.csv", new String[0]);
    private static Path graphOutputDir = Paths.get("/home/ge28quv/Data/gibbsSample/gnpsData/gibbsNetAll2", new String[0]);
    private static int iterationSteps = 100000;
    private static int burnInIterations = 10000;
    private static EdgeFilter edgeFilter = new EdgeThresholdFilter(1.0D);
    private static boolean normalize = false;
    private static int maxCandidates = 50;
    private static boolean useLibraryHits = true;
    private static double libraryScore = 1.0D;
    final String SEP = "\t";
    private static final String[] reactionStringsMyCompoundID = new String[]{"H2", "CH2", "NH", "O", "NH3", "H2O", "CO", "C2H4", "C2H2O", "CO2", "C2H3NO", "SO3", "HPO3", "C4H3N3", "C4H2N2O", "C3H5NOS", "C2H5NO2S", "C5H4N2O", "C3H5NO2S", "C5H8O4", "C5H3N5", "C7H13NO2", "C5H7NO3S", "C6H10O5", "C6H8O6", "C10H12N2O4", "C9H11N3O4", "C9H10N2O5", "C16H30O", "C6H11O8P", "C10H11N5O3", "C10H11N5O4", "C10H15N3O5S", "C10H15N3O6S", "C12H20O10", "C18H30O15"};
    private static final String[] reactionStringsRogers = new String[]{"C10H11N5O3", "C10H11N5O4", "C10H12N2O4", "C10H12N5O6P", "C10H12N5O7P", "C10H13N2O7P", "C10H13N5O10P2", "C10H13N5O9P2", "C10H14N2O10P2", "C10H14N2O2S", "C10H15N2O3S", "C10H15N3O5S", "C11H10N2O", "C12H20O11", "C16H30O", "C18H30O15", "C21H33N7O15P3S", "C21H34N7O16P3S", "C2H2", "C2H2O", "C2H3NO", "C2H3O2", "C2H4", "C2O2", "C3H2O3", "C3H5NO", "C3H5NO2", "C3H5NOS", "C3H5O", "C4H3N2O2", "C4H4N3O", "C4H4O2", "C4H5NO3", "C4H6N2O2", "C4H7NO2", "C5H4N5", "C5H4N5O", "C5H5N2O2", "C5H7", "C5H7NO", "C5H7NO3", "C5H8N2O2", "C5H8O4", "C5H9NO", "C5H9NOS", "C6H10N2O3S2", "C6H10O5", "C6H10O6", "C6H11NO", "C6H11O8P", "C6H12N2O", "C6H12N4O", "C6H7N3O", "C6H8O6", "C8H8NO5P", "C9H10N2O5", "C9H11N2O8P", "C9H12N2O11P2", "C9H12N3O7P", "C9H13N3O10P2", "C9H9NO", "C9H9NO2", "CH2", "CH2ON", "CH3N2O", "CHO2", "CO", "CO2", "H2", "H2O", "H3O6P2", "HPO3", "N", "NH", "NH2", "O", "P", "PP", "SO3"};

    public GibbsSamplerMain() {
    }

    public static void main(String[] args) throws IOException {
        GibbsSamplerMain main = new GibbsSamplerMain();
        if(args.length != 0 && !args[0].equals("--help") && !args[0].equals("-h")) {
            GibbsSamplerOptions opts = (GibbsSamplerOptions)CliFactory.createCli(GibbsSamplerOptions.class).parseArguments(args);
            mgfFile = Paths.get(opts.getSpectrumsFile(), new String[0]);
            libraryHits = Paths.get(opts.getCorrectHitsFile(), new String[0]);
            outputFile = Paths.get(opts.getOutputPath(), new String[0]);
            treeDir = Paths.get(opts.getTreeDir(), new String[0]);
            if(opts.getOutputDirPath() != null) {
                graphOutputDir = Paths.get(opts.getOutputDirPath(), new String[0]);
            } else {
                graphOutputDir = null;
            }

            iterationSteps = opts.getIterationSteps();
            burnInIterations = opts.getBurnInSteps();
            normalize = opts.isNormalize();
            if(opts.getThresholdFilter() > 0.0D && opts.getLocalFilter() > 0.0D) {
                edgeFilter = new EdgeThresholdMinConnectionsFilter(opts.getThresholdFilter(), opts.getLocalFilter(), opts.getMinLocalConnections());
            } else if(opts.getThresholdFilter() > 0.0D) {
                edgeFilter = new EdgeThresholdFilter(opts.getThresholdFilter());
            } else if(opts.getLocalFilter() > 0.0D) {
                edgeFilter = new LocalEdgeFilter(opts.getLocalFilter());
            }

            if(edgeFilter == null) {
                edgeFilter = new EdgeThresholdFilter(opts.getThresholdFilter());
            }

            maxCandidates = opts.getMaxCandidates();
            if(opts.getLibrarySearchScore() > 0.0D) {
                useLibraryHits = true;
                libraryScore = opts.getLibrarySearchScore();
            } else {
                useLibraryHits = false;
            }

            System.out.println("arguments " + Arrays.toString(args));
            if(normalize) {
                System.out.println("do normalize");
            } else {
                System.out.println("don\'t normalize");
            }

            byte stepsize = 2;
            Reaction[] reactions = parseReactions(stepsize);
            new ReactionScorer(reactions, new ExponentialReactionStepSizeScorer(4.0D));
            EdgeScorer[] edgeScorers;
            if(opts.getPCPScoreFile() != null) {
                ScoreProbabilityDistribution scoreProbabilityDistribution = readPCP(opts.getPCPScoreFile());
                CommonFragmentAndLossScorer probabilityDistribution = new CommonFragmentAndLossScorer(opts.getThresholdFilter());
                edgeScorers = new EdgeScorer[]{probabilityDistribution};
            } else {
                new DummyScoreProbabilityDistribution();
                Object probabilityDistribution1;
                if(opts.getProbabilityDistribution().toLowerCase().equals("exponential")) {
                    probabilityDistribution1 = new ExponentialDistribution(0.0D, opts.getThresholdFilter(), opts.isMedian());
                } else if(opts.getProbabilityDistribution().toLowerCase().equals("pareto")) {
                    probabilityDistribution1 = new ParetoDistribution(opts.getThresholdFilter(), opts.isMedian());
                } else {
                    if(!opts.getProbabilityDistribution().toLowerCase().equals("lognormal") && !opts.getProbabilityDistribution().toLowerCase().equals("log-normal")) {
                        System.out.println("unkown distribution function");
                        return;
                    }

                    probabilityDistribution1 = new LogNormalDistribution(opts.getThresholdFilter(), opts.isMedian());
                }

                double minimumOverlap = 0.1D;
                ScoreProbabilityDistributionEstimator commonFragmentAndLossScorer = new ScoreProbabilityDistributionEstimator(new CommonFragmentAndLossScorer(0.1D), (ScoreProbabilityDistribution)probabilityDistribution1);
                edgeScorers = new EdgeScorer[]{commonFragmentAndLossScorer};
            }

            if(opts.isSampleScores()) {
                main.sampleFromScoreDistribution(treeDir, mgfFile, libraryHits, outputFile, edgeScorers);
            } else if(opts.isCrossvalidation()) {
                main.doCVEvaluation(treeDir, mgfFile, libraryHits, outputFile, edgeScorers);
            } else {
                main.doEvaluation(treeDir, mgfFile, libraryHits, outputFile, edgeScorers);
            }

        } else {
            System.out.println(CliFactory.createCli(GibbsSamplerOptions.class).getHelpMessage());
        }
    }

    private static ScoreProbabilityDistribution readPCP(String pathString) throws IOException {
        Path path = Paths.get(pathString, new String[0]);
        TDoubleArrayList scores = new TDoubleArrayList();
        TDoubleArrayList probabilities = new TDoubleArrayList();
        List lines = Files.readAllLines(path);
        String header = (String)lines.remove(0);
        if(!header.equals("score\tpcp")) {
            throw new RuntimeException("incorrect pcp header");
        } else {
            Iterator var6 = lines.iterator();

            while(var6.hasNext()) {
                String l = (String)var6.next();
                String[] col = l.split("\t");
                scores.add(Double.parseDouble(col[0]));
                probabilities.add(Double.parseDouble(col[1]));
            }

            return new EmpiricalScoreProbabilityDistribution(scores.toArray(), probabilities.toArray());
        }
    }

    protected void sampleFromScoreDistribution(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers) throws IOException {
        System.out.println("sample scores");
        int workerCount = Runtime.getRuntime().availableProcessors();
        if(Runtime.getRuntime().availableProcessors() > 20) {
            workerCount /= 2;
        }

        Set netSingleReactionDiffs = (Set)Arrays.stream(parseReactions(1)).map((r) -> {
            return r.netChange();
        }).collect(Collectors.toSet());
        Map candidatesMap = this.parseMFCandidates(treeDir, mgfFile, maxCandidates, workerCount);
        PrecursorIonType[] ionTypes = (PrecursorIonType[])Arrays.stream(new String[]{"[M+H]+", "[M]+", "[M+K]+", "[M+Na]+"}).map((s) -> {
            return PrecursorIonType.getPrecursorIonType(s);
        }).toArray((l) -> {
            return new PrecursorIonType[l];
        });
        this.guessIonizationAndRemove(candidatesMap, ionTypes);
        this.parseLibraryHits(libraryHitsPath, candidatesMap);
        Map correctHits = this.identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);
        double useFreq = 0.0D;
        this.extractEvaluationIds(candidatesMap, correctHits, useFreq, netSingleReactionDiffs);
        String[] ids = (String[])candidatesMap.keySet().stream().filter((key) -> {
            return ((List)candidatesMap.get(key)).size() > 0;
        }).toArray((s) -> {
            return new String[s];
        });
        MFCandidate[][] candidatesArray = new MFCandidate[ids.length][];

        for(int commonFragmentAndLossScorer = 0; commonFragmentAndLossScorer < ids.length; ++commonFragmentAndLossScorer) {
            String commonFragmentScorer = ids[commonFragmentAndLossScorer];
            candidatesArray[commonFragmentAndLossScorer] = (MFCandidate[])((List)candidatesMap.get(commonFragmentScorer)).toArray(new MFCandidate[0]);
        }

        CommonFragmentAndLossScorer var55 = new CommonFragmentAndLossScorer(0.0D);
        CommonFragmentScorer var56 = new CommonFragmentScorer(1.0D);
        CommonRootLossScorer commonLossScorer = new CommonRootLossScorer();
        var55.prepare(candidatesArray);
        var56.prepare(candidatesArray);
        commonLossScorer.prepare(candidatesArray);
        int numberOfCandidates = 0;
        MFCandidate[][] numberOfEdgesBound = candidatesArray;
        int var21 = candidatesArray.length;

        for(int randThres = 0; randThres < var21; ++randThres) {
            MFCandidate[] candidates = numberOfEdgesBound[randThres];
            numberOfCandidates += candidates.length;
        }

        long var57 = (long)(numberOfCandidates * (numberOfCandidates - maxCandidates));
        System.out.println("numberOfEdgesBound " + var57);
        double var58 = 1000000.0D / (double)var57;
        Path outpath = Paths.get("sampled_scoresAndMatches.csv", new String[0]);
        HighQualityRandom rando = new HighQualityRandom();
        BufferedWriter writer = Files.newBufferedWriter(outpath, new OpenOption[0]);
        Throwable var27 = null;

        try {
            writer.write("MF1\tMF2\ttreeSize1\ttreeSize2\tmass1\tmass2\tcommonF\tcommonL\tCommonFragmentAndLossScorer");

            for(int i = 0; i < candidatesArray.length; ++i) {
                MFCandidate[] candidates1 = candidatesArray[i];

                for(int j = i + 1; j < candidatesArray.length; ++j) {
                    MFCandidate[] candidates2 = candidatesArray[j];
                    MFCandidate[] var32 = candidates1;
                    int var33 = candidates1.length;

                    for(int var34 = 0; var34 < var33; ++var34) {
                        MFCandidate c1 = var32[var34];
                        MFCandidate[] var36 = candidates2;
                        int var37 = candidates2.length;

                        for(int var38 = 0; var38 < var37; ++var38) {
                            MFCandidate c2 = var36[var38];
                            if(rando.nextDouble() < var58) {
                                writer.write("\n" + c1.getFormula().formatByHill() + "\t" + c2.getFormula().formatByHill());
                                int treesize1 = c1.getTree().numberOfVertices();
                                int treesize2 = c2.getTree().numberOfVertices();
                                writer.write("\t" + treesize1 + "\t" + treesize2);
                                writer.write("\t" + c1.getFormula().getMass() + "\t" + c2.getFormula().getMass());
                                int commonF = var56.getNumberOfCommon(c1, c2);
                                int commonL = commonLossScorer.getNumberOfCommon(c1, c2);
                                writer.write("\t" + commonF + "\t" + commonL);
                                double score = var55.score(c1, c2);
                                writer.write("\t" + String.valueOf(score));
                            }
                        }
                    }
                }
            }
        } catch (Throwable var53) {
            var27 = var53;
            throw var53;
        } finally {
            if(writer != null) {
                if(var27 != null) {
                    try {
                        writer.close();
                    } catch (Throwable var52) {
                        var27.addSuppressed(var52);
                    }
                } else {
                    writer.close();
                }
            }

        }

    }

    protected void doEvaluation(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers) throws IOException {
        int workerCount = Runtime.getRuntime().availableProcessors();
        if(Runtime.getRuntime().availableProcessors() > 20) {
            workerCount /= 2;
        }

        Set netSingleReactionDiffs = (Set)Arrays.stream(parseReactions(1)).map((r) -> {
            return r.netChange();
        }).collect(Collectors.toSet());
        Map candidatesMap = this.parseMFCandidates(treeDir, mgfFile, maxCandidates, workerCount);
        PrecursorIonType[] ionTypes = (PrecursorIonType[])Arrays.stream(new String[]{"[M+H]+", "[M]+", "[M+K]+", "[M+Na]+"}).map((s) -> {
            return PrecursorIonType.getPrecursorIonType(s);
        }).toArray((l) -> {
            return new PrecursorIonType[l];
        });
        this.guessIonizationAndRemove(candidatesMap, ionTypes);
        this.parseLibraryHits(libraryHitsPath, candidatesMap);
        Map correctHits = this.identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);
        double useFreq = 0.0D;
        Set evaluationIds = this.extractEvaluationIds(candidatesMap, correctHits, useFreq, netSingleReactionDiffs);
        String[] ids = (String[])candidatesMap.keySet().stream().filter((key) -> {
            return ((List)candidatesMap.get(key)).size() > 0;
        }).toArray((s) -> {
            return new String[s];
        });
        MFCandidate[][] candidatesArray = new MFCandidate[ids.length][];

        for(int nodeScorers = 0; nodeScorers < ids.length; ++nodeScorers) {
            String gibbsParallel = ids[nodeScorers];
            candidatesArray[nodeScorers] = (MFCandidate[])((List)candidatesMap.get(gibbsParallel)).toArray(new MFCandidate[0]);
        }

        System.out.println("before");
        this.statisticsOfKnownCompounds(candidatesArray, ids, evaluationIds, correctHits);
        NodeScorer[] var19;
        if(useLibraryHits) {
            var19 = new NodeScorer[]{new RankNodeScorer(), new LibraryHitScorer(libraryScore, 0.5D, netSingleReactionDiffs)};
            System.out.println("use LibraryHitScorer");
        } else {
            var19 = new NodeScorer[]{new StandardNodeScorer(false, 1.0D)};
            System.out.println("ignore Library Hits");
        }

        GibbsParallel var20 = new GibbsParallel(ids, candidatesArray, var19, edgeScorers, edgeFilter, workerCount);
        if(graphOutputDir != null) {
            this.writeMFNetworkToDir(graphOutputDir, var20.getGraph());
        }

        System.out.println("start");
        var20.iteration(iterationSteps, burnInIterations);
        Scored[][] result = var20.getChosenFormulasBySampling();
        System.out.println("standard");
        this.statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);
        if(outputFile != null) {
            this.writeBestFormulas(result, candidatesArray, var20.getGraph(), outputFile);
        }

        result = var20.getChosenFormulasByAddedUpPosterior();
        System.out.println("addedPosterior");
        this.statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);
        result = var20.getChosenFormulasByMaxPosterior();
        System.out.println("maxPosterior");
        this.statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);
    }

    protected void doCVEvaluation(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers) throws IOException {
        System.out.println("do crossval");
        int workerCount = Runtime.getRuntime().availableProcessors();
        if(Runtime.getRuntime().availableProcessors() > 20) {
            workerCount /= 2;
        }

        Set netSingleReactionDiffs = (Set)Arrays.stream(parseReactions(1)).map((r) -> {
            return r.netChange();
        }).collect(Collectors.toSet());
        Map candidatesMap = this.parseMFCandidates(treeDir, mgfFile, maxCandidates, workerCount);
        PrecursorIonType[] ionTypes = (PrecursorIonType[])Arrays.stream(new String[]{"[M+H]+", "[M]+", "[M+K]+", "[M+Na]+"}).map((s) -> {
            return PrecursorIonType.getPrecursorIonType(s);
        }).toArray((l) -> {
            return new PrecursorIonType[l];
        });
        this.guessIonizationAndRemove(candidatesMap, ionTypes);
        this.parseLibraryHits(libraryHitsPath, candidatesMap);
        Map<String, LibraryHit> correctHits = this.identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);
        new ArrayList(correctHits.keySet());
        List knownLibraryStructures = (List)correctHits.values().stream().map((libraryHit) -> {
            return libraryHit.getStructure();
        }).distinct().collect(Collectors.toList());
        int end = -1;

        for(int fold = 0; fold < 10; ++fold) {
            int start = end + 1;
            end = (int)((double)((fold + 1) * knownLibraryStructures.size()) / 10.0D);
            if(fold == 9) {
                end = knownLibraryStructures.size() - 1;
            }

            System.out.println("start " + start + " end " + end);
            this.resetCorrects(candidatesMap);
            Set evaluationIds = this.extractEvaluationBySpecificStructure(candidatesMap, correctHits, netSingleReactionDiffs, knownLibraryStructures, start, end);
            String[] ids = (String[])candidatesMap.keySet().stream().filter((key) -> {
                return ((List)candidatesMap.get(key)).size() > 0;
            }).toArray((s) -> {
                return new String[s];
            });
            MFCandidate[][] candidatesArray = new MFCandidate[ids.length][];

            for(int nodeScorers = 0; nodeScorers < ids.length; ++nodeScorers) {
                String gibbsParallel = ids[nodeScorers];
                candidatesArray[nodeScorers] = (MFCandidate[])((List)candidatesMap.get(gibbsParallel)).toArray(new MFCandidate[0]);
            }

            System.out.println("before");
            this.statisticsOfKnownCompounds(candidatesArray, ids, evaluationIds, correctHits);
            NodeScorer[] var22;
            if(useLibraryHits) {
                var22 = new NodeScorer[]{new RankNodeScorer(), new LibraryHitScorer(libraryScore, 0.5D, netSingleReactionDiffs)};
                System.out.println("use LibraryHitScorer");
            } else {
                var22 = new NodeScorer[]{new RankNodeScorer()};
                System.out.println("ignore Library Hits");
            }

            GibbsParallel var23 = new GibbsParallel(ids, candidatesArray, var22, edgeScorers, edgeFilter, workerCount);
            System.out.println("start");
            var23.iteration(iterationSteps, burnInIterations);
            Scored[][] result = var23.getChosenFormulasBySampling();
            System.out.println("standard");
            this.statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);
            result = var23.getChosenFormulasByAddedUpPosterior();
            System.out.println("addedPosterior");
            this.statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);
            result = var23.getChosenFormulasByMaxPosterior();
            System.out.println("maxPosterior");
            this.statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);
        }

    }

    private void resetCorrects(Map<String, List<MFCandidate>> candidatesMap) {
        Iterator var2 = candidatesMap.values().iterator();

        while(var2.hasNext()) {
            List candidates = (List)var2.next();
            this.resetCorrects((Collection)candidates);
        }

    }

    private void resetCorrects(Collection<MFCandidate> candidates) {
        MFCandidate candidate;
        for(Iterator var2 = candidates.iterator(); var2.hasNext(); candidate.isCorrect = false) {
            candidate = (MFCandidate)var2.next();
            candidate.inEvaluationSet = false;
            candidate.inTrainingSet = false;
        }

    }

    private void writeBestFormulas(Scored<MFCandidate>[][] results, MFCandidate[][] initialAssignment, Graph graph, Path outputFile) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset(), new OpenOption[0]);
        String SEP = "\t";
        int i = 0;
        Scored[][] var8 = results;
        int var9 = results.length;

        for(int var10 = 0; var10 < var9; ++var10) {
            Scored[] result = var8[var10];
            Scored best = result[0];
            MFCandidate initalBest = initialAssignment[i][0];
            String id = ((MFCandidate)best.getCandidate()).getExperiment().getName();
            String formula = ((MFCandidate)best.getCandidate()).getFormula().formatByHill();
            String iniFormula = initalBest.getFormula().formatByHill();
            int numberOfEdges = graph.getConnections(graph.getAbsoluteFormulaIdx(i, 0)).length;
            double score = (double)result.length / best.getScore();
            writer.write(id + SEP + iniFormula + SEP + formula + SEP + score + SEP + numberOfEdges + "\n");
            ++i;
        }

        writer.close();
    }

    private Set<String> extractEvaluationSpecificIds(Map<String, List<MFCandidate>> candidatesMap, Map<String, LibraryHit> correctHitsMap, Set<MolecularFormula> allowedDifferences, List<String> knownIDs, int start, int end) {
        ArrayList trainIds = new ArrayList(knownIDs.subList(0, start));
        trainIds.addAll(knownIDs.subList(end + 1, knownIDs.size()));
        ArrayList evaluationIds = new ArrayList(knownIDs.subList(start, end + 1));
        System.out.println("used ids: " + Arrays.toString(trainIds.toArray(new String[0])));
        this.setKnownCompounds(trainIds, correctHitsMap, candidatesMap, allowedDifferences, false);
        return this.setKnownCompounds(evaluationIds, correctHitsMap, candidatesMap, allowedDifferences, true);
    }

    private Set<String> extractEvaluationBySpecificStructure(Map<String, List<MFCandidate>> candidatesMap, Map<String, LibraryHit> correctHitsMap, Set<MolecularFormula> allowedDifferences, List<String> knownLibraryStructures, int start, int end) {
        ArrayList trainIds = new ArrayList(knownLibraryStructures.subList(0, start));
        trainIds.addAll(knownLibraryStructures.subList(end + 1, knownLibraryStructures.size()));
        ArrayList evaluationIds = new ArrayList(knownLibraryStructures.subList(start, end + 1));
        System.out.println("used eval structures: " + Arrays.toString(evaluationIds.toArray(new String[0])));
        this.setKnownCompoundsByLibraryStructure(trainIds, correctHitsMap, candidatesMap, allowedDifferences, false);
        return this.setKnownCompoundsByLibraryStructure(evaluationIds, correctHitsMap, candidatesMap, allowedDifferences, true);
    }

    private Set<String> extractEvaluationIds(Map<String, List<MFCandidate>> candidatesMap, Map<String, LibraryHit> correctHitsMap, double useFreq, Set<MolecularFormula> allowedDifferences) {
        ArrayList knownIDs = new ArrayList(correctHitsMap.keySet());
        List trainIds = knownIDs.subList(0, (int)(useFreq * (double)knownIDs.size()));
        List evaluationIds = knownIDs.subList((int)(useFreq * (double)knownIDs.size()), knownIDs.size());
        System.out.println("used ids: " + Arrays.toString(trainIds.toArray(new String[0])));
        this.setKnownCompounds(trainIds, correctHitsMap, candidatesMap, allowedDifferences, false);
        return this.setKnownCompounds(evaluationIds, correctHitsMap, candidatesMap, allowedDifferences, true);
    }

    private Set<String> setKnownCompounds(Collection<String> ids, Map<String, LibraryHit> correctHitsMap, Map<String, List<MFCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences, boolean eval) {
        HashSet usedIds = new HashSet();
        Iterator var7 = ids.iterator();

        while(true) {
            while(var7.hasNext()) {
                String id = (String)var7.next();
                LibraryHit libraryHit = (LibraryHit)correctHitsMap.get(id);
                MolecularFormula correctMF = libraryHit.getMolecularFormula();
                List candidates = (List)candidatesMap.get(id);
                if(candidates == null) {
                    System.out.println();
                    System.err.println("all candidates have been removed: " + id);
                } else {
                    System.out.println("candidates size " + candidates.size() + " for " + id);
                    int correctHits = 0;
                    Iterator var13 = candidates.iterator();

                    while(var13.hasNext()) {
                        MFCandidate candidate = (MFCandidate)var13.next();
                        boolean matches = candidate.getFormula().equals(correctMF);
                        if(!matches) {
                            MolecularFormula diff = candidate.getFormula().subtract(correctMF);
                            if(diff.getMass() < 0.0D) {
                                diff = diff.negate();
                            }

                            matches = allowedDifferences.contains(diff);
                        }

                        if(matches) {
                            candidate.isCorrect = true;
                            ++correctHits;
                        }

                        if(eval) {
                            candidate.inEvaluationSet = true;
                        } else {
                            candidate.inTrainingSet = true;
                        }
                    }

                    assert correctHits <= 1;

                    if(correctHits > 1) {
                        throw new RuntimeException("unexpected number of correct hits : " + correctHits);
                    }

                    if(correctHits == 1) {
                        usedIds.add(id);
                    }
                }
            }

            return usedIds;
        }
    }

    private Set<String> setKnownCompoundsByLibraryStructure(Collection<String> structures, Map<String, LibraryHit> correctHitsMap, Map<String, List<MFCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences, boolean eval) {
        HashSet usedIds = new HashSet();
        Iterator var7 = structures.iterator();

        label80:
        while(var7.hasNext()) {
            String structure = (String)var7.next();
            Iterator var9 = candidatesMap.keySet().iterator();

            while(true) {
                while(true) {
                    String id;
                    LibraryHit libraryHit;
                    do {
                        do {
                            do {
                                if(!var9.hasNext()) {
                                    continue label80;
                                }

                                id = (String)var9.next();
                                libraryHit = (LibraryHit)correctHitsMap.get(id);
                            } while(libraryHit == null);
                        } while(!libraryHit.getStructure().equals(structure));
                    } while(usedIds.contains(id));

                    System.out.println("struct " + libraryHit.getStructure());
                    MolecularFormula correctMF = libraryHit.getMolecularFormula();
                    List candidates = (List)candidatesMap.get(id);
                    if(candidates == null) {
                        System.out.println();
                        System.err.println("all candidates have been removed: " + id);
                    } else {
                        System.out.println("candidates size " + candidates.size() + " for " + id);
                        int correctHits = 0;
                        Iterator var15 = candidates.iterator();

                        while(var15.hasNext()) {
                            MFCandidate candidate = (MFCandidate)var15.next();
                            boolean matches = candidate.getFormula().equals(correctMF);
                            if(!matches) {
                                MolecularFormula diff = candidate.getFormula().subtract(correctMF);
                                if(diff.getMass() < 0.0D) {
                                    diff = diff.negate();
                                }

                                matches = allowedDifferences.contains(diff);
                            }

                            if(matches) {
                                candidate.isCorrect = true;
                                ++correctHits;
                            }

                            if(eval) {
                                candidate.inEvaluationSet = true;
                            } else {
                                candidate.inTrainingSet = true;
                            }
                        }

                        assert correctHits <= 1;

                        if(correctHits > 1) {
                            throw new RuntimeException("unexpected number of correct hits : " + correctHits);
                        }

                        if(correctHits == 1) {
                            usedIds.add(id);
                        }
                    }
                }
            }
        }

        return usedIds;
    }

    private Map<String, LibraryHit> identifyCorrectLibraryHits(Map<String, List<MFCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences) {
        HashMap correctHits = new HashMap();
        Deviation deviation = new Deviation(20.0D);
        Iterator var5 = candidatesMap.keySet().iterator();

        while(true) {
            String id;
            List<MFCandidate> candidateList;
            LibraryHit libraryHit;
            do {
                do {
                    do {
                        if(!var5.hasNext()) {
                            return correctHits;
                        }

                        id = (String)var5.next();
                        candidateList = candidatesMap.get(id);
                    } while(!(candidateList.get(0)).hasLibraryHit());

                    libraryHit = ((MFCandidate)candidateList.get(0)).getLibraryHit();
                    System.out.println("using lower threshold for references: Bronze, cos 0.66, shared 5 peaks");
                } while(libraryHit.getCosine() < 0.66D);
            } while(libraryHit.getSharedPeaks() < 5);

            double theoreticalMass = libraryHit.getIonType().neutralMassToPrecursorMass(libraryHit.getMolecularFormula().getMass());
            double measuredMass = libraryHit.getQueryExperiment().getIonMass();
            PrecursorIonType hitIonization = libraryHit.getIonType().withoutAdduct().withoutInsource();
            boolean sameIonization = candidateList.stream().anyMatch((c) -> {
                return c.getIonType().equals(hitIonization);
            });
            boolean matches = deviation.inErrorWindow(theoreticalMass, measuredMass);
            if(!matches) {
                Iterator var16 = candidateList.iterator();

                while(var16.hasNext()) {
                    MFCandidate candidate = (MFCandidate)var16.next();
                    if(candidate.getIonType().equals(libraryHit.getIonType())) {
                        MolecularFormula mFDiff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                        if(mFDiff.getMass() < 0.0D) {
                            mFDiff = mFDiff.negate();
                        }

                        if(allowedDifferences.contains(mFDiff)) {
                            matches = true;
                            break;
                        }
                    }
                }
            }

            if(matches) {
                if(sameIonization) {
                    correctHits.put(id, libraryHit);
                } else {
                    System.out.println("warning: different ionizations for library hit " + id);
                }
            }
        }
    }

    private void parseLibraryHits(Path libraryHitsPath, Map<String, List<MFCandidate>> candidatesMap) throws IOException {
        List lines = Files.readAllLines(libraryHitsPath, Charset.defaultCharset());
        String[] header = ((String)lines.remove(0)).split("\t");
        String[] ofInterest = new String[]{"Feature_id", "Formula", "Structure", "Adduct", "Cosine", "SharedPeaks", "Quality"};
        int[] indices = new int[ofInterest.length];

        for(int i = 0; i < ofInterest.length; ++i) {
            int line = this.arrayFind(header, ofInterest[i]);
            if(line < 0) {
                throw new RuntimeException("Column " + ofInterest[i] + " not found");
            }

            indices[i] = line;
        }

        Iterator var23 = lines.iterator();

        while(true) {
            while(var23.hasNext()) {
                String var24 = (String)var23.next();
                String[] cols = var24.split("\t");
                String featureId = cols[indices[0]];
                List candidatesList = (List)candidatesMap.get(featureId);
                if(candidatesList == null) {
                    System.out.println("corresponding query (" + featureId + ") to library hit not found");
                    System.err.println("corresponding query (" + featureId + ") to library hit not found");
                } else {
                    Ms2Experiment experiment = ((MFCandidate)candidatesList.get(0)).getExperiment();
                    MolecularFormula formula = MolecularFormula.parse(cols[indices[1]]);
                    String structure = cols[indices[2]];
                    PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(cols[indices[3]]);
                    double cosine = Double.parseDouble(cols[indices[4]]);
                    int sharedPeaks = Integer.parseInt(cols[indices[5]]);
                    LibraryHitQuality quality = LibraryHitQuality.valueOf(cols[indices[6]]);
                    LibraryHit libraryHit = new LibraryHit(experiment, formula, structure, ionType, cosine, sharedPeaks, quality);
                    Iterator var21 = candidatesList.iterator();

                    while(var21.hasNext()) {
                        MFCandidate candidate = (MFCandidate)var21.next();
                        candidate.setLibraryHit(libraryHit);
                    }
                }
            }

            return;
        }
    }

    public Map<String, List<Scored<MFCandidate>>> getScoredCandidatesByTreeScore(Map<String, List<MFCandidate>> candidatesMap) {
        HashMap scoredCandidateMap = new HashMap();
        Iterator var3 = candidatesMap.entrySet().iterator();

        while(var3.hasNext()) {
            Entry entry = (Entry)var3.next();
            String id = (String)entry.getKey();
            List list = (List)entry.getValue();
            ArrayList scoredList = new ArrayList(list.size());
            Iterator var8 = list.iterator();

            while(var8.hasNext()) {
                MFCandidate candidate = (MFCandidate)var8.next();
                double score = ((TreeScoring)candidate.getTree().getAnnotationOrThrow(TreeScoring.class)).getOverallScore();
                if(score > 0.0D) {
                    scoredList.add(new Scored(candidate, score));
                }
            }

            if(scoredList.size() == 0) {
                System.out.println("no candidates anymore");
            } else {
                Collections.sort(scoredList, Scored.desc());
                scoredCandidateMap.put(id, scoredList);
            }
        }

        return scoredCandidateMap;
    }

    public void guessIonizationAndRemove(Map<String, List<MFCandidate>> candidateMap, PrecursorIonType[] ionTypes) {
        ArrayList idList = new ArrayList(candidateMap.keySet());
        Sirius sirius = new Sirius();
        Iterator var5 = idList.iterator();

        while(true) {
            String id;
            List candidates;
            Ms2Experiment experiment;
            PrecursorIonType[] guessed;
            do {
                if(!var5.hasNext()) {
                    return;
                }

                id = (String)var5.next();
                candidates = (List)candidateMap.get(id);
                experiment = ((MFCandidate)candidates.get(0)).getExperiment();
                guessed = sirius.guessIonization(experiment, ionTypes);
            } while(guessed.length == 0);

            PrecursorIonType chosenIonType = experiment.getPrecursorIonType();
            PrecursorIonType chosenIonization = chosenIonType.withoutAdduct().withoutInsource();
            if(!chosenIonization.isIonizationUnknown()) {
                if(!this.arrayContains(guessed, chosenIonization)) {
                    System.out.println("warning: guessed ionization ( " + chosenIonType.toString() + " ) contradicts chosen one.");
                    guessed = (PrecursorIonType[])Arrays.copyOf(guessed, guessed.length + 1);
                    guessed[guessed.length - 1] = chosenIonization;
                } else {
                    guessed = new PrecursorIonType[]{chosenIonization};
                }

                System.out.println("guessed is known " + Arrays.toString(guessed) + " for " + id);
            } else {
                System.out.println("guessed is unknown " + Arrays.toString(guessed) + " for " + id);
            }

            Iterator iterator = candidates.iterator();

            while(iterator.hasNext()) {
                MFCandidate next = (MFCandidate)iterator.next();
                if(!this.arrayContains(guessed, next.getIonType())) {
                    iterator.remove();
                }
            }

            if(candidates.size() == 0) {
                System.out.println("no candidates anymore " + id + " | guessed ionization: " + Arrays.toString(guessed));
                candidateMap.remove(id);
            }
        }
    }

    private int[] statisticsOfKnownCompounds(Scored<MFCandidate>[] result, String[] ids, Set<String> evaluationIDs, Map<String, MolecularFormula> correctHitsMap) {
        ArrayList correctIds = new ArrayList();
        ArrayList wrongIds = new ArrayList();
        int correctId = 0;
        int wrongId = 0;

        for(int i = 0; i < result.length; ++i) {
            Scored candidateScored = result[i];
            String id = ids[i];
            if(correctHitsMap.containsKey(id) && evaluationIDs.contains(id)) {
                MolecularFormula correct = (MolecularFormula)correctHitsMap.get(id);
                if(((MFCandidate)candidateScored.getCandidate()).getFormula().equals(correct)) {
                    ++correctId;
                    correctIds.add(id);
                } else {
                    ++wrongId;
                    wrongIds.add(id);
                }
            }
        }

        return new int[]{correctId, wrongId};
    }

    private int[] statisticsOfKnownCompounds(Scored<MFCandidate>[][] result, String[] ids, Set<String> evaluationIDs, Map<String, LibraryHit> correctHitsMap) {
        byte correctId = 0;
        byte wrongId = 0;

        for(int i = 0; i < result.length; ++i) {
            Scored[] candidatesScored = result[i];
            String id = ids[i];
            if(correctHitsMap.containsKey(id) && evaluationIDs.contains(id)) {
                int pos = 1;
                Scored[] var11 = candidatesScored;
                int var12 = candidatesScored.length;

                for(int var13 = 0; var13 < var12; ++var13) {
                    Scored candidateScored = var11[var13];
                    if(((MFCandidate)candidateScored.getCandidate()).isCorrect) {
                        break;
                    }

                    ++pos;
                }

                if(pos > candidatesScored.length) {
                    System.out.println(id + "not found");
                } else {
                    System.out.println(id + " found at " + pos + " (" + candidatesScored[pos - 1].getScore() + ") of " + candidatesScored.length);
                }
            }
        }

        return new int[]{correctId, wrongId};
    }

    private int[] statisticsOfKnownCompounds(MFCandidate[][] result, String[] ids, Set<String> evaluationIDs, Map<String, LibraryHit> correctHitsMap) {
        byte correctId = 0;
        byte wrongId = 0;

        for(int i = 0; i < result.length; ++i) {
            MFCandidate[] candidates = result[i];
            String id = ids[i];
            if(correctHitsMap.containsKey(id) && evaluationIDs.contains(id)) {
                int pos = 1;
                MFCandidate[] var11 = candidates;
                int var12 = candidates.length;

                for(int var13 = 0; var13 < var12; ++var13) {
                    MFCandidate candidate = var11[var13];
                    if(candidate.isCorrect) {
                        break;
                    }

                    ++pos;
                }

                if(pos > candidates.length) {
                    System.out.println(id + " ( " + candidates[0].getExperiment().getIonMass() + " mz) not found");
                } else {
                    System.out.println(id + " ( " + candidates[0].getExperiment().getIonMass() + " mz) found at " + pos + " (" + candidates[pos - 1].getScore() + ") of " + candidates.length);
                }
            }
        }

        return new int[]{correctId, wrongId};
    }

    private <T> boolean arrayContains(T[] array, T object) {
        return this.arrayFind(array, object) >= 0;
    }

    private <T> int arrayFind(T[] array, T object) {
        for(int i = 0; i < array.length; ++i) {
            Object t = array[i];
            if(t.equals(object)) {
                return i;
            }
        }

        return -1;
    }

    private static Reaction parseReactionString(String string) {
        String[] reactants = string.split("->");
        if(reactants.length == 1) {
            return new SimpleReaction(MolecularFormula.parse(reactants[0]));
        } else if(reactants.length == 2) {
            return new Transformation(MolecularFormula.parse(reactants[0]), MolecularFormula.parse(reactants[1]));
        } else {
            throw new RuntimeException("Error parsing reaction");
        }
    }

    public Map<String, List<MFCandidate>> parseMFCandidates(Path treeDir, Path mgfFile, int maxCandidates, int workercount) throws IOException {
        System.out.println(treeDir.toString());
        Path[] trees = (Path[])Files.find(treeDir, 2, (path, basicFileAttributes) -> {
            return path.toString().endsWith(".json");
        }, new FileVisitOption[0]).toArray((s) -> {
            return new Path[s];
        });
        System.out.println("number " + trees.length);
        MsExperimentParser parser = new MsExperimentParser();
        List allExperiments = parser.getParser(mgfFile.toFile()).parseFromFile(mgfFile.toFile());
        return this.parseMFCandidates(trees, allExperiments, maxCandidates, workercount);
    }

    public Map<String, List<MFCandidate>> parseMFCandidates(Path[] trees, List<Ms2Experiment> experiments, final int maxCandidates, int workercount) throws IOException {
        final SpectralPreprocessor preprocessor = new SpectralPreprocessor((new Sirius()).getMs2Analyzer());
        final HashMap explanationsMap = new HashMap();
        final HashMap experimentMap = new HashMap();
        Iterator service = experiments.iterator();

        while(service.hasNext()) {
            Ms2Experiment futures = (Ms2Experiment)service.next();
            String pos = this.cleanString(futures.getName());
            if(experimentMap.containsKey(pos)) {
                throw new RuntimeException("experiment name duplicate");
            }

            experimentMap.put(pos, futures);
        }

        ExecutorService var20 = Executors.newFixedThreadPool(workercount);
        ArrayList var21 = new ArrayList();
        final int[] var22 = new int[]{0};
        final ConcurrentLinkedQueue pathQueue = new ConcurrentLinkedQueue(Arrays.asList(trees));

        for(int listMap = 0; listMap < workercount; ++listMap) {
            var21.add(var20.submit(new Runnable() {
                public void run() {
                    while(!pathQueue.isEmpty()) {
                        Path treePath = (Path)pathQueue.poll();
                        if(treePath != null) {
                            if(++var22[0] % 1000 == 0) {
                                System.out.println("tree " + var22[0]);
                            }

                            String name = treePath.getFileName().toString();
                            String id = name.split("_")[0];

                            assert id.length() > 0;

                            Ms2Experiment experiment = (Ms2Experiment)experimentMap.get(id);
                            if(experiment == null) {
                                throw new RuntimeException("cannot find experiment");
                            }

                            FTree tree = null;

                            try {
                                tree = (FTree)(new GenericParser(new FTJsonReader())).parseFromFile(treePath.toFile()).get(0);
                            } catch (RuntimeException var12) {
                                System.out.println("cannot read tree " + treePath.getFileName().toString());
                                continue;
                            } catch (IOException var13) {
                                throw new RuntimeException(var13);
                            }

                            if(tree.numberOfVertices() >= 3) {
                                tree = (new IonTreeUtils()).treeToNeutralTree(tree);

                                try {
                                    preprocessor.preprocessTrees(tree);
                                } catch (RuntimeException var11) {
                                    System.out.println("error:" + var11.getMessage() + ", vertices " + tree.numberOfVertices() + " for " + treePath.getFileName().toString());
                                    continue;
                                }

                                PriorityBlockingQueue candidates = (PriorityBlockingQueue)explanationsMap.get(id);
                                if(candidates == null) {
                                    Map var7 = experimentMap;
                                    synchronized(experimentMap) {
                                        candidates = (PriorityBlockingQueue)explanationsMap.get(id);
                                        if(candidates == null) {
                                            candidates = new PriorityBlockingQueue(maxCandidates, Collections.reverseOrder());
                                            explanationsMap.put(id, candidates);
                                        }
                                    }
                                }

                                candidates.add(new MFCandidate(tree, experiment));
                                if(candidates.size() > maxCandidates) {
                                    synchronized(candidates) {
                                        while(candidates.size() > maxCandidates) {
                                            candidates.poll();
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }));
        }

        Iterator var23 = var21.iterator();

        while(var23.hasNext()) {
            Future keys = (Future)var23.next();

            try {
                keys.get();
            } catch (InterruptedException var18) {
                var18.printStackTrace();
                throw new RuntimeException(var18);
            } catch (ExecutionException var19) {
                var19.printStackTrace();
                throw new RuntimeException(var19);
            }
        }

        var20.shutdown();
        HashMap var24 = new HashMap();
        Set var25 = explanationsMap.keySet();

        String key;
        Object mfCandidates;
        for(Iterator e = var25.iterator(); e.hasNext(); var24.put(key, mfCandidates)) {
            key = (String)e.next();
            PriorityBlockingQueue queue = (PriorityBlockingQueue)explanationsMap.get(key);
            mfCandidates = new ArrayList(queue);
            Collections.sort((List)mfCandidates);
            if(((List)mfCandidates).size() > maxCandidates) {
                mfCandidates = ((List)mfCandidates).subList(0, maxCandidates);
            }
        }

        return var24;
    }


    /**
     *
     * @param stepsize up to stepsize many {@link Reaction}s can be combined to a new {@link Reaction}
     * @return
     */
    public static Reaction[] parseReactions(int stepsize){
        Set<Reaction> reactionsSet = new HashSet<>();
        for (int i = 0; i < reactionStringsMyCompoundID.length; i++) {
            reactionsSet.add(parseReactionString(reactionStringsMyCompoundID[i]));
        }
        for (int i = 0; i < reactionStringsRogers.length; i++) {
            reactionsSet.add(parseReactionString(reactionStringsRogers[i]));
        }

        System.out.println("reactions");
        for (Reaction reaction : reactionsSet) {
            System.out.println(reaction);
        }
        System.out.println(".............");

        if (stepsize>1){

            //convert to combined reaction for equal testing
            Set<Reaction> reactionsSetCombined = new HashSet<>();
            for (Reaction reaction : reactionsSet) {
                reactionsSetCombined.add(new CombinedReaction(reaction));
            }

            Set<Reaction> reactionsSetAll = new HashSet<>(reactionsSetCombined);
            Set<Reaction> reactionsSet1 = new HashSet<>(reactionsSetCombined);
            Set<Reaction> singleReactionsSet = new HashSet<>(reactionsSetCombined);
            int i = 1;
            while (i++<stepsize){
                System.out.println("step: "+i);
                Set<Reaction> reactionSet2 = new HashSet<>();
                for (Reaction reaction1 : reactionsSet1) {
                    for (Reaction reaction2 : singleReactionsSet) {
                        CombinedReaction combinedReaction = new CombinedReaction(reaction1, reaction2);
                        if (combinedReaction==null || combinedReaction.netChange()==null || combinedReaction.netChange().equals(MolecularFormula.emptyFormula())){
                            System.out.println("failed for: ");
                            System.out.println(reaction1);
                            System.out.println(reaction2);
                            System.out.println(combinedReaction);
                            System.out.println();
                        }

                        //don't use empty or negative reactions (duplicates)
                        if (!combinedReaction.netChange().equals(MolecularFormula.emptyFormula()) && combinedReaction.netChange().getMass()>0){
                            reactionSet2.add(combinedReaction);
                        }
                        combinedReaction = new CombinedReaction(reaction1, reaction2.negate());
                        if (combinedReaction==null || combinedReaction.netChange()==null || combinedReaction.netChange().equals(MolecularFormula.emptyFormula())){
                            System.out.println("failed for: ");
                            System.out.println(reaction1);
                            System.out.println(reaction2);
                            System.out.println(combinedReaction);
                            System.out.println();
                        }
                        //don't use empty or negative reactions (duplicates)
                        if (!combinedReaction.netChange().equals(MolecularFormula.emptyFormula()) && combinedReaction.netChange().getMass()>0){
                            reactionSet2.add(combinedReaction);
                        }
                    }
                }

                //todo does not remove dublicates!!
                reactionsSet1 = new HashSet<>();
                int duplicate = 0;
                for (Reaction reaction : reactionSet2) {
                    if (!reactionsSetAll.contains(reaction)) reactionsSet1.add(reaction);
                    else {
                        duplicate++;
                    }
                }
                System.out.println("duplicate "+duplicate+ " new "+reactionsSet1.size());
                reactionsSetAll.addAll(reactionsSet1);
            }
            reactionsSet = reactionsSetAll;
        }
        Reaction[] reactions = reactionsSet.toArray(new Reaction[0]);
        Set<MolecularFormula> formulas = new HashSet<>();
        for (Reaction reaction : reactions) {
            formulas.add(reaction.netChange());
        }
        System.out.println("formulas: "+formulas.size());
        System.out.println("reactions: "+reactions.length);
        return reactions;

    }

    private void computeNet(int iterationSteps, int burnInIterations, double edgeWeight, int multipleTransformationCount) throws IOException {
        BufferedReader reader = Files.newBufferedReader(formulaFile);

        String[] header = reader.readLine().split(SEP);

//        Arrays.stream(header).filter(s -> s.toLowerCase().equals("score")).findFirst().
        int scoreIdx = IntStream.range(0, header.length).filter(i -> header[i].equals("score")).findFirst().getAsInt();
        int formulaIdx = IntStream.range(0, header.length).filter(i -> header[i].contains("formula")).findFirst().getAsInt();
        Map<String, List<MFCandidate>> explanationsMap = new HashMap<>();

        String line;
        while ((line=reader.readLine())!=null){
            try {
                String[] row = line.split(SEP);
                final String id = row[0];
                if (!explanationsMap.containsKey(id)){
                    explanationsMap.put(id, new ArrayList<>());
                }
                MolecularFormula mf = MolecularFormula.parse(row[formulaIdx]);
                double score = transformScore(Double.parseDouble(row[scoreIdx]));

                if (score<0) continue;

                explanationsMap.get(id).add(new MFCandidate(mf, score));
            } catch (Exception e){
                e.printStackTrace();
                System.out.println("line: " + line);
                System.exit(0);
            }


        }

        computeNet(explanationsMap, iterationSteps, burnInIterations, edgeWeight, multipleTransformationCount);
    }

    /**
     * remove formulas with negative score!!!!!!
     * @throws IOException
     */
    private void computeNet(Map<String, List<MFCandidate>> explanationsMap, int iterationSteps, int burnInIterations, double edgeWeight, int multipleTransformationCount) throws IOException {
        //todo Use weight of formulas as (directed) edge weight?????


        //todo use tree scores als "log logodds" and add weights or normalize sum(scores)=1 und use them as probabilities and multiply them


        int workerCount = Runtime.getRuntime().availableProcessors();
        //Zloty
        if (Runtime.getRuntime().availableProcessors()>20){
            workerCount /= 2;
        }


        String[] ids = explanationsMap.keySet().stream().filter(key -> explanationsMap.get(key).size()>0).toArray(s -> new String[s]);
        MFCandidate[][] explanations = new MFCandidate[ids.length][];

        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            explanations[i] = explanationsMap.get(id).toArray(new MFCandidate[0]);
        }

        Reaction[] reactions = parseReactions(multipleTransformationCount);


        System.out.println("reactions: "+reactions.length);
//        for (Reaction reaction : reactions) {
//            System.out.println(reaction);
//        }

        EdgeScorer[] edgeScorers = new EdgeScorer[]{new ReactionScorer(reactions, new ReactionStepSizeScorer.ConstantReactionStepSizeScorer(edgeWeight))};
        NodeScorer[] nodeScorers = new NodeScorer[]{new StandardNodeScorer(false, 1d)};
        GibbsMFCorrectionNetwork gibbsMFCorrectionNetwork = new GibbsMFCorrectionNetwork(ids, explanations, nodeScorers, edgeScorers, new EdgeThresholdFilter(edgeWeight), workerCount);


        TIntIntHashMap map = new TIntIntHashMap();
        for (int i = 0; i < explanations.length; i++) {
            int length = explanations[i].length;
            map.adjustOrPutValue(length, 1, 1);
        }
        map.forEachEntry(new TIntIntProcedure() {
            @Override
            public boolean execute(int i, int i1) {
                System.out.println(i+": "+i1);
                return true;
            }
        });


        writeMFNetwork(Paths.get("/home/ge28quv/Data/gibbsSample/gnpsData/computedTrees/mfNetwork.csv"), gibbsMFCorrectionNetwork);
        if (true) return;

        gibbsMFCorrectionNetwork.iteration(iterationSteps, burnInIterations);

        Scored<MFCandidate>[][] result = gibbsMFCorrectionNetwork.getChosenFormulas();



        //"probability/ relative score" tree score
        Scored<MFCandidate>[][]initialExplanations = getBestInitialAssignments(ids, explanationsMap);

        Scored<MFCandidate>[][] samplingMFs = gibbsMFCorrectionNetwork.getChosenFormulasBySampling();
        Scored<MFCandidate>[][] addedPosteriorProbsMFs = gibbsMFCorrectionNetwork.getChosenFormulasByAddedUpPosterior();
        Scored<MFCandidate>[][] maxPosteriorProbMFs = gibbsMFCorrectionNetwork.getChosenFormulasByMaxPosterior();


        writeOutput(outputFile, ids, new Scored[][][]{initialExplanations, samplingMFs, addedPosteriorProbsMFs, maxPosteriorProbMFs},
                new String[]{"initial", "sampling", "addedPosterior", "maxPosterior"}, SEP);


    }

    private void writeOutput(Path path, String[] ids, Scored<MFCandidate>[][][] formulaCandidates, String[] methodNames, String sep) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path);
        final int length = formulaCandidates[0].length;
        for (int i = 0; i < formulaCandidates.length; i++) {
            Scored<MFCandidate>[][] formulaA = formulaCandidates[i];
            if (formulaA.length != length) throw new RuntimeException("array lengths differ");
        }

        //header
        writer.write("id"+SEP);
        writer.write(Arrays.stream(methodNames).map(name -> new String[]{name + "MF", name + "Score"}).
                flatMap(Arrays::stream).collect(Collectors.joining(sep)));

        for (int i = 0; i < length; i++) {
            final int idx = i;
            String row = Arrays.stream(formulaCandidates).map(f ->
                    new String[]{f[idx][0].getCandidate().getFormula().toString(), Double.toString(f[idx][0].getScore())}).
                    flatMap(Arrays::stream).collect(Collectors.joining(sep));
            writer.write("\n"+ids[idx]+SEP+row);
        }
        writer.close();
    }

    /**
     *
     * @param ids
     * @param explanationsMap
     * @return best initial formulas with RELATIVE SCORE!!
     */
    private Scored<MFCandidate>[][] getBestInitialAssignments(String[] ids, Map<String, List<MFCandidate>> explanationsMap){
        Scored<MFCandidate>[][] array = new Scored[ids.length][];
        for (int i = 0; i < ids.length; i++) {
            List<MFCandidate> smfList = explanationsMap.get(ids[i]);
            Scored<MFCandidate>[] smfarray = new Scored[smfList.size()];
            double sum = 0d;
            for (MFCandidate mfCandidateScored : smfList) {
                sum += mfCandidateScored.getScore();
            }
            for (int j = 0; j < smfarray.length; j++) {
                smfarray[j] = new Scored<MFCandidate>(smfList.get(j), smfList.get(j).getScore()/sum);
            }
            Arrays.sort(smfarray);
            array[i] = smfarray;
        }
        return array;
    }

    private Scored<MFCandidate>[][] oldVsNewFormulaAssignment(String[] ids, Map<String, List<Scored<MFCandidate>>> explanationsMap, Scored<MFCandidate>[] newAssignments){
        Scored<MFCandidate>[][] array = new Scored[ids.length][];
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            List<Scored<MFCandidate>> smfList = explanationsMap.get(id);
            Scored<MFCandidate> best = smfList.stream().max((s1, s2) -> Double.compare(s1.getScore(), s2.getScore())).get();
            Scored<MFCandidate> newA = newAssignments[i];
            array[i] = new Scored[]{best, newA};
        }
        return array;
    }

    public void writeMFNetwork(Path outputPath, GibbsMFCorrectionNetwork gibbsMFCorrectionNetwork) throws IOException {
        //        //network connecting all formulas
        BufferedWriter bw = Files.newBufferedWriter(outputPath);
        bw.write(Arrays.stream(new String[]{"MF1","Label1","MF2","Label2", "Score1", "Score2", "Rank1", "Rank2"}).collect(Collectors.joining(SEP)).toString());
        int[][] edges = gibbsMFCorrectionNetwork.getAllEdgesIndices();
        String[] ids = gibbsMFCorrectionNetwork.getIds();
        Scored<MFCandidate>[][] explanations = gibbsMFCorrectionNetwork.getAllPossibleMolecularFormulas();
        for (int i = 0; i < edges.length; i++) {
            int[] e = edges[i];
            String peakID1 = ids[e[0]];
            Scored<MFCandidate> candidate1 = explanations[e[0]][e[1]];
            String peakID2 = ids[e[2]];
            Scored<MFCandidate> candidate2 = explanations[e[2]][e[3]];
            StringJoiner joiner = new StringJoiner(SEP);
            joiner.add(e[0]+"--"+candidate1.getCandidate().getFormula().toString());
            joiner.add(peakID1);
            joiner.add(e[2]+"--"+candidate2.getCandidate().getFormula().toString());
            joiner.add(peakID2);
            //scores
            joiner.add(String.valueOf(candidate1.getScore()));
            joiner.add(String.valueOf(candidate2.getScore()));
            //ranks
            joiner.add(String.valueOf(e[1]));
            joiner.add(String.valueOf(e[3]));
            bw.write("\n"+joiner.toString());
        }
        bw.close();
    }


    public void writeMFNetworkToDir(Path outputDir, Graph graph) throws IOException {
        if (!Files.exists(outputDir)) Files.createDirectory(outputDir);

        BufferedWriter edgeWriter = Files.newBufferedWriter(outputDir.resolve("edges.csv"));
        BufferedWriter nodesWriter = Files.newBufferedWriter(outputDir.resolve("nodesInfo.csv"));

        EdgeScorer[] edgeScorers = graph.getUsedEdgeScorers();

        List<String> header = new ArrayList<>(Arrays.asList("Source","Target", "treeSizeSource", "treeSizeTarget", "weight"));
        for (EdgeScorer edgeScorer : edgeScorers) {
            header.add(edgeScorer.getClass().getSimpleName());
        }
        edgeWriter.write(header.stream().collect(Collectors.joining(SEP)).toString());

        int[][] edges = graph.getAllEdgesIndices();
        String[] ids = graph.getIds();
        Scored<MFCandidate>[][] explanations = graph.getPossibleFormulas();
        MFCandidate[][] candidates = parseCandiatesArray(explanations);
//        MFCandidate[] candidates1D = parseCandiatesArray(explanations);
        for (EdgeScorer edgeScorer : edgeScorers) edgeScorer.prepare(candidates);

        double[] edgeWeights = graph.getAllEdgesWeights();
        for (int i = 0; i < edges.length; i++) {
            int[] e = edges[i];
            String peakID1 = ids[e[0]];
            Scored<MFCandidate> candidate1 = explanations[e[0]][e[1]];
            String peakID2 = ids[e[2]];
            Scored<MFCandidate> candidate2 = explanations[e[2]][e[3]];

            double weight = edgeWeights[i];


            StringJoiner joinerEdgeInfo = new StringJoiner(SEP);
            joinerEdgeInfo.add(e[0]+"--"+candidate1.getCandidate().getFormula().formatByHill());
            joinerEdgeInfo.add(e[2]+"--"+candidate2.getCandidate().getFormula().formatByHill());

            joinerEdgeInfo.add(String.valueOf(candidate1.getCandidate().getTree().numberOfVertices()));
            joinerEdgeInfo.add(String.valueOf(candidate2.getCandidate().getTree().numberOfVertices()));

            joinerEdgeInfo.add(String.valueOf(weight));
            for (EdgeScorer edgeScorer : edgeScorers) {
                joinerEdgeInfo.add(String.valueOf(edgeScorer.score(candidate1.getCandidate(), candidate2.getCandidate())));
            }

            edgeWriter.newLine();
            edgeWriter.write(joinerEdgeInfo.toString());
            edgeWriter.flush();
        }

        nodesWriter.write(Arrays.stream(new String[]{"Id", "Label", "mass", "score", "rank", "correct", "known"}).collect(Collectors.joining(SEP)).toString());
        for (int i = 0; i < explanations.length; i++) {
            Scored<MFCandidate>[] candidates1D = explanations[i];
            for (int j = 0; j < candidates.length; j++) {
                Scored<MFCandidate> candidate = candidates1D[j];
                StringJoiner joinerNodeInfo = new StringJoiner(SEP);
                joinerNodeInfo.add(i+"--"+candidate.getCandidate().getFormula().formatByHill());
                joinerNodeInfo.add(String.valueOf(i));
                joinerNodeInfo.add(String.valueOf(candidate.getCandidate().getExperiment().getIonMass()));
                joinerNodeInfo.add(String.valueOf(candidate.getScore()));
                joinerNodeInfo.add(String.valueOf(String.valueOf(j)));

                joinerNodeInfo.add(String.valueOf(candidate.getCandidate().isCorrect));
                joinerNodeInfo.add(String.valueOf(candidate.getCandidate().inTrainingSet));

                nodesWriter.newLine();
                nodesWriter.write(joinerNodeInfo.toString());
            }
            nodesWriter.flush();
        }

        for (EdgeScorer edgeScorer : edgeScorers) edgeScorer.clean();

        edgeWriter.close();
        nodesWriter.close();
    }

    private MFCandidate[] parseCandiatesArray1D(Scored<MFCandidate>[][] scoredCandidates) {
        int length = 0;
        for (Scored<MFCandidate>[] sc : scoredCandidates)
            length += sc.length;

        MFCandidate[] candidates = new MFCandidate[length];

        int i = 0;
        for (Scored<MFCandidate>[] sc : scoredCandidates) {
            for (Scored<MFCandidate> scoredCandidate : sc) {
                candidates[i++] = scoredCandidate.getCandidate();
            }
        }

        return candidates;
    }

    private MFCandidate[][] parseCandiatesArray(Scored<MFCandidate>[][] scoredCandidates) {
        final MFCandidate[][] candidates = new MFCandidate[scoredCandidates.length][];
        for (int i = 0; i < candidates.length; i++) {
            final Scored<MFCandidate>[] scored = scoredCandidates[i];
            candidates[i] = new MFCandidate[scored.length];
            for (int j = 0; j < scored.length; j++) {
                candidates[i][j] = scored[j].getCandidate();
            }
        }

        return candidates;
    }

    private double transformScore(double score){
        return Math.max(0, score);
    }


    private static void test(){
//        String[] reactionStrings = new String[]{"H2O"};

        PeriodicTable table = PeriodicTable.getInstance().getInstance();
        Reaction[] reactions = new Reaction[reactionStringsMyCompoundID.length];
        for (int i = 0; i < reactionStringsMyCompoundID.length; i++) {
            reactions[i] = parseReactionString(reactionStringsMyCompoundID[i]);
//            reactions[i] = new SimpleReaction(MolecularFormula.parse(simpleReactionsString[i]));
//            reactions[i] = new Transformation(MolecularFormula.parse(reactionStringsMyCompoundID[i]), MolecularFormula.emptyFormula());
//            reactions[i] = new SimpleReaction(PeriodicTable.getInstance().ionByName(simpleReactionsString[i]));
        }


        String[] compounds = new String[]{"C6H10O", "C6H12O2", "C6H14O3", "C2H3NO"};


        MolecularFormula[] molecularFormulas = new MolecularFormula[compounds.length];

        for (int i = 0; i < molecularFormulas.length; i++) {
            molecularFormulas[i] = MolecularFormula.parse(compounds[i]);
        }

        Deviation deviation = new Deviation(20);
        MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer();

        MolecularFormula[][] candidates = new MolecularFormula[compounds.length][];

        for (int i = 0; i < molecularFormulas.length; i++) {
            MolecularFormula molecularFormula = molecularFormulas[i];
            final double mass = molecularFormula.getMass();
            List<MolecularFormula> formulaList = decomposer.decomposeToFormulas(mass, deviation);

            List<MolecularFormula> formulaList2 = new ArrayList<>();

            for (MolecularFormula formula : formulaList) {
                if (formula.equals(molecularFormula)){
                    formulaList2.add(molecularFormula);
                    continue;
                }
                boolean remove = false;
                for (int j = 0; j < i; j++) {
//                    if (j==i) continue;
                    MolecularFormula[] cand = candidates[j];
                    for (int k = 0; k < cand.length; k++) {
                        MolecularFormula mf = cand[k];
                        for (Reaction reaction : reactions) {
                            if (mf.numberOfCarbons()==formula.numberOfCarbons() &&
                                    Math.abs(mf.numberOfHydrogens()-formula.numberOfHydrogens())==2 &&
                                    Math.abs(mf.numberOfOxygens()-formula.numberOfOxygens())==1
                                    ){
                                System.out.println(mf+" | "+formula);
                            }

                            if (reaction.hasReactionAnyDirection(mf, formula)){
                                System.out.println("removed");
                                remove = true; break;

                            }
                        }
                        if (remove) break;
                    }
                    if (remove) break;

                }
                if (!remove) formulaList2.add(formula);
            }

            formulaList = formulaList2;

//            ... remove everything, which is connected just by chance.

            candidates[i] = formulaList.toArray(new MolecularFormula[0]);
            System.out.println(Arrays.toString(candidates[i]));
        }


        for (int i = 0; i < candidates.length; i++) {
            System.out.println(Arrays.toString(candidates[i]));

        }

        MFCandidate[][] mfCandidates = new MFCandidate[compounds.length][];

        for (int i = 0; i < candidates.length; i++) {
            MolecularFormula[] candidateArray = candidates[i];
            MFCandidate[] scoredCandidateArray = new MFCandidate[candidateArray.length];
            for (int j = 0; j < candidateArray.length; j++) {
                MolecularFormula mf = candidateArray[j];
                MFCandidate scoredMolecularFormula = new MFCandidate(mf, 0.1+Math.random()*0.01);
                scoredCandidateArray[j] = scoredMolecularFormula;
            }
            mfCandidates[i] = scoredCandidateArray;
        }

        GibbsMFCorrectionNetwork gibbsMFCorrectionNetwork = new GibbsMFCorrectionNetwork(compounds, mfCandidates, reactions);

        gibbsMFCorrectionNetwork.iteration(10000,1000);

        Scored<MFCandidate>[][] result = gibbsMFCorrectionNetwork.getChosenFormulas();

        for (int i = 0; i < result.length; i++) {
            Scored<MFCandidate> scoredCandidate = result[i][0];
            System.out.println(scoredCandidate.getCandidate().getFormula()+" -- "+scoredCandidate.getScore());
        }


    }


    private static TCharSet forbidden = new TCharHashSet(new char[]{' ',':','\\','/','[',']', '_'});
    private String cleanString(String s){
        final StringBuilder builder = new StringBuilder(s.length());
        final char[] chars = s.toCharArray();
        for (char c : chars) {
            if (!forbidden.contains(c)) builder.append(c);
        }
        return builder.toString();
    }
}
