package de.unijena.bioinf.GibbsSampling;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.ChemistryBase.ms.ft.UnregardedCandidatesUpperBound;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.*;
import de.unijena.bioinf.GibbsSampling.model.scorer.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.json.FTJsonReader;
//import de.unijena.bioinf.fingerid.SpectralPreprocessor;
import de.unijena.bioinf.sirius.Ms2DatasetPreprocessor;
import de.unijena.bioinf.sirius.Sirius;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;
import org.apache.commons.math3.analysis.function.Exp;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    private static boolean is2Phase;

    private static ScoreProbabilityDistribution probabilityDistribution;

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

            is2Phase = opts.isTwoPhase();

            byte stepsize = 2;
//            Reaction[] reactions = parseReactions(stepsize);
//            new ReactionScorer(reactions, new ExponentialReactionStepSizeScorer(4.0D));
            EdgeScorer[] edgeScorers;
            if(opts.getPCPScoreFile() != null) {
                probabilityDistribution = readPCP(opts.getPCPScoreFile());
                CommonFragmentAndLossScorer commonFragmentAndLossScorer;
                if (false) {
                    commonFragmentAndLossScorer = new CommonFragmentAndLossWithTreeScoresScorer(opts.getThresholdFilter());
                } else {
                    commonFragmentAndLossScorer = new CommonFragmentAndLossScorer(opts.getThresholdFilter());
                }

                ScoreProbabilityDistributionEstimator scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionEstimator(commonFragmentAndLossScorer, probabilityDistribution);
                edgeScorers = new EdgeScorer[]{scoreProbabilityDistributionEstimator};
            } else {
                if(opts.getProbabilityDistribution().toLowerCase().equals("exponential")) {
                    probabilityDistribution = new ExponentialDistribution(0.0D, opts.getThresholdFilter(), opts.isMedian());
                } else if(opts.getProbabilityDistribution().toLowerCase().equals("pareto")) {
                    probabilityDistribution = new ParetoDistribution(opts.getThresholdFilter(), opts.isMedian());
                } else {
                    if(!opts.getProbabilityDistribution().toLowerCase().equals("lognormal") && !opts.getProbabilityDistribution().toLowerCase().equals("log-normal")) {
                        System.out.println("unkown distribution function");
                        return;
                    }

                    probabilityDistribution = new LogNormalDistribution(opts.getThresholdFilter(), opts.isMedian());
                }

                //todo changed !!!?!?!??!?!?!
                double minimumOverlap = 0.01D; //changed from 0.1

                CommonFragmentAndLossScorer commonFragmentAndLossScorer;
                if (false) {
                    commonFragmentAndLossScorer = new CommonFragmentAndLossWithTreeScoresScorer(minimumOverlap);
                } else {
                    commonFragmentAndLossScorer = new CommonFragmentAndLossScorer(minimumOverlap);
                }

                EdgeScorer scoreProbabilityDistributionEstimator;
                if (opts.getLambda()<0){
                    scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionEstimator(commonFragmentAndLossScorer, probabilityDistribution);
                } else {
                    ((ExponentialDistribution)probabilityDistribution).setLambda(opts.getLambda());
                    scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionFix(commonFragmentAndLossScorer, probabilityDistribution);
                }

                edgeScorers = new EdgeScorer[]{scoreProbabilityDistributionEstimator};
            }

            if (opts.isMakeStats()) {
                main.makeStats(treeDir, mgfFile, libraryHits, outputFile, edgeScorers);
            } else if(opts.isSampleScores()) {
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
        List<String> lines = Files.readAllLines(path);
        String header = (String)lines.remove(0);
        if(!header.equals("score\tpcp")) {
            throw new RuntimeException("incorrect pcp header");
        } else {

            for (String l : lines) {
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
        Map<String, List<FragmentsCandidate>> candidatesMap = this.parseMFCandidates(treeDir, mgfFile, maxCandidates, workerCount);
        PrecursorIonType[] ionTypes = (PrecursorIonType[])Arrays.stream(new String[]{"[M+H]+", "[M]+", "[M+K]+", "[M+Na]+"}).map((s) -> {
            return PrecursorIonType.getPrecursorIonType(s);
        }).toArray((l) -> {
            return new PrecursorIonType[l];
        });

        //do before all that
//        this.guessIonizationAndRemove(candidatesMap, ionTypes);


        this.parseLibraryHits(libraryHitsPath, candidatesMap);
        Map correctHits = this.identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);
        double useFreq = 0.0D;
        this.extractEvaluationIds(candidatesMap, correctHits, useFreq, netSingleReactionDiffs);
        String[] ids = (String[])candidatesMap.keySet().stream().filter((key) -> {
            return ((List)candidatesMap.get(key)).size() > 0;
        }).toArray((s) -> {
            return new String[s];
        });
        FragmentsCandidate[][] candidatesArray = new FragmentsCandidate[ids.length][];

        for(int i = 0; i < ids.length; ++i) {
            String id = ids[i];
            candidatesArray[i] = candidatesMap.get(id).toArray(new FragmentsCandidate[0]);
        }

        CommonFragmentAndLossScorer commonFragmentAndLossScorer = new CommonFragmentAndLossScorer(0.0D);
        CommonFragmentScorer commonFragmentScorer = new CommonFragmentScorer(1.0D);
        CommonRootLossScorer commonLossScorer = new CommonRootLossScorer();
        commonFragmentAndLossScorer.prepare(candidatesArray);
        commonFragmentScorer.prepare(candidatesArray);
        commonLossScorer.prepare(candidatesArray);
        int numberOfCandidates = 0;

        for(int i = 0; i < candidatesArray.length; ++i) {
            Candidate[] candidates = candidatesArray[i];
            numberOfCandidates += candidates.length;
        }

        long numberOfEdgesBound = (long)(numberOfCandidates * (numberOfCandidates - maxCandidates));
        System.out.println("numberOfEdgesBound " + numberOfEdgesBound);
        double samplingProb = 1000000.0D / (double)numberOfEdgesBound;
        Path outpath = Paths.get("sampled_scoresAndMatches.csv", new String[0]);
        HighQualityRandom rando = new HighQualityRandom();
        BufferedWriter writer = Files.newBufferedWriter(outpath, new OpenOption[0]);

        writer.write("MF1\tMF2\ttreeSize1\ttreeSize2\tmass1\tmass2\tcommonF\tcommonL\tCommonFragmentAndLossScorer");

        for(int i = 0; i < candidatesArray.length; ++i) {
            FragmentsCandidate[] candidates1 = candidatesArray[i];

            for(int j = i + 1; j < candidatesArray.length; ++j) {
                FragmentsCandidate[] candidates2 = candidatesArray[j];

                for(int k = 0; k < candidates1.length; ++k) {
                    FragmentsCandidate c1 = candidates1[k];

                    for(int l = 0; l < candidates2.length; ++l) {
                        FragmentsCandidate c2 = candidates2[l];
                        if(rando.nextDouble() < samplingProb) {
                            writer.write("\n" + c1.getFormula().formatByHill() + "\t" + c2.getFormula().formatByHill());
                            int treesize1 = c1.getFragments().length;
                            int treesize2 = c2.getFragments().length;
                            writer.write("\t" + treesize1 + "\t" + treesize2);
                            writer.write("\t" + c1.getFormula().getMass() + "\t" + c2.getFormula().getMass());
                            int commonF = commonFragmentScorer.getNumberOfCommon(c1, c2);
                            int commonL = commonLossScorer.getNumberOfCommon(c1, c2);
                            writer.write("\t" + commonF + "\t" + commonL);
                            double score = commonFragmentAndLossScorer.score(c1, c2);
                            writer.write("\t" + String.valueOf(score));
                        }
                    }
                }
            }
        }
        writer.close();

    }

    protected void makeStats(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers) throws IOException {

        int workerCount = Runtime.getRuntime().availableProcessors();
//            workerCount = 6;
        //Zloty
        if (Runtime.getRuntime().availableProcessors()>20){
            workerCount /= 2;
        }

        Set<MolecularFormula> netSingleReactionDiffs = Arrays.stream(parseReactions(1)).map(r -> r.netChange()).collect(Collectors.toSet());

        Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidates(treeDir, mgfFile, maxCandidates, workerCount);


        //do before all that
//        PrecursorIonType[] ionTypes = Arrays.stream(new String[]{"[M+H]+", "[M]+", "[M+K]+", "[M+Na]+"}).map(s -> PrecursorIonType.getPrecursorIonType(s)).toArray(l -> new PrecursorIonType[l]);
//        guessIonizationAndRemove(candidatesMap, ionTypes);


        parseLibraryHits(libraryHitsPath, candidatesMap); //changed
//        parseLibraryHits(libraryHitsPath, mgfFile, candidatesMap);

        Map<String, LibraryHit> correctHits = identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);





        Deviation deviation = new Deviation(20);
        for (String id : candidatesMap.keySet()) {
            final List<FragmentsCandidate> candidateList = candidatesMap.get(id);
            if (!candidateList.get(0).hasLibraryHit()) continue;

            final LibraryHit libraryHit = candidateList.get(0).getLibraryHit();

            // || sharedPeaksRatio<0.5 //todo use sharedPeaksRatio
            //at least cosine 0.8 and 10 shared peaks
//            if (libraryHit.getQuality()!=LibraryHitQuality.Gold || libraryHit.getCosine()<0.8 || libraryHit.getSharedPeaks()<10) continue;

            System.out.println("using lower threshold for references: Bronze, cos 0.66, shared 5 peaks");
            if (libraryHit.getCosine()<0.66 || libraryHit.getSharedPeaks()<5) continue;

            final double theoreticalMass = libraryHit.getIonType().neutralMassToPrecursorMass(libraryHit.getMolecularFormula().getMass());
            final double measuredMass = libraryHit.getQueryExperiment().getIonMass();

            //todo compare without aduct!?
            PrecursorIonType hitIonization = libraryHit.getIonType().withoutAdduct().withoutInsource();

            boolean sameIonization = candidateList.stream().anyMatch(c -> c.getIonType().equals(hitIonization));
            //same mass?
            boolean matches = deviation.inErrorWindow(theoreticalMass, measuredMass);
            //changed matches if one with correct MF
//            boolean matches = candidateList.stream().anyMatch(c -> c.getFormula().equals(libraryHit.getMolecularFormula()));
            //changed --> disadvantage: just 'correct' if Sirius has found it as well
            if (!matches){
                //any know MF diff?
                for (FragmentsCandidate candidate : candidateList) {
                    if (!candidate.getIonType().equals(libraryHit.getIonType())) continue;
                    MolecularFormula mFDiff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                    if (mFDiff.getMass()<0) mFDiff = mFDiff.negate();
                    if (netSingleReactionDiffs.contains(mFDiff)){
                        matches = true;
                        break;
                    }
                }
            }

            if (matches){
                if (sameIonization){
                    correctHits.put(id, libraryHit);
                    System.out.println("all good for "+id+" "+libraryHit.getMolecularFormula());
                } else {
                    System.out.println("warning: different ionizations for library hit "+id);
                }
            } else {
                double closestMassAbs = Math.abs(theoreticalMass-measuredMass);
                double closestMassRel = Math.abs(theoreticalMass-measuredMass);
                double closestMassRelPPM = Math.abs(theoreticalMass-measuredMass)/measuredMass*10e6;

                for (MolecularFormula molecularFormula : netSingleReactionDiffs) {
                    for (FragmentsCandidate candidate : candidateList) {
                        MolecularFormula mFDiff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                        if (mFDiff.getMass()<0) mFDiff = mFDiff.negate();
                        if (molecularFormula.equals(mFDiff)){
                            MolecularFormula mFDiff2 = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                            double shiftedMeasuredMass = measuredMass+mFDiff2.getMass();
                            double currentDiff = Math.abs(shiftedMeasuredMass-theoreticalMass);
                            if (currentDiff<closestMassRel){
                                closestMassRel = currentDiff;
                                closestMassRelPPM = currentDiff/measuredMass*10e6;
                            }
                        }

                    }

                }

                System.out.println("not found: "+id+" "+libraryHit.getMolecularFormula()+" with abs "+closestMassAbs+" ,rel: "+closestMassRel+", ppm:"+closestMassRelPPM+" measured "+measuredMass+" theoretical "+theoreticalMass);

                //any know MF diff?
                for (FragmentsCandidate candidate : candidateList) {
                    if (!candidate.getIonType().equals(libraryHit.getIonType())) continue;
                    MolecularFormula mFDiff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                    if (mFDiff.getMass()<0) mFDiff = mFDiff.negate();
                    if (netSingleReactionDiffs.contains(mFDiff)){
                        matches = true;
                        break;
                    }
                }

            }
        }


    }

    protected void doEvaluation(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers) throws IOException {

        int workerCount = Runtime.getRuntime().availableProcessors();
//            workerCount = 6;
        //Zloty
        if (Runtime.getRuntime().availableProcessors()>20){
            workerCount /= 2;
        }


//        //reactions
//        int stepsize = 2; //low for testing purposes
//        Reaction[] reactions = parseReactions(stepsize);
        //all possible netto changes of MolecularFormulas using on of the reactions.
        Set<MolecularFormula> netSingleReactionDiffs = Arrays.stream(parseReactions(1)).map(r -> r.netChange()).collect(Collectors.toSet());

//        Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidates(treeDir, mgfFile, maxCandidates, workerCount);
        Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidatesEval(treeDir, mgfFile, Integer.MAX_VALUE, workerCount, true); //remove candidates later when adding dummy


        //do before all that
//        PrecursorIonType[] ionTypes = Arrays.stream(new String[]{"[M+H]+", "[M]+", "[M+K]+", "[M+Na]+"}).map(s -> PrecursorIonType.getPrecursorIonType(s)).toArray(l -> new PrecursorIonType[l]);

//        parseLibraryHits(libraryHitsPath, candidatesMap); //changed
        parseLibraryHits(libraryHitsPath, mgfFile, candidatesMap);


        Map<String, LibraryHit> correctHits = identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);


        double useFreq = 0.0; //use x*100 percent of knowledge
        //todo don't use as strict information
        Set<String> evaluationIds = extractEvaluationIds(candidatesMap, correctHits, useFreq, netSingleReactionDiffs);




        System.out.println("adding dummy node");
        addNotExplainableDummy(candidatesMap, maxCandidates);





//        //start Gibbs
//        //todo prior scorers!
//        Map<String, List<Scored<MFCandidate>>> scoredCandidateMap  = getScoredCandidatesByTreeScore(candidatesMap);


        String[] ids = candidatesMap.keySet().stream().filter(key -> candidatesMap.get(key).size()>0).toArray(s -> new String[s]);
        FragmentsCandidate[][] candidatesArray = new FragmentsCandidate[ids.length][];

        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            candidatesArray[i] = candidatesMap.get(id).toArray(new FragmentsCandidate[0]);
        }


        System.out.println("before");
        statisticsOfKnownCompounds(candidatesArray, ids, evaluationIds, correctHits);

        NodeScorer[] nodeScorers;
        if (useLibraryHits){
            nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1), new LibraryHitScorer(libraryScore, 0.5, netSingleReactionDiffs)};
            System.out.println("use LibraryHitScorer");
        } else {
            nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1)};
            System.out.println("ignore Library Hits");
        }



        if (is2Phase){
            TwoPhaseGibbsSampling<FragmentsCandidate> twoPhaseGibbsSampling = new TwoPhaseGibbsSampling<>(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, workerCount, 1);
            System.out.println("start");
            twoPhaseGibbsSampling.run(iterationSteps, burnInIterations);

            Scored<FragmentsCandidate>[][] result = twoPhaseGibbsSampling.getChosenFormulas();
            System.out.println("standard");
            statisticsOfKnownCompounds(result, twoPhaseGibbsSampling.getIds(), evaluationIds, correctHits);
        } else {
            //GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, workerCount, 20);
            //changed
            GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, workerCount, 1);


            if (graphOutputDir!=null) writeMFNetworkToDir(graphOutputDir, gibbsParallel.getGraph());




            System.out.println("start");
            gibbsParallel.iteration(iterationSteps, burnInIterations);

            Scored<FragmentsCandidate>[][] result = gibbsParallel.getChosenFormulasBySampling();
            System.out.println("standard");
            statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);

            result = gibbsParallel.getChosenFormulasByAddedUpPosterior();
            System.out.println("addedPosterior");
            statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);

            result = gibbsParallel.getChosenFormulasByMaxPosterior();
            System.out.println("maxPosterior");
            statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);
        }
    }

    protected void doCVEvaluation(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers) throws IOException {
        System.out.println("do crossval");
        int workerCount = Runtime.getRuntime().availableProcessors();
//            workerCount = 6;
        //Zloty
        if (Runtime.getRuntime().availableProcessors()>20){
            workerCount /= 2;
        }


        //reactions
//        int stepsize = 2; //low for testing purposes
//        Reaction[] reactions = parseReactions(stepsize);
        //all possible netto changes of MolecularFormulas using on of the reactions.
        Set<MolecularFormula> netSingleReactionDiffs = Arrays.stream(parseReactions(1)).map(r -> r.netChange()).collect(Collectors.toSet());

//        Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidates(treeDir, mgfFile, maxCandidates, workerCount);
//
//        parseLibraryHits(libraryHitsPath, candidatesMap);
//
//        Map<String, LibraryHit> correctHits = identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);




        Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidates(treeDir, mgfFile, Integer.MAX_VALUE, workerCount); //remove candidates later when adding dummy
//        parseLibraryHits(libraryHitsPath, candidatesMap); //changed
        parseLibraryHits(libraryHitsPath, mgfFile, candidatesMap);


        Map<String, LibraryHit> correctHits = identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);

        System.out.println("adding dummy node");
        addNotExplainableDummy(candidatesMap, maxCandidates);




        List<String> knownIDs = new ArrayList<>(correctHits.keySet());

        List<String> knownLibraryStructures = correctHits.values().stream().map(libraryHit -> libraryHit.getStructure()).distinct().collect(Collectors.toList());


        int start;
        int end = -1;
        for (int fold = 0; fold < 10; fold++) {
            start = end+1;
//            end = (int)((fold+1)*knownIDs.size()/10.0);
//            if (fold==9) end = knownIDs.size()-1;
            end = (int)((fold+1)*knownLibraryStructures.size()/10.0);
            if (fold==9) end = knownLibraryStructures.size()-1;
            System.out.println("start "+start+" end "+end);

            resetCorrects(candidatesMap);


            //todo don't use as strict information
//            Set<String> evaluationIds = extractEvaluationSpecificIds(candidatesMap, correctHits, netSingleReactionDiffs, knownIDs, start, end);
            Set<String> evaluationIds = extractEvaluationBySpecificStructure(candidatesMap, correctHits, netSingleReactionDiffs, knownLibraryStructures, start, end);


            String[] ids = candidatesMap.keySet().stream().filter(key -> candidatesMap.get(key).size()>0).toArray(s -> new String[s]);
            FragmentsCandidate[][] candidatesArray = new FragmentsCandidate[ids.length][];

            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];
                candidatesArray[i] = candidatesMap.get(id).toArray(new FragmentsCandidate[0]);
            }


            System.out.println("before");
            statisticsOfKnownCompounds(candidatesArray, ids, evaluationIds, correctHits);


//            NodeScorer[] nodeScorers;
//            if (useLibraryHits){
////                nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1), new LibraryHitScorer(libraryScore, 0.5, netSingleReactionDiffs)};
//                nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1), new AllOrNothingLibraryHitsScorer()};
//                System.out.println("use LibraryHitScorer");
//            } else {
//                nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1)};
//                System.out.println("ignore Library Hits");
//            }


            NodeScorer[] nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1), new AllOrNothingLibraryHitsScorer()};
            System.out.println("use LibraryHitScorer");


//            //todo changed. Just a hotfix!!!!
//            double minimumOverlap = 0.1D;
//            ScoreProbabilityDistributionEstimator commonFragmentAndLossScorer = new ScoreProbabilityDistributionEstimator(new CommonFragmentAndLossScorer(minimumOverlap), probabilityDistribution);
//            edgeScorers = new EdgeScorer[]{commonFragmentAndLossScorer};

            GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel<FragmentsCandidate>(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, workerCount, 20);


            System.out.println("start");
            gibbsParallel.iteration(iterationSteps, burnInIterations);

            Scored<FragmentsCandidate>[][] standardresult = gibbsParallel.getChosenFormulasBySampling();
            System.out.println("standard");
            statisticsOfKnownCompounds(standardresult, ids, evaluationIds, correctHits);

            Scored<FragmentsCandidate>[][] result = gibbsParallel.getChosenFormulasByAddedUpPosterior();
            System.out.println("addedPosterior");
            statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);

            result = gibbsParallel.getChosenFormulasByMaxPosterior();
            System.out.println("maxPosterior");
            statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits);


            writeBestFormulas(standardresult, gibbsParallel.getGraph(), outputFile);

        }


    }


    private <C extends HasLibraryHit> void resetCorrects(Map<String, List<C>> candidatesMap){
        for (List<C> candidates : candidatesMap.values()) {
            resetCorrects(candidates);
        }
    }

    private void resetCorrects(Collection<? extends HasLibraryHit> candidates){
        for (HasLibraryHit candidate : candidates) {
            candidate.setInEvaluationSet(false);
            candidate.setInTrainingSet(false);
            candidate.setCorrect(false);
//            candidate.setLibraryHit(null);
        }
    }


    private void writeBestFormulas(Scored<FragmentsCandidate>[][] results, Graph<FragmentsCandidate> graph, Path outputFile) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset(), new OpenOption[0]);
        String SEP = "\t";

        for(int i = 0; i < results.length; ++i) {
            Scored<FragmentsCandidate>[] result = results[i];
            Scored<FragmentsCandidate> best = result[0];
            FragmentsCandidate initalBest = graph.getPossibleFormulas()[i][0].getCandidate(); //todo correctly sorted?
            String id = best.getCandidate().getExperiment().getName();
            String formula = best.getCandidate().getFormula().formatByHill();
            String iniFormula = initalBest.getFormula().formatByHill();
            int numberOfEdges = graph.getConnections(graph.getAbsoluteFormulaIdx(i, 0)).length;
            double score = (double)result.length / best.getScore();
            writer.write(id + SEP + iniFormula + SEP + formula + SEP + score + SEP + numberOfEdges + "\n");
        }

        writer.close();
    }


    private void writeBestFormulas(Scored<FragmentsCandidate>[][] results, FragmentsCandidate[][] initialAssignment, Graph graph, Path outputFile) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset(), new OpenOption[0]);
        String SEP = "\t";

        for(int i = 0; i < results.length; ++i) {
            Scored<FragmentsCandidate>[] result = results[i];
            Scored<FragmentsCandidate> best = result[0];
            FragmentsCandidate initalBest = initialAssignment[i][0];
            String id = best.getCandidate().getExperiment().getName();
            String formula = best.getCandidate().getFormula().formatByHill();
            String iniFormula = initalBest.getFormula().formatByHill();
            int numberOfEdges = graph.getConnections(graph.getAbsoluteFormulaIdx(i, 0)).length;
            double score = (double)result.length / best.getScore();
            writer.write(id + SEP + iniFormula + SEP + formula + SEP + score + SEP + numberOfEdges + "\n");
        }

        writer.close();
    }

    private Set<String> extractEvaluationSpecificIds(Map<String, List<FragmentsCandidate>> candidatesMap, Map<String, LibraryHit> correctHitsMap, Set<MolecularFormula> allowedDifferences, List<String> knownIDs, int start, int end) {
        ArrayList trainIds = new ArrayList(knownIDs.subList(0, start));
        trainIds.addAll(knownIDs.subList(end + 1, knownIDs.size()));
        ArrayList evaluationIds = new ArrayList(knownIDs.subList(start, end + 1));
        System.out.println("used ids: " + Arrays.toString(trainIds.toArray(new String[0])));
        this.setKnownCompounds(trainIds, correctHitsMap, candidatesMap, allowedDifferences, false);
        return this.setKnownCompounds(evaluationIds, correctHitsMap, candidatesMap, allowedDifferences, true);
    }

    private Set<String> extractEvaluationBySpecificStructure(Map<String, List<FragmentsCandidate>> candidatesMap, Map<String, LibraryHit> correctHitsMap, Set<MolecularFormula> allowedDifferences, List<String> knownLibraryStructures, int start, int end) {
        ArrayList trainIds = new ArrayList(knownLibraryStructures.subList(0, start));
        trainIds.addAll(knownLibraryStructures.subList(end + 1, knownLibraryStructures.size()));
        ArrayList evaluationIds = new ArrayList(knownLibraryStructures.subList(start, end + 1));
        System.out.println("used eval structures: " + Arrays.toString(evaluationIds.toArray(new String[0])));
        this.setKnownCompoundsByLibraryStructure(trainIds, correctHitsMap, candidatesMap, allowedDifferences, false);
        return this.setKnownCompoundsByLibraryStructure(evaluationIds, correctHitsMap, candidatesMap, allowedDifferences, true);
    }

    private Set<String> extractEvaluationIds(Map<String, List<FragmentsCandidate>> candidatesMap, Map<String, LibraryHit> correctHitsMap, double useFreq, Set<MolecularFormula> allowedDifferences) {
        ArrayList knownIDs = new ArrayList(correctHitsMap.keySet());
        List trainIds = knownIDs.subList(0, (int)(useFreq * (double)knownIDs.size()));
        List evaluationIds = knownIDs.subList((int)(useFreq * (double)knownIDs.size()), knownIDs.size());
        System.out.println("used ids: " + Arrays.toString(trainIds.toArray(new String[0])));
        this.setKnownCompounds(trainIds, correctHitsMap, candidatesMap, allowedDifferences, false);
        return this.setKnownCompounds(evaluationIds, correctHitsMap, candidatesMap, allowedDifferences, true);
    }

    private Set<String> setKnownCompounds(Collection<String> ids, Map<String, LibraryHit> correctHitsMap, Map<String, List<FragmentsCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences, boolean eval) {
        System.out.println("allowedDifferences "+allowedDifferences.size());
        Set<String> usedIds = new HashSet<>();
        for (String id : ids) {
            LibraryHit libraryHit = correctHitsMap.get(id);
            MolecularFormula correctMF = libraryHit.getMolecularFormula();
            List<FragmentsCandidate> candidates = candidatesMap.get(id);
            if (candidates==null){
                System.out.println();
//                throw new RuntimeException("all candidates have been removed: "+id);
                System.err.println("all candidates have been removed: "+id);
            } else {
                System.out.println("candidates size "+candidates.size()+" for "+id);
//                List<MFCandidate> newCandidates = new ArrayList<>();
                int correctHits = 0;
                for (FragmentsCandidate candidate : candidates) {
                    boolean matches = candidate.getFormula().equals(correctMF);
                    if (!matches){
                        MolecularFormula diff = candidate.getFormula().subtract(correctMF);
                        if (diff.getMass()<0) diff = diff.negate();
                        matches = allowedDifferences.contains(diff);
                    }
                    if (matches){
                        candidate.setCorrect(true);
                        correctHits++;
                    }
                    if (eval){
                        candidate.setInEvaluationSet(true);
                    } else {
                        candidate.setInTrainingSet(true);
                    }
                }
                //if ==0 presumably correct is not in our list.
                assert correctHits<=1;
                if (correctHits>1){
                    throw new RuntimeException("unexpected number of correct hits : "+correctHits);
                }
                if (correctHits==1){
                    usedIds.add(id);
                }
                //todo don't use as strict information
//                candidatesMap.put(id, newCandidates);
            }
        }
        return usedIds;
    }

    private Set<String> setKnownCompoundsByLibraryStructure(Collection<String> structures, Map<String, LibraryHit> correctHitsMap, Map<String, List<FragmentsCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences, boolean eval) {
        Set<String> usedIds = new HashSet<>();
        for (String structure : structures) {
            for (String id : candidatesMap.keySet()) {
                LibraryHit libraryHit = correctHitsMap.get(id);
                if (libraryHit==null || !libraryHit.getStructure().equals(structure)) continue;
                if (usedIds.contains(id)) continue;
                System.out.println("struct "+libraryHit.getStructure());

                MolecularFormula correctMF = libraryHit.getMolecularFormula();
                List<FragmentsCandidate> candidates = candidatesMap.get(id);
                if (candidates==null){
                    System.out.println();
//                throw new RuntimeException("all candidates have been removed: "+id);
                    System.err.println("all candidates have been removed: "+id);
                } else {
                    System.out.println("candidates size "+candidates.size()+" for "+id);
//                List<MFCandidate> newCandidates = new ArrayList<>();
                    int correctHits = 0;
                    for (FragmentsCandidate candidate : candidates) {
                        boolean matches = candidate.getFormula().equals(correctMF);
                        if (!matches){
                            MolecularFormula diff = candidate.getFormula().subtract(correctMF);
                            if (diff.getMass()<0) diff = diff.negate();
                            matches = allowedDifferences.contains(diff);
                        }
                        if (matches){
//                        newCandidates.add(candidate);
                            candidate.isCorrect = true;
                            correctHits++;
                        }
                        if (eval){
                            candidate.inEvaluationSet = true;
                        } else {
                            candidate.inTrainingSet = true;
                        }
                    }
                    //if ==0 presumably correct is not in our list.
                    assert correctHits<=1;
                    if (correctHits>1){
                        throw new RuntimeException("unexpected number of correct hits : "+correctHits);
                    }
                    if (correctHits==1){
                        usedIds.add(id);
                    }
                    //todo don't use as strict information
//                candidatesMap.put(id, newCandidates);
                }
            }

        }
        return usedIds;
    }


    private Map<String, LibraryHit> identifyCorrectLibraryHits(Map<String, List<FragmentsCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences) {
        Map<String, LibraryHit> correctHits = new HashMap<>();
        Deviation deviation = new Deviation(20);

        for (String id : candidatesMap.keySet()) {
            final List<FragmentsCandidate> candidateList = candidatesMap.get(id);
            if (!candidateList.get(0).hasLibraryHit()) continue;

            final LibraryHit libraryHit = candidateList.get(0).getLibraryHit();

            // || sharedPeaksRatio<0.5 //todo use sharedPeaksRatio
            //at least cosine 0.8 and 10 shared peaks
//            if (libraryHit.getQuality()!=LibraryHitQuality.Gold || libraryHit.getCosine()<0.8 || libraryHit.getSharedPeaks()<10) continue;

            if (libraryHit.getCosine()<0.7 || libraryHit.getSharedPeaks()<5) continue;

            final double theoreticalMass = libraryHit.getIonType().neutralMassToPrecursorMass(libraryHit.getMolecularFormula().getMass());
            final double measuredMass = libraryHit.getQueryExperiment().getIonMass();

            //todo compare without aduct!?
            PrecursorIonType hitIonization = libraryHit.getIonType().withoutAdduct().withoutInsource();

            boolean sameIonization = candidateList.stream().anyMatch(c -> c.getIonType().equals(hitIonization));
            //same mass?
            boolean matches = deviation.inErrorWindow(theoreticalMass, measuredMass);
            //changed matches if one with correct MF
//            boolean matches = candidateList.stream().anyMatch(c -> c.getFormula().equals(libraryHit.getMolecularFormula()));
            //changed --> disadvantage: just 'correct' if Sirius has found it as well
            if (!matches){
                //any know MF diff?
                for (FragmentsCandidate candidate : candidateList) {
                    if (!candidate.getIonType().equals(libraryHit.getIonType())) continue;
                    MolecularFormula mFDiff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                    if (mFDiff.getMass()<0) mFDiff = mFDiff.negate();
                    if (allowedDifferences.contains(mFDiff)){
                        matches = true;
                        break;
                    }
                }
            }


            if (matches){
                if (sameIonization){
                    correctHits.put(id, libraryHit);
                } else {
                    System.out.println("warning: different ionizations for library hit "+id);
                }
            } else {
                System.out.println("mass or biotransformations don't match for library hit "+id+". lib mass "+libraryHit.getMolecularFormula().getMass()+" vs measured "+measuredMass);
            }
        }

        return correctHits;
    }

    private <C extends Candidate & HasLibraryHit> void parseLibraryHits(Path libraryHitsPath, Map<String, List<C>> candidatesMap) throws IOException {
        List<String> lines = Files.readAllLines(libraryHitsPath, Charset.defaultCharset());
        String[] header = lines.remove(0).split("\t");
        String[] ofInterest = new String[]{"Feature_id", "Formula", "Structure", "Adduct", "Cosine", "SharedPeaks", "Quality"};
        int[] indices = new int[ofInterest.length];
        for (int i = 0; i < ofInterest.length; i++) {
            int idx = arrayFind(header, ofInterest[i]);
            if (idx<0) throw new RuntimeException("Column "+ofInterest[i]+" not found");
            indices[i] = idx;
        }
        for (String line : lines) {
            String[] cols = line.split("\t");
            final String featureId = cols[indices[0]];

            List<C> candidatesList = candidatesMap.get(featureId);
            if (candidatesList == null) {
                System.out.println("corresponding query (" +featureId+ ") to library hit not found");
                System.err.println("corresponding query (" +featureId+ ") to library hit not found");
//                System.err.println("all candidates have been removed: "+id);
                continue;
            }

            final Ms2Experiment experiment = candidatesList.get(0).getExperiment();
            final MolecularFormula formula = MolecularFormula.parse(cols[indices[1]]);
            final String structure = cols[indices[2]];
            final PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(cols[indices[3]]);
            final double cosine = Double.parseDouble(cols[indices[4]]);
            final int sharedPeaks = Integer.parseInt(cols[indices[5]]);
            final LibraryHitQuality quality = LibraryHitQuality.valueOf(cols[indices[6]]);
            LibraryHit libraryHit = new LibraryHit(experiment, formula, structure, ionType, cosine, sharedPeaks, quality);
            for (C candidate : candidatesList) {
                candidate.setLibraryHit(libraryHit);
            }
        }
    }

    public static  <C extends Candidate & HasLibraryHit> void parseLibraryHits(Path libraryHitsPath, Path mgfFile, Map<String, List<C>> candidatesMap) throws IOException {
        try {
            //todo test missing values !!!!
//            final MsExperimentParser parser = new MsExperimentParser();
//            List<Ms2Experiment> allExperiments = parser.getParser(mgfFile.toFile()).parseFromFile(mgfFile.toFile());

//            //assumption "#Scan#" is the number of the ms2 scan starting with 1
//            String[] featureIDs = new String[allExperiments.size()];
//            int j = 0;
//            for (Ms2Experiment experiment : allExperiments) {
//                featureIDs[j++] = experiment.getName();
//            }

            List<String> featureIDs = new ArrayList<>();
            try(BufferedReader reader = Files.newBufferedReader(mgfFile)){
                String line;
                String lastID = null;
                while ((line=reader.readLine())!=null){
                    if (line.toLowerCase().startsWith("feature_id=")){
                        String id = line.split("=")[1];
                        if (!id.equals(lastID)){
                            featureIDs.add(id);
                            lastID = id;
                        }
                    }
                }
            }

            //todo change nasty hack


            List<String> lines = Files.readAllLines(libraryHitsPath, Charset.defaultCharset());
            String[] header = lines.remove(0).split("\t");
//        String[] ofInterest = new String[]{"Feature_id", "Formula", "Structure", "Adduct", "Cosine", "SharedPeaks", "Quality"};
            String[] ofInterest = new String[]{"#Scan#", "INCHI", "Smiles", "Adduct", "MQScore", "SharedPeaks", "Quality"};
            int[] indices = new int[ofInterest.length];
            for (int i = 0; i < ofInterest.length; i++) {
                int idx = arrayFind(header, ofInterest[i]);
                if (idx<0){
                    int[] more = arrayFindSimilar(header, ofInterest[i]);
                    if (more.length!=1) throw new RuntimeException("Cannot parse spectral library hits file. Column "+ofInterest[i]+" not found.");
                    else idx = more[0];
                }
                indices[i] = idx;
            }
            for (String line : lines) {
                try {
                    String[] cols = line.split("\t");
                    final int scanNumber = Integer.parseInt(cols[indices[0]]);
                    final String featureId = featureIDs.get(scanNumber-1); //starting with 1!

                    List<C> candidatesList = candidatesMap.get(featureId);
                    if (candidatesList == null) {
                        //todo check:q
                    System.err.println("no corresponding compound (FEATURE_ID: " +featureId+ ", #SCAN# "+scanNumber+") to library hit found");
                        continue;
                    }

                    final Ms2Experiment experiment = candidatesList.get(0).getExperiment();
                    final MolecularFormula formula = getFormulaFromStructure(cols[indices[1]].replace("\"", ""), cols[indices[2]].replace("\"", ""));

                    if (formula==null){
                        System.err.println("cannot compute molecular formula of library hit #SCAN# "+scanNumber);
                        continue;
                    }

                    final String structure = (isInchi(cols[indices[1]]) ? cols[indices[1]] : cols[indices[2]]);
                    final PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(cols[indices[3]]);
                    final double cosine = Double.parseDouble(cols[indices[4]]);
                    final int sharedPeaks = Integer.parseInt(cols[indices[5]]);
                    final LibraryHitQuality quality = LibraryHitQuality.valueOf(cols[indices[6]]);
                    LibraryHit libraryHit = new LibraryHit(experiment, formula, structure, ionType, cosine, sharedPeaks, quality);
                    for (C candidate : candidatesList) {
                        candidate.setLibraryHit(libraryHit);
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Cannot parse library hit. Reason: "+ e.getMessage());
                }

            }
        } catch (Exception e){
            throw new IOException("cannot parse library hits. Reason "+e.getMessage());
        }

    }


    private static MolecularFormula getFormulaFromStructure(String inchi, String smiles){

        MolecularFormula formula = null;
        if (inchi!=null && isInchi(inchi)){
            formula = new InChI(null, inchi).extractFormula();
        }

        if (formula==null){
            try {
                final SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
                final IAtomContainer c = parser.parseSmiles(smiles);
                String formulaString = MolecularFormulaManipulator.getString(MolecularFormulaManipulator.getMolecularFormula(c));
                formula = MolecularFormula.parse(formulaString);
            } catch (CDKException e) {
                return null;
            }
        }
        return formula;
    }

    private static boolean isInchi(String inchi) {
        if (!inchi.toLowerCase().startsWith("inchi=")) return false;
        int idx1 = inchi.indexOf("/");
        int idx2 = inchi.indexOf("/", idx1+1);
        if (idx1>0 && idx2>0 && (idx2-idx1)>1) return true;
        return false;
    }


//    public Map<String, List<Scored<Candidate>>> getScoredCandidatesByTreeScore(Map<String, List<Candidate>> candidatesMap) {
//        //convert and remove negative scores
////        System.out.println("just CHO");
////        FormulaConstraints constraints = new FormulaConstraints("CHO");
//        Map<String, List<Scored<Candidate>>> scoredCandidateMap  = new HashMap<>();
//        for (Map.Entry<String, List<Candidate>> entry : candidatesMap.entrySet()) {
//            final String id = entry.getKey();
//            final List<Candidate> list = entry.getValue();
//            final List<Scored<Candidate>> scoredList = new ArrayList<>(list.size());
//            for (Candidate candidate : list) {
//                final double score = candidate.getTree().getAnnotationOrThrow(TreeScoring.class).getOverallScore();
////                if (score>0 && constraints.isSatisfied(candidate.getFormula())) {
//                if (score>0) {
//                    scoredList.add(new Scored<>(candidate, score));
//                }
//
//            }
//            if (scoredList.size()==0){
//                System.out.println("no candidates anymore");
//            } else {
//                Collections.sort(scoredList, Scored.desc());
//                scoredCandidateMap.put(id, scoredList);
//            }
//        }
//        return scoredCandidateMap;
//    }

    public <C extends Candidate>  void guessIonizationAndRemove(Map<String, List<C>> candidateMap, PrecursorIonType[] ionTypes){
        List<String> idList = new ArrayList<>(candidateMap.keySet());
        Sirius sirius = new Sirius();
        for (String id : idList) {
            List<C> candidates = candidateMap.get(id);
            Ms2Experiment experiment = candidates.get(0).getExperiment();
            PrecursorIonType[] guessed = sirius.guessIonization(experiment, ionTypes);

            if (guessed.length==0) continue;

            //todo completely discard?
            PrecursorIonType chosenIonType = experiment.getPrecursorIonType();
            PrecursorIonType chosenIonization = chosenIonType.withoutAdduct().withoutInsource();
            if (!chosenIonization.isIonizationUnknown()){
                if (!arrayContains(guessed, chosenIonization)){
                    System.out.println("warning: guessed ionization ( "+chosenIonType.toString()+" ) contradicts chosen one.");
                    guessed = Arrays.copyOf(guessed, guessed.length+1);
                    guessed[guessed.length-1] = chosenIonization;
                } else{
                    guessed = new PrecursorIonType[]{chosenIonization};
                }
                System.out.println("guessed is known "+Arrays.toString(guessed)+" for "+id);
            } else {
                System.out.println("guessed is unknown "+Arrays.toString(guessed)+" for "+id);
            }

            Iterator<C> iterator = candidates.iterator();
            while (iterator.hasNext()) {
                Candidate next = iterator.next();

                //todo just test once
                if (!((FragmentsCandidate)next).getIonType().equals(next.getAnnotation(PrecursorIonType.class))){
                    throw new RuntimeException("ion types problem");
                }

                if(!this.arrayContains(guessed, next.getAnnotation(PrecursorIonType.class))) {
                    iterator.remove();
                }
                
            }


            if (candidates.size()==0){
                System.out.println("no candidates anymore "+id+" | guessed ionization: "+Arrays.toString(guessed));
                candidateMap.remove(id);
            }

        }
    }

    public static void addNotExplainableDummy(Map<String, List<FragmentsCandidate>> candidateMap, int maxCandidates){
        List<String> idList = new ArrayList<>(candidateMap.keySet());

        for (String id : idList) {
            List<FragmentsCandidate> candidates = candidateMap.get(id);
            Ms2Experiment experiment = candidates.get(0).getExperiment();

            UnregardedCandidatesUpperBound unregardedCandidatesUpperBound = candidates.get(0).getAnnotationOrNull(UnregardedCandidatesUpperBound.class);

            if (unregardedCandidatesUpperBound==null){
                System.err.println("Cannot create dummy node. Information missing.");
                break;
            }

            double worstScore = unregardedCandidatesUpperBound.getLowestConsideredCandidateScore();
            int numberOfIgnored = unregardedCandidatesUpperBound.getNumberOfUnregardedCandidates();

            if (candidates.size()>maxCandidates) {
                numberOfIgnored += candidates.size()-maxCandidates;

                candidates = candidates.subList(0,maxCandidates);
                candidateMap.put(id, candidates);

                worstScore = candidates.get(candidates.size()-1).getScore();
            }

            if (numberOfIgnored>0) {
                FragmentsCandidate dummyCandidate = DummyFragmentCandidate.newDummy(worstScore, numberOfIgnored, experiment);
                candidates.add(dummyCandidate);
            }

        }
    }


    private int[] statisticsOfKnownCompounds(Scored<FragmentsCandidate>[] result, String ids[], Set<String> evaluationIDs, Map<String, MolecularFormula> correctHitsMap){
        List<String> correctIds = new ArrayList<>();
        List<String> wrongIds = new ArrayList<>();
        int correctId = 0;
        int wrongId = 0;
        for (int i = 0; i < result.length; i++) {
            Scored<FragmentsCandidate> candidateScored = result[i];
            String id = ids[i];

            if (correctHitsMap.containsKey(id) && evaluationIDs.contains(id)){
                MolecularFormula correct = correctHitsMap.get(id);
                if (candidateScored.getCandidate().getFormula().equals(correct)) {
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
    
    /**
     * print ranks of compounds
     * @param result sorted!
     * @param ids
     * @param correctHitsMap
     * @return
     */
    private int[] statisticsOfKnownCompounds(Scored<FragmentsCandidate>[][] result, String ids[], Set<String> evaluationIDs, Map<String, LibraryHit> correctHitsMap){
        int bestIsDummyCount = 0;
        int total = 0;
        for (int i = 0; i < result.length; i++) {
            Scored<FragmentsCandidate>[] candidatesScored = result[i];
            if (candidatesScored.length==0) continue;
            ++total;
            if (DummyFragmentCandidate.isDummy(candidatesScored[0].getCandidate())){
                ++bestIsDummyCount;
            }


        }
        System.out.println("used dummies: "+bestIsDummyCount + " of " + total);

        int correctId = 0;
        int wrongId = 0;
        for (int i = 0; i < result.length; i++) {
            Scored<FragmentsCandidate>[] candidatesScored = result[i];
            String id = ids[i];

            if (correctHitsMap.containsKey(id) && evaluationIDs.contains(id)){
//                MolecularFormula correct = correctHitsMap.get(id).getMolecularFormula();
                int pos = 1;
                int correctPos = Integer.MAX_VALUE;
                int dummyPos = Integer.MAX_VALUE;
                for (Scored<FragmentsCandidate> candidateScored : candidatesScored) {
                    if (candidateScored.getCandidate().isCorrect()) {
                        correctPos = pos;
                    } else if (DummyFragmentCandidate.isDummy(candidateScored.getCandidate())){
                        dummyPos = pos;
                    }
                    pos++;
                }
                if (dummyPos==1){
                    System.out.println(id + " best is dummy.");
                }
                if (correctPos>candidatesScored.length){
                    System.out.println(id + " not found | best has "+candidatesScored[0].getScore());
                } else {
                    System.out.println(id + " found at " + correctPos + " (" + candidatesScored[correctPos-1].getScore()+ ") of " + candidatesScored.length+"  | best has "+candidatesScored[0].getScore());
                }
            }
        }
        return new int[]{correctId, wrongId};
    }

    /**
     * print ranks of compounds
     * @param result sorted!
     * @param ids
     * @param correctHitsMap
     * @return
     */
    private int[] statisticsOfKnownCompounds(FragmentsCandidate[][] result, String ids[], Set<String> evaluationIDs, Map<String, LibraryHit> correctHitsMap){
        int correctId = 0;
        int wrongId = 0;
        for (int i = 0; i < result.length; i++) {
            FragmentsCandidate[] candidates = result[i];
            String id = ids[i];

            if (correctHitsMap.containsKey(id) && evaluationIDs.contains(id)){
//                MolecularFormula correct = correctHitsMap.get(id).getMolecularFormula();
                int pos = 1;
                for (FragmentsCandidate candidate : candidates) {
                    if (candidate.isCorrect()) {
                        break;
                    }
                    pos++;
                }
                if (pos>candidates.length){
                    System.out.println(id + " ( "+candidates[0].getExperiment().getIonMass()+" mz) not found");
                } else {
                    System.out.println(id + " ( "+candidates[0].getExperiment().getIonMass()+" mz) found at " + pos + " (" + candidates[pos-1].getScore()+ ") of " + candidates.length);
                }
            }
        }
        return new int[]{correctId, wrongId};
    }

    private <T> boolean arrayContains(T[] array, T object) {
        return this.arrayFind(array, object) >= 0;
    }

    private static <T> int arrayFind(T[] array, T object) {
        for(int i = 0; i < array.length; ++i) {
            Object t = array[i];
            if(t.equals(object)) {
                return i;
            }
        }

        return -1;
    }

    private static int[] arrayFindSimilar(String[] array, String object) {
        TIntArrayList positions = new TIntArrayList();
        for(int i = 0; i < array.length; ++i) {
            String t = array[i];
            if(t.toLowerCase().contains(object.toLowerCase())) {
                positions.add(i);
            }
        }

        return positions.toArray();
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

    public static Map<String, List<FragmentsCandidate>> parseMFCandidates(Path treeDir, Path mgfFile, int maxCandidates, int workercount) throws IOException {
        return parseMFCandidates(treeDir, mgfFile, maxCandidates, workercount, false);
    }


    public static Map<String, List<FragmentsCandidate>> parseMFCandidates(Path treeDir, Path mgfFile, int maxCandidates, int workercount, boolean ignoreSilicon) throws IOException {
        System.out.println(treeDir.toString());
        Path[] trees = Files.find(treeDir, 2, (path, basicFileAttributes) -> path.toString().endsWith(".json")).toArray(s -> new Path[s]);

        final MsExperimentParser parser = new MsExperimentParser();
        List<Ms2Experiment> allExperiments = parser.getParser(mgfFile.toFile()).parseFromFile(mgfFile.toFile());

        Ms2Dataset dataset = new MutableMs2Dataset(allExperiments, "default", Double.NaN, (new Sirius("default")).getMs2Analyzer().getDefaultProfile());
        Ms2DatasetPreprocessor preprocessor = new Ms2DatasetPreprocessor(true);
        dataset = preprocessor.preprocess(dataset);
//        return parseMFCandidates(trees, allExperiments, maxCandidates, workercount, ignoreSilicon);
        return parseMFCandidates(trees, dataset.getExperiments(), maxCandidates, workercount, ignoreSilicon);
    }


    public static Map<String, List<FragmentsCandidate>> parseMFCandidatesEval(Path treeDir, Path mgfFile, int maxCandidates, int workercount, boolean ignoreSilicon) throws IOException {
        System.out.println(treeDir.toString());
        Path[] trees = Files.find(treeDir, 2, (path, basicFileAttributes) -> path.toString().endsWith(".json")).toArray(s -> new Path[s]);
        System.out.println("number "+trees.length);

        final MsExperimentParser parser = new MsExperimentParser();
        List<Ms2Experiment> allExperiments = parser.getParser(mgfFile.toFile()).parseFromFile(mgfFile.toFile());

        Ms2Dataset dataset = new MutableMs2Dataset(allExperiments, "default", Double.NaN, (new Sirius("default")).getMs2Analyzer().getDefaultProfile());
        Ms2DatasetPreprocessor preprocessor = new Ms2DatasetPreprocessor(true);
        dataset = preprocessor.preprocess(dataset);
//        return parseMFCandidates(trees, allExperiments, maxCandidates, workercount, ignoreSilicon);
        return parseMFCandidatesEval(trees, dataset.getExperiments(), maxCandidates, workercount, ignoreSilicon);
    }

    public static Map<String, List<FragmentsCandidate>> parseMFCandidatesEval(Path[] treesPaths, List<Ms2Experiment> experiments, int maxCandidates, int workercount, boolean ignoreSilicon) throws IOException {
//        final SpectralPreprocessor preprocessor = new SpectralPreprocessor((new Sirius()).getMs2Analyzer());
//        final Map<String, PriorityBlockingQueue<FragmentsCandidate>> explanationsMap = new HashMap<>();
        final Map<String, Ms2Experiment> experimentMap = new HashMap<>();
        for (Ms2Experiment experiment : experiments) {
            String name = cleanString(experiment.getName());
            if (experimentMap.containsKey(name)) throw new RuntimeException("experiment name duplicate");
            experimentMap.put(name, experiment);
        }


        ExecutorService service = Executors.newFixedThreadPool(workercount);
        List<Future> futures = new ArrayList<>();


        Map<String, List<FTree>> idToTrees = new HashMap<>();

        int[] pos = new int[]{0};
        final ConcurrentLinkedQueue<Path> pathQueue = new ConcurrentLinkedQueue<>(Arrays.asList(treesPaths));
        for (int i = 0; i < workercount; i++) {
            futures.add(service.submit(new Runnable() {
                @Override
                public void run() {
                    while (!pathQueue.isEmpty()) {
                        Path treePath = pathQueue.poll();
                        if (treePath==null) continue;

                        if (++pos[0]%1000==0) System.out.println("tree "+pos[0]);

                        final String name = treePath.getFileName().toString();
                        final String id = name.split("_")[0];
                        assert id.length()>0;

//                        Ms2Experiment experiment = experimentMap.get(id);
//                        if (experiment==null) throw new RuntimeException("cannot find experiment");

                        FTree tree = null;
                        try {
                            tree = new GenericParser<FTree>(new FTJsonReader()).parseFromFile(treePath.toFile()).get(0);
                        } catch (RuntimeException e) {
                            System.out.println("cannot read tree "+treePath.getFileName().toString());
                            continue;
                        } catch (IOException e){
                            throw new RuntimeException(e);
                        }

                        //todo changed
//                        if(tree.numberOfVertices() >= 3) {
                        //hier nochmal schauen....
                        if(tree.numberOfVertices() >= 1) {
                            tree = new IonTreeUtils().treeToNeutralTree(tree);

                            if (ignoreSilicon && tree.getRoot().getFormula().numberOf("Si")>0) continue;


                            List<FTree> trees = idToTrees.get(id);
                            if (trees==null){
                                synchronized (idToTrees){
                                    trees = idToTrees.get(id);
                                    if (trees==null){
                                        trees = new ArrayList<FTree>();
                                        idToTrees.put(id, trees);
                                    }
                                }
                            }

                            trees.add(tree);
                        }


                    }
                }
            }));


        }

        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        service.shutdown();


        final Map<String, List<FragmentsCandidate>> listMap = new HashMap<>();
        Set<String> keys = idToTrees.keySet();
        for (String key : keys) {
            List<FTree> trees  = idToTrees.get(key);

            if (!atLeastOneTreeExplainsSomeIntensity(trees)){
                System.out.println("exclude "+key+". No tree explains enough intensity.");
                continue;
            }
//            if (!atLeastOneTreeExplainsSomePeaks(trees)){
//                System.out.println("exclude "+key+". No tree explains enough peaks.");
//                continue;
//            }

            List<FragmentsCandidate> candidates = FragmentsCandidate.createAllCandidateInstances(trees, experimentMap.get(key));

            Collections.sort(candidates);
            if (candidates.size()>maxCandidates) candidates = candidates.subList(0, maxCandidates);
            listMap.put(key, candidates);
        }

        System.out.println("keys size "+keys.size());
        System.out.println("all compounds: "+experiments.size()+" | used compounds: "+listMap.size());

        return listMap;
    }

    public static Map<String, List<FragmentsCandidate>> parseMFCandidates(Path[] treesPaths, List<Ms2Experiment> experiments, int maxCandidates, int workercount, boolean ignoreSilicon) throws IOException {
//        final SpectralPreprocessor preprocessor = new SpectralPreprocessor((new Sirius()).getMs2Analyzer());
//        final Map<String, PriorityBlockingQueue<FragmentsCandidate>> explanationsMap = new HashMap<>();
        final Map<String, Ms2Experiment> experimentMap = new HashMap<>();
        for (Ms2Experiment experiment : experiments) {
            String name = cleanString(experiment.getName());
            if (experimentMap.containsKey(name)) throw new RuntimeException("experiment name duplicate");
            experimentMap.put(name, experiment);
        }


        ExecutorService service = Executors.newFixedThreadPool(workercount);
        List<Future> futures = new ArrayList<>();


        Map<String, List<FTree>> idToTrees = new HashMap<>();

        int[] pos = new int[]{0};
        final ConcurrentLinkedQueue<Path> pathQueue = new ConcurrentLinkedQueue<>(Arrays.asList(treesPaths));
        for (int i = 0; i < workercount; i++) {
            futures.add(service.submit(new Runnable() {
                @Override
                public void run() {
                    while (!pathQueue.isEmpty()) {
                        Path treePath = pathQueue.poll();
                        if (treePath==null) continue;

                        if (++pos[0]%1000==0) System.out.println("tree "+pos[0]);

                        final String name = treePath.getFileName().toString();
                        final String id = name.split("_")[0];
                        assert id.length()>0;

//                        Ms2Experiment experiment = experimentMap.get(id);
//                        if (experiment==null) throw new RuntimeException("cannot find experiment");

                        FTree tree = null;
                        try {
                            tree = new GenericParser<FTree>(new FTJsonReader()).parseFromFile(treePath.toFile()).get(0);
                        } catch (RuntimeException e) {
                            System.out.println("cannot read tree "+treePath.getFileName().toString());
                            continue;
                        } catch (IOException e){
                            throw new RuntimeException(e);
                        }

                        //todo changed
//                        if(tree.numberOfVertices() >= 3) {
                        //hier nochmal schauen....
                        if(tree.numberOfVertices() >= 1) {
                            tree = new IonTreeUtils().treeToNeutralTree(tree);

                            if (ignoreSilicon && tree.getRoot().getFormula().numberOf("Si")>0) continue;


                            List<FTree> trees = idToTrees.get(id);
                            if (trees==null){
                                synchronized (idToTrees){
                                    trees = idToTrees.get(id);
                                    if (trees==null){
                                        trees = new ArrayList<FTree>();
                                        idToTrees.put(id, trees);
                                    }
                                }
                            }

                            trees.add(tree);
                        }


                    }
                }
            }));


        }

        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        service.shutdown();


        final Map<String, List<FragmentsCandidate>> listMap = new HashMap<>();
        Set<String> keys = idToTrees.keySet();
        for (String key : keys) {
            List<FTree> trees  = idToTrees.get(key);
            Ms2Experiment experiment = experimentMap.get(key);
            if (!atLeastOneTreeExplainsSomeIntensity(trees, 0.5)){
                CompoundQuality.setProperty(experiment, SpectrumProperty.PoorlyExplained);
//                System.out.println("exclude "+key+". No tree explains enough intensity.");
//                continue;
            }
            if (!atLeastOneTreeExplainsSomePeaks(trees, 3)){
                CompoundQuality.setProperty(experiment, SpectrumProperty.PoorlyExplained);
//                System.out.println("exclude "+key+". No tree explains enough peaks.");
//                continue;
            }

            List<FragmentsCandidate> candidates = FragmentsCandidate.createAllCandidateInstances(trees, experiment);

            Collections.sort(candidates);
            if (candidates.size()>maxCandidates) candidates = candidates.subList(0, maxCandidates);
            listMap.put(key, candidates);
        }

        System.out.println("keys size "+keys.size());
        System.out.println("all compounds: "+experiments.size()+" | used compounds: "+listMap.size());

        return listMap;
    }

    public static boolean atLeastOneTreeExplainsSomeIntensity(List<FTree> trees){
        return atLeastOneTreeExplainsSomeIntensity(trees, 0.5);
    }

    public static boolean atLeastOneTreeExplainsSomeIntensity(List<FTree> trees, double threshold){
        for (FTree tree : trees) {
            final double intensity = tree.getAnnotationOrThrow(TreeScoring.class).getExplainedIntensity();
            if (intensity>threshold) return true;
        }
        return false;
    }

    public static boolean atLeastOneTreeExplainsSomePeaks(List<FTree> trees){
        return atLeastOneTreeExplainsSomePeaks(trees, 3);
    }

    public static boolean atLeastOneTreeExplainsSomePeaks(List<FTree> trees, int threshold){
        for (FTree tree : trees) {
            if (tree.numberOfVertices()>=threshold) return true;
        }
        return false;
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
//        System.out.println("formulas: "+formulas.size());
//        System.out.println("reactions: "+reactions.length);
        return reactions;

    }

    private void computeNet(int iterationSteps, int burnInIterations, double edgeWeight, int multipleTransformationCount) throws IOException {
        BufferedReader reader = Files.newBufferedReader(formulaFile);

        String[] header = reader.readLine().split(SEP);

//        Arrays.stream(header).filter(s -> s.toLowerCase().equals("score")).findFirst().
        int scoreIdx = IntStream.range(0, header.length).filter(i -> header[i].equals("score")).findFirst().getAsInt();
        int formulaIdx = IntStream.range(0, header.length).filter(i -> header[i].contains("formula")).findFirst().getAsInt();
        Map<String, List<Candidate>> explanationsMap = new HashMap<>();

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

                explanationsMap.get(id).add(new Candidate(mf, score));
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
    private void computeNet(Map<String, List<Candidate>> explanationsMap, int iterationSteps, int burnInIterations, double edgeWeight, int multipleTransformationCount) throws IOException {
        //todo Use weight of formulas as (directed) edge weight?????


        //todo use tree scores als "log logodds" and add weights or normalize sum(scores)=1 und use them as probabilities and multiply them


        int workerCount = Runtime.getRuntime().availableProcessors();
        //Zloty
        if (Runtime.getRuntime().availableProcessors()>20){
            workerCount /= 2;
        }


        String[] ids = explanationsMap.keySet().stream().filter(key -> explanationsMap.get(key).size()>0).toArray(s -> new String[s]);
        Candidate[][] explanations = new Candidate[ids.length][];

        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            explanations[i] = explanationsMap.get(id).toArray(new Candidate[0]);
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

        Scored<Candidate>[][] result = gibbsMFCorrectionNetwork.getChosenFormulas();



        //"probability/ relative score" tree score
        Scored<Candidate>[][]initialExplanations = getBestInitialAssignments(ids, explanationsMap);

        Scored<Candidate>[][] samplingMFs = gibbsMFCorrectionNetwork.getChosenFormulasBySampling();
        Scored<Candidate>[][] addedPosteriorProbsMFs = gibbsMFCorrectionNetwork.getChosenFormulasByAddedUpPosterior();
        Scored<Candidate>[][] maxPosteriorProbMFs = gibbsMFCorrectionNetwork.getChosenFormulasByMaxPosterior();


        writeOutput(outputFile, ids, new Scored[][][]{initialExplanations, samplingMFs, addedPosteriorProbsMFs, maxPosteriorProbMFs},
                new String[]{"initial", "sampling", "addedPosterior", "maxPosterior"}, SEP);


    }

    private void writeOutput(Path path, String[] ids, Scored<FragmentsCandidate>[][][] formulaCandidates, String[] methodNames, String sep) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path);
        final int length = formulaCandidates[0].length;
        for (int i = 0; i < formulaCandidates.length; i++) {
            Scored<FragmentsCandidate>[][] formulaA = formulaCandidates[i];
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
    private Scored<Candidate>[][] getBestInitialAssignments(String[] ids, Map<String, List<Candidate>> explanationsMap){
        Scored<Candidate>[][] array = new Scored[ids.length][];
        for (int i = 0; i < ids.length; i++) {
            List<Candidate> smfList = explanationsMap.get(ids[i]);
            Scored<Candidate>[] smfarray = new Scored[smfList.size()];
            double sum = 0d;
            for (Candidate candidateScored : smfList) {
                sum += candidateScored.getScore();
            }
            for (int j = 0; j < smfarray.length; j++) {
                smfarray[j] = new Scored<Candidate>(smfList.get(j), smfList.get(j).getScore()/sum);
            }
            Arrays.sort(smfarray);
            array[i] = smfarray;
        }
        return array;
    }

    private Scored<Candidate>[][] oldVsNewFormulaAssignment(String[] ids, Map<String, List<Scored<Candidate>>> explanationsMap, Scored<Candidate>[] newAssignments){
        Scored<Candidate>[][] array = new Scored[ids.length][];
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            List<Scored<Candidate>> smfList = explanationsMap.get(id);
            Scored<Candidate> best = smfList.stream().max((s1, s2) -> Double.compare(s1.getScore(), s2.getScore())).get();
            Scored<Candidate> newA = newAssignments[i];
            array[i] = new Scored[]{best, newA};
        }
        return array;
    }

    public void writeMFNetwork(Path outputPath, GibbsMFCorrectionNetwork<FragmentsCandidate> gibbsMFCorrectionNetwork) throws IOException {
        //        //network connecting all formulas
        BufferedWriter bw = Files.newBufferedWriter(outputPath);
        bw.write(Arrays.stream(new String[]{"MF1","Label1","MF2","Label2", "Score1", "Score2", "Rank1", "Rank2"}).collect(Collectors.joining(SEP)).toString());
        int[][] edges = gibbsMFCorrectionNetwork.getAllEdgesIndices();
        String[] ids = gibbsMFCorrectionNetwork.getIds();
        Scored<FragmentsCandidate>[][] explanations = gibbsMFCorrectionNetwork.getAllPossibleMolecularFormulas();
        for (int i = 0; i < edges.length; i++) {
            int[] e = edges[i];
            String peakID1 = ids[e[0]];
            Scored<FragmentsCandidate> candidate1 = explanations[e[0]][e[1]];
            String peakID2 = ids[e[2]];
            Scored<FragmentsCandidate> candidate2 = explanations[e[2]][e[3]];
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


    public void writeMFNetworkToDir(Path outputDir, Graph<FragmentsCandidate> graph) throws IOException {
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
        Scored<FragmentsCandidate>[][] explanations = graph.getPossibleFormulas();
        Candidate[][] candidates = parseCandiatesArray(explanations);
//        Candidate[] candidates1D = parseCandiatesArray(explanations);
        for (EdgeScorer edgeScorer : edgeScorers) edgeScorer.prepare(candidates);

        double[] edgeWeights = graph.getAllEdgesWeights();
        for (int i = 0; i < edges.length; i++) {
            int[] e = edges[i];
            String peakID1 = ids[e[0]];
            Scored<FragmentsCandidate> candidate1 = explanations[e[0]][e[1]];
            String peakID2 = ids[e[2]];
            Scored<FragmentsCandidate> candidate2 = explanations[e[2]][e[3]];

            double weight = edgeWeights[i];


            StringJoiner joinerEdgeInfo = new StringJoiner(SEP);
            joinerEdgeInfo.add(e[0]+"--"+candidate1.getCandidate().getFormula().formatByHill());
            joinerEdgeInfo.add(e[2]+"--"+candidate2.getCandidate().getFormula().formatByHill());

            joinerEdgeInfo.add(String.valueOf(candidate1.getCandidate().getFragments().length));
            joinerEdgeInfo.add(String.valueOf(candidate2.getCandidate().getFragments().length));

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
            Scored<FragmentsCandidate>[] candidates1D = explanations[i];
            for (int j = 0; j < candidates.length; j++) {
                Scored<FragmentsCandidate> candidate = candidates1D[j];
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

    private Candidate[] parseCandiatesArray1D(Scored<Candidate>[][] scoredCandidates) {
        int length = 0;
        for (Scored<Candidate>[] sc : scoredCandidates)
            length += sc.length;

        Candidate[] candidates = new Candidate[length];

        int i = 0;
        for (Scored<Candidate>[] sc : scoredCandidates) {
            for (Scored<Candidate> scoredCandidate : sc) {
                candidates[i++] = scoredCandidate.getCandidate();
            }
        }

        return candidates;
    }

    private <C extends Candidate<?>> C[][] parseCandiatesArray(Scored<C>[][] scoredCandidates) {
        final C[][] candidates = (C[][])new Candidate[scoredCandidates.length][];
        for (int i = 0; i < candidates.length; i++) {
            final Scored<C>[] scored = scoredCandidates[i];
            candidates[i] = (C[])new Candidate[scored.length];
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

        Candidate<MolecularFormula>[][] mfCandidates = new Candidate[compounds.length][];

        for (int i = 0; i < candidates.length; i++) {
            MolecularFormula[] candidateArray = candidates[i];
            Candidate[] scoredCandidateArray = new Candidate[candidateArray.length];
            for (int j = 0; j < candidateArray.length; j++) {
                MolecularFormula mf = candidateArray[j];
                Candidate<MolecularFormula> scoredMolecularFormula = new Candidate(mf, 0.1+Math.random()*0.01);
                scoredCandidateArray[j] = scoredMolecularFormula;
            }
            mfCandidates[i] = scoredCandidateArray;
        }

        GibbsMFCorrectionNetwork<Candidate<MolecularFormula>> gibbsMFCorrectionNetwork = new GibbsMFCorrectionNetwork(compounds, mfCandidates, reactions);

        gibbsMFCorrectionNetwork.iteration(10000,1000);

        Scored<Candidate<MolecularFormula>>[][] result = gibbsMFCorrectionNetwork.getChosenFormulas();

        for (int i = 0; i < result.length; i++) {
            Scored<Candidate<MolecularFormula>> scoredCandidate = result[i][0];
            System.out.println(scoredCandidate.getCandidate().getCandidate()+" -- "+scoredCandidate.getScore());
        }


    }


    private static TCharSet forbidden = new TCharHashSet(new char[]{' ',':','\\','/','[',']', '_'});
    private static String cleanString(String s){
        final StringBuilder builder = new StringBuilder(s.length());
        final char[] chars = s.toCharArray();
        for (char c : chars) {
            if (!forbidden.contains(c)) builder.append(c);
        }
        return builder.toString();
    }
}
