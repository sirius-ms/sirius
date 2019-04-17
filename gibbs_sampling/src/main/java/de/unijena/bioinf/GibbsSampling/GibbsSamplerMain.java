package de.unijena.bioinf.GibbsSampling;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.*;
import de.unijena.bioinf.GibbsSampling.model.scorer.*;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Ms2DatasetPreprocessor;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.projectspace.DirectoryReader;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import de.unijena.bioinf.sirius.projectspace.SiriusFileReader;
import de.unijena.bioinf.sirius.projectspace.SiriusWorkspaceReader;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private static final Logger LOG = LoggerFactory.getLogger(GibbsSamplerMain.class);
    private static Path formulaFile;
    private static Path outputFile;
    private static Path treeDir;
    private static Path mgfFile;
    private static Path libraryHits;
    private static Path graphOutputDir;
    private static int iterationSteps = 100000;
    private static int burnInIterations = 10000;
    private static EdgeFilter edgeFilter = new EdgeThresholdFilter(1.0D);
    private static boolean normalize = false;
    private static int maxCandidates = 50;
    private static boolean useLibraryHits = true;
    private static double libraryScore = 1.0D;
    final static String SEP = "\t";

    private static boolean is2Phase;
    private static boolean is3Phase;
    private final static int reactionStepSize = 1;

    private static ScoreProbabilityDistribution probabilityDistribution;

    public GibbsSamplerMain() {
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        if (args.length>0 && args[0].equalsIgnoreCase("--testIonization")){
            testIonization(Paths.get(args[1]));
            return;
        }
        GibbsSamplerMain main = new GibbsSamplerMain();
        if(args.length != 0 && !args[0].equals("--help") && !args[0].equals("-h")) {
            GibbsSamplerOptions opts = CliFactory.createCli(GibbsSamplerOptions.class).parseArguments(args);
            mgfFile = Paths.get(opts.getSpectrumsFile());
            libraryHits = Paths.get(opts.getCorrectHitsFile());
            outputFile = Paths.get(opts.getOutputPath());
            treeDir = Paths.get(opts.getTreeDir());
            if(opts.getOutputDirPath() != null) {
                graphOutputDir = Paths.get(opts.getOutputDirPath());
            } else {
                graphOutputDir = null;
            }

            iterationSteps = opts.getIterationSteps();
            burnInIterations = opts.getBurnInSteps();
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

            is2Phase = opts.isTwoPhase();
            is3Phase = opts.isThreePhase();


            EdgeScorer[] edgeScorers;
            if(opts.getProbabilityDistribution().toLowerCase().equals("exponential")) {
                probabilityDistribution = new ExponentialDistribution(opts.isMedian());
            } else if(opts.getProbabilityDistribution().toLowerCase().equals("pareto")) {
                probabilityDistribution = new ParetoDistribution(0.134, opts.isMedian());
            } else {
                if(!opts.getProbabilityDistribution().toLowerCase().equals("lognormal") && !opts.getProbabilityDistribution().toLowerCase().equals("log-normal")) {
                    System.out.println("unkown distribution function");
                    return;
                }

                probabilityDistribution = new LogNormalDistribution(opts.isMedian());
            }

            //todo changed !!!?!?!??!?!?!
            double minimumOverlap = 0.00D; //changed from 0.1

            CommonFragmentAndLossScorer commonFragmentAndLossScorer = new CommonFragmentAndLossScorer(minimumOverlap);

            EdgeScorer scoreProbabilityDistributionEstimator;
            if (opts.getParameters()==null || opts.getParameters().length()<=0){
                scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionEstimator(commonFragmentAndLossScorer, probabilityDistribution, opts.getThresholdFilter());
            } else {
                double[] parameters = Arrays.stream(opts.getParameters().replace("\"", "").replace("'", "").split(",")).mapToDouble(s->Double.parseDouble(s)).toArray();
                System.out.println("parameters: "+Arrays.toString(parameters));
                if (probabilityDistribution instanceof ExponentialDistribution){
                    probabilityDistribution = new ExponentialDistribution(parameters[0]);
                } else if (probabilityDistribution instanceof LogNormalDistribution){
                    probabilityDistribution = new LogNormalDistribution(parameters[0], parameters[1]);
                } else {
                    throw new RuntimeException("cannot set parameters for given distribution");
                }

                scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionFix(commonFragmentAndLossScorer, probabilityDistribution, opts.getThresholdFilter());
            }

            edgeScorers = new EdgeScorer[]{scoreProbabilityDistributionEstimator};

            if (opts.isMakeStats()) {
                main.makeStats(treeDir, mgfFile, libraryHits, outputFile, edgeScorers);
            } else if(opts.isSampleScores()) {
                main.sampleFromScoreDistribution(treeDir, mgfFile, libraryHits, outputFile, edgeScorers);
            } else if(opts.isCrossvalidation()) {
                main.doCVEvaluation(treeDir, mgfFile, libraryHits, outputFile, edgeScorers);
            } else if(opts.isRobustnessTest()) {
                main.testRobustness(treeDir, mgfFile, libraryHits, outputFile, edgeScorers, opts);
//            } else if (opts.isTestGraphGeneration()){
//                System.out.println("test graph");
//                main.testGraphGeneration(treeDir, mgfFile, libraryHits, edgeScorers, opts);
            } else if(opts.getEvalCliOutput()!=null) {
                Path clusterSummary = opts.getClusterSummary()==null?null:Paths.get(opts.getClusterSummary());
                main.evalZodiacOutput(treeDir, Paths.get(opts.getEvalCliOutput()), mgfFile, libraryHits, clusterSummary);
            } else {
                System.out.println("do evaluation");
                main.doEvaluation(treeDir, mgfFile, libraryHits, outputFile, edgeScorers);
            }

        } else {
            System.out.println(CliFactory.createCli(GibbsSamplerOptions.class).getHelpMessage());
        }
    }

    private static void testIonization(Path mgf) throws IOException {
        final MsExperimentParser parser = new MsExperimentParser();
        List<Ms2Experiment> allExperiments = parser.getParser(mgf.toFile()).parseFromFile(mgf.toFile());

        Deviation deviation = new Deviation(10);
        String[] ions = new String[]{"[M+H]+", "[M+K]+", "[M+Na]+"};
        PrecursorIonType[] ionTypes = new PrecursorIonType[ions.length];
        for (int i = 0; i < ionTypes.length; i++) {
            ionTypes[i] = PrecursorIonType.getPrecursorIonType(ions[i]);

        }

        for (Ms2Experiment experiment : allExperiments) {
            if (experiment.getMs1Spectra().size()==0) {
                System.err.println("no MS1 for "+experiment.getName());
                continue;
            }
            double precursorMass = experiment.getIonMass();
            int mostIntensiveIdx = -1;
            double maxIntensity = -1d;
            int pos = -1;
            //one ms1 corresponds to one ms2. we take ms2 with most intense ms1 precursor peak
            for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
                ++pos;
                Deviation dev = new Deviation(100);
                int idx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, dev);
                if (idx<0) continue;
                double intensity = spectrum.getIntensityAt(idx);
                if (intensity>maxIntensity){
                    maxIntensity = intensity;
                    mostIntensiveIdx = pos;
                }
            }

            if (mostIntensiveIdx<0){
                System.err.println("no precursor peak for "+experiment.getName());
            }

            Spectrum<Peak> ms1 = experiment.getMs1Spectra().get(mostIntensiveIdx);

            SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(ms1);
            Spectrums.normalizeToMax(mutableSpectrum, 100d);
            Spectrums.applyBaseline(mutableSpectrum, 5d);

            PrecursorIonType[] guessed = Spectrums.guessIonization(mutableSpectrum, experiment.getIonMass(), deviation, ionTypes);
            if (guessed==null){
                System.out.println(experiment.getName()+"\t"+"unknown");
            } else {
                System.out.print(experiment.getName());
                for (PrecursorIonType precursorIonType : guessed) {
                    System.out.print("\t"+precursorIonType.toString());
                }
                System.out.println();
            }

        }

    }

    protected void onlyKeepMostIntenseMS2(MutableMs2Experiment experiment){
        if (experiment.getMs2Spectra().size()==0) return;
        double precursorMass = experiment.getIonMass();
        int mostIntensiveIdx = -1;
        double maxIntensity = -1d;
        int pos = -1;
        if (experiment.getMs1Spectra().size()==experiment.getMs2Spectra().size()){
            //one ms1 corresponds to one ms2. we take ms2 with most intense ms1 precursor peak
            for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
                ++pos;
                Deviation dev = new Deviation(100);
                int idx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, dev);
                if (idx<0) continue;
                double intensity = spectrum.getIntensityAt(idx);
                if (intensity>maxIntensity){
                    maxIntensity = intensity;
                    mostIntensiveIdx = pos;
                }
            }
        }
        if (mostIntensiveIdx<0){
            //take ms2 with highest summed intensity
            pos = -1;
            for (Spectrum<Peak> spectrum : experiment.getMs2Spectra()) {
                ++pos;
                final int n = spectrum.size();
                double sumIntensity = 0d;
                for (int i = 0; i < n; ++i) {
                    sumIntensity += spectrum.getIntensityAt(i);
                }
                if (sumIntensity>maxIntensity){
                    maxIntensity = sumIntensity;
                    mostIntensiveIdx = pos;
                }
            }
        }

        List<SimpleSpectrum> ms1List = new ArrayList<>();
        List<MutableMs2Spectrum> ms2List = new ArrayList<>();
        if (experiment.getMs1Spectra().size()==experiment.getMs2Spectra().size()){
            ms1List.add(experiment.getMs1Spectra().get(mostIntensiveIdx));
        } else {
            ms1List.addAll(experiment.getMs1Spectra());
        }
        ms2List.add(experiment.getMs2Spectra().get(mostIntensiveIdx));
        experiment.setMs1Spectra(ms1List);
        experiment.setMs2Spectra(ms2List);
    }

    private void evalZodiacOutput(Path workspace, Path zodiacSummary, Path mgfFile, Path libraryHitsPath, Path clusterSummary) throws IOException {
        Map<String, List<FragmentsCandidate>> candidatesMap;
        System.out.println("Reading from Sirius workspace");
        candidatesMap = parseMFCandidatesFromWorkspace(workspace, mgfFile);

        Map<String, ResultInformation> zodiacResults = parseZodiacCliResults(zodiacSummary);

        Set<MolecularFormula> netSingleReactionDiffs = Arrays.stream(ZodiacUtils.parseReactions(reactionStepSize)).map(r -> r.netChange()).collect(Collectors.toSet());

        parseLibraryHits(libraryHitsPath, mgfFile, candidatesMap);


        Map<String, LibraryHit> correctHits = identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);


        Set<String> evaluationIds = extractEvaluationIds(candidatesMap, correctHits, 0.0, netSingleReactionDiffs);

        String[] ids = candidatesMap.keySet().stream().filter(key -> candidatesMap.get(key).size()>0).toArray(s -> new String[s]);


        Map<String, String> instanceToClusterRep;
        if (clusterSummary==null) instanceToClusterRep=null;
        else instanceToClusterRep = readCluster(clusterSummary);

        Scored<FragmentsCandidate>[][] result = mergeCandidatesWithZodiacResults(ids, candidatesMap, zodiacResults, instanceToClusterRep);


        int[] numberOfIds = statisticsOfKnownCompounds(result, ids, zodiacResults, evaluationIds, correctHits, null, true);
        System.out.println(numberOfIds[0]+" correct out of "+(numberOfIds[0]+numberOfIds[1]));
//
//

    }

    private Map<String, String> readCluster(Path clusterSummary) throws IOException {
        List<String> lines = Files.readAllLines(clusterSummary);
        Map<String, String> instanceToRepresentative = new HashMap<>();
        for (String line : lines) {
            String[] row = line.split("\t");
            String rep = row[0];
            for (int i = 1; i < row.length; i++) {
                String id = row[i];
                if (instanceToRepresentative.containsKey(id)){
                    throw new RuntimeException("id already contained in another cluster: "+id);
                }
                instanceToRepresentative.put(id, rep);

            }
        }
        return instanceToRepresentative;

    }

    private Scored<FragmentsCandidate>[][] mergeCandidatesWithZodiacResults(String[] ids, Map<String, List<FragmentsCandidate>> candidatesMap, Map<String, ResultInformation> zodiacResults, Map<String, String> instanceToClusterRepresentative) {
        Scored<FragmentsCandidate>[][] scored = new Scored[ids.length][];
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            String repId;
            if (instanceToClusterRepresentative==null) repId = id;
            else repId = instanceToClusterRepresentative.get(id);
            List<FragmentsCandidate> candidates = candidatesMap.get(repId);

            List<Scored<FragmentsCandidate>> scoredCandidates = new ArrayList<>();
            List<ScoredIonizedMolecularFormula> results = zodiacResults.get(id).candidates;
//            if (candidates.size()<results.size()) throw new RuntimeException("results are missing from candidates set for id "+id); //ignores dummy
            //map candidates and results
            for (ScoredIonizedMolecularFormula result : results) {
                MolecularFormula mf = result.formula;
                FragmentsCandidate fragmentsCandidate;
                if (DummyFragmentCandidate.dummy.equals(mf)){
                    fragmentsCandidate = DummyFragmentCandidate.newDummy(-1, -1, null);
                } else {
                    fragmentsCandidate = findCandidate(candidates, mf, id);
                }
                scoredCandidates.add(new Scored<>(fragmentsCandidate, result.score));
            }
            scored[i] = scoredCandidates.toArray(new Scored[0]);
        }
        return scored;
    }

    private FragmentsCandidate findCandidate(List<FragmentsCandidate> candidates, MolecularFormula mf, String id){
        for (FragmentsCandidate candidate : candidates) {
            if (candidate.getFormula().equals(mf)) return candidate;
        }
        throw new RuntimeException("cannot find candidate for "+mf+" for id "+id);
    }

    private Map<String, ResultInformation> parseZodiacCliResults(Path zodiacSummary) throws IOException {
        BufferedReader reader = Files.newBufferedReader(zodiacSummary, Charset.defaultCharset());
        //header
        reader.readLine();
        //todo this currently ignores number of connections !!!!
        String dummy = DummyFragmentCandidate.dummy.toString();
        Map<String,ResultInformation> map = new HashMap<>();
        String line;
        while ((line=reader.readLine())!=null) {
            String[] cols = line.split(SEP);
            String id = cols[0];
            String qualityString = cols[1];
            CompoundQuality compoundQuality = CompoundQuality.fromString(qualityString);
            List<ScoredIonizedMolecularFormula> scoredList = new ArrayList<>();
            for (int i = 5; i < cols.length; i+=3) {
                String formula = cols[i];
                MolecularFormula mf;
                if (formula.equals(dummy)){
                    mf = DummyFragmentCandidate.dummy;
                } else {
                    mf = MolecularFormula.parse(formula);
                }
                PrecursorIonType ionization = PrecursorIonType.getPrecursorIonType(cols[i+1]);
                double score = Double.parseDouble(cols[i+2]);


                scoredList.add(new ScoredIonizedMolecularFormula(mf, score, ionization));
            }
            ResultInformation resultInformation = new ResultInformation(compoundQuality, scoredList);
            map.put(id, resultInformation);
        }
        return map;
    }


    private class ResultInformation {
        CompoundQuality compoundQuality;
        List<ScoredIonizedMolecularFormula> candidates;

        public ResultInformation(CompoundQuality compoundQuality, List<ScoredIonizedMolecularFormula> candidates) {
            this.compoundQuality = compoundQuality;
            this.candidates = candidates;
        }
    }

    private class ScoredIonizedMolecularFormula {
        MolecularFormula formula;
        double score;
        PrecursorIonType ionType;

        public ScoredIonizedMolecularFormula(MolecularFormula formula, double score, PrecursorIonType ionType) {
            this.formula = formula;
            this.score = score;
            this.ionType = ionType;
        }
    }

    private static ScoreProbabilityDistribution readPCP(String pathString) throws IOException {
        Path path = Paths.get(pathString, new String[0]);
        TDoubleArrayList scores = new TDoubleArrayList();
        TDoubleArrayList pValues = new TDoubleArrayList();
        List<String> lines = Files.readAllLines(path);
        String header = (String)lines.remove(0);
        if(!header.equals("score\tpValue")) {
            throw new RuntimeException("incorrect pValue header");
        } else {

            for (String l : lines) {
                String[] col = l.split("\t");
                scores.add(Double.parseDouble(col[0]));
                pValues.add(Double.parseDouble(col[1]));
            }

            return new EmpiricalScoreProbabilityDistribution(scores.toArray(), pValues.toArray());
        }
    }

    protected void sampleFromScoreDistribution(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers) throws IOException {
        System.out.println("sample scores");
        int workerCount = Runtime.getRuntime().availableProcessors();
        if(Runtime.getRuntime().availableProcessors() > 20) {
            workerCount /= 2;
        }

        Set netSingleReactionDiffs = (Set)Arrays.stream(ZodiacUtils.parseReactions(reactionStepSize)).map((r) -> {
            return r.netChange();
        }).collect(Collectors.toSet());


        Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidates(treeDir, mgfFile, Integer.MAX_VALUE, workerCount); //remove candidates later when adding dummy
        parseLibraryHits(libraryHitsPath, mgfFile, candidatesMap);
        Map<String, LibraryHit> correctHits = identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);
        System.out.println("adding dummy node");
        ZodiacUtils.addNotExplainableDummy(candidatesMap, maxCandidates, LOG);

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
//        commonFragmentScorer.prepare(candidatesArray);
//        commonLossScorer.prepare(candidatesArray);
        int numberOfCandidates = 0;

        for(int i = 0; i < candidatesArray.length; ++i) {
            Candidate[] candidates = candidatesArray[i];
            numberOfCandidates += candidates.length;
        }

        long numberOfEdgesBound = (long)(numberOfCandidates * (numberOfCandidates - maxCandidates));
        System.out.println("numberOfEdgesBound " + numberOfEdgesBound);
        double samplingProb = 1000000.0D / numberOfEdgesBound;
        Path outpath = outputFile;
        HighQualityRandom rando = new HighQualityRandom();
        BufferedWriter writer = Files.newBufferedWriter(outpath, new OpenOption[0]);

//        writer.write("MF1\tMF2\ttreeSize1\ttreeSize2\tmass1\tmass2\tcommonF\tcommonL\tCommonFragmentAndLossScorer");
        writer.write("MF1\tMF2\ttreeSize1\ttreeSize2\tmass1\tmass2\tCommonFragmentAndLossScorer");

        if (numberOfEdgesBound>0){
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
//                            int commonF = commonFragmentScorer.getNumberOfCommon(c1, c2);
//                            int commonL = commonLossScorer.getNumberOfCommon(c1, c2);
//                            writer.write("\t" + commonF + "\t" + commonL);            HighQualityRandom random = new HighQualityRandom();
                                double score = commonFragmentAndLossScorer.scoreWithoutThreshold(c1, c2);
                                writer.write("\t" + String.valueOf(score));
                            }
                        }
                    }
                }
            }
        } else {
            int numberOfSamples = 500000;
            for (int i = 0; i < numberOfSamples; i++) {
                int color1 = rando.nextInt(candidatesArray.length);
                int color2 = rando.nextInt(candidatesArray.length - 1);
                if(color2 >= color1) {
                    ++color2;
                }
                
                int mf1 = rando.nextInt(candidatesArray[color1].length);
                int mf2 = rando.nextInt(candidatesArray[color2].length);

                writer.write("\n" + candidatesArray[color1][mf1].getFormula().formatByHill() + "\t" + candidatesArray[color2][mf2].getFormula().formatByHill());
                int treesize1 = candidatesArray[color1][mf1].getFragments().length;
                int treesize2 = candidatesArray[color2][mf2].getFragments().length;
                writer.write("\t" + treesize1 + "\t" + treesize2);
                writer.write("\t" + candidatesArray[color1][mf1].getFormula().getMass() + "\t" + candidatesArray[color2][mf2].getFormula().getMass());
                
                double score = commonFragmentAndLossScorer.scoreWithoutThreshold(candidatesArray[color1][mf1], candidatesArray[color2][mf2]);;
                writer.write("\t" + String.valueOf(score));
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

        Set<MolecularFormula> netSingleReactionDiffs = Arrays.stream(ZodiacUtils.parseReactions(reactionStepSize)).map(r -> r.netChange()).collect(Collectors.toSet());

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


    protected void testRobustness(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers, GibbsSamplerOptions opts) throws IOException, ExecutionException, InterruptedException {
//        if (GibbsMFCorrectionNetwork.iniAssignMostLikely) throw new RuntimeException("don't initialize MF candidates by most likely");
        int workerCount = Runtime.getRuntime().availableProcessors();
//            workerCount = 6;
        //Zloty
        if (Runtime.getRuntime().availableProcessors()>20){
            workerCount /= 2;
        }

//        workerCount = 1;


        //all possible netto changes of MolecularFormulas using on of the reactions.
        Set<MolecularFormula> netSingleReactionDiffs = Arrays.stream(ZodiacUtils.parseReactions(reactionStepSize)).map(r -> r.netChange()).collect(Collectors.toSet());

        Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidatesEval(treeDir, mgfFile, Integer.MAX_VALUE, workerCount, true); //remove candidates later when adding dummy

        parseLibraryHits(libraryHitsPath, mgfFile, candidatesMap);


        Map<String, LibraryHit> correctHits = identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);


        double useFreq = 0.0; //use x*100 percent of knowledge
        //todo don't use as strict information
        Set<String> evaluationIds = extractEvaluationIds(candidatesMap, correctHits, useFreq, netSingleReactionDiffs);




        System.out.println("adding dummy node");
        ZodiacUtils.addNotExplainableDummy(candidatesMap, maxCandidates, LOG);





//        //start Gibbs
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


        int numberOfRuns = 10;
        List<Scored<FragmentsCandidate>[][]> listOfResults = new ArrayList<>();
        String[] resultIds = ids;

        JobManager jobManager = new JobManager(workerCount);
        if (is2Phase){
            for (int i = 0; i < numberOfRuns; i++) {
                TwoPhaseGibbsSampling<FragmentsCandidate> twoPhaseGibbsSampling = new TwoPhaseGibbsSampling<>(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, 1, FragmentsCandidate.class);
                System.out.println("start");
                twoPhaseGibbsSampling.setIterationSteps(iterationSteps, burnInIterations);
                jobManager.submitJob(twoPhaseGibbsSampling);


                twoPhaseGibbsSampling.awaitResult();
                Scored<FragmentsCandidate>[][] result = twoPhaseGibbsSampling.getChosenFormulas();
                resultIds = twoPhaseGibbsSampling.getIds();

                System.out.println("result");
                statisticsOfKnownCompounds(result, resultIds, evaluationIds, correctHits, twoPhaseGibbsSampling.getGraph());
                listOfResults.add(result);
            }
        } else {
            //todo why producing different results?
//            GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, workerCount, 1);
//            System.out.println("number of candidates: "+gibbsParallel.getGraph().getSize());
//            if (graphOutputDir!=null) writeMFNetworkToDir(graphOutputDir, gibbsParallel.getGraph());
            for (int i = 0; i < numberOfRuns; i++) {


                if (useLibraryHits){
                    nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1), new LibraryHitScorer(libraryScore, 0.5, netSingleReactionDiffs)};
                    System.out.println("use LibraryHitScorer");
                } else {
                    nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1)};
                    System.out.println("ignore Library Hits");
                }

                double minimumOverlap = 0.00D; //changed from 0.1

                CommonFragmentAndLossScorer commonFragmentAndLossScorer;
                if (false) {
                    commonFragmentAndLossScorer = new CommonFragmentAndLossWithTreeScoresScorer(minimumOverlap);
                } else {
                    commonFragmentAndLossScorer = new CommonFragmentAndLossScorer(minimumOverlap);
                }

                EdgeScorer scoreProbabilityDistributionEstimator;
                if (opts.getParameters()==null || opts.getParameters().length()<=0){
                    scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionEstimator(commonFragmentAndLossScorer, probabilityDistribution, opts.getThresholdFilter());
                } else {
                    double[] parameters = Arrays.stream(opts.getParameters().replace("\"", "").replace("'", "").split(",")).mapToDouble(s->Double.parseDouble(s)).toArray();
                    System.out.println("parameters: "+Arrays.toString(parameters));
                    if (probabilityDistribution instanceof ExponentialDistribution){
                        probabilityDistribution = new ExponentialDistribution(parameters[0]);
                    } else if (probabilityDistribution instanceof LogNormalDistribution){
                        probabilityDistribution = new LogNormalDistribution(parameters[0], parameters[1]);
                    } else {
                        throw new RuntimeException("cannot set parameters for given distribution");
                    }

                    scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionFix(commonFragmentAndLossScorer, probabilityDistribution, opts.getThresholdFilter());
                }

                EdgeScorer[] currentEdgeScorers = new EdgeScorer[]{scoreProbabilityDistributionEstimator};

                GraphBuilder<FragmentsCandidate> graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);
                jobManager.submitJob(graphBuilder);
                Graph<FragmentsCandidate> graph = graphBuilder.awaitResult();
                GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel(graph, 1);

                System.out.println("start");
                gibbsParallel.setIterationSteps(iterationSteps, burnInIterations);
                jobManager.submitJob(gibbsParallel);
                gibbsParallel.awaitResult();
                Scored<FragmentsCandidate>[][] result = gibbsParallel.getChosenFormulas();

                System.out.println("result");
                statisticsOfKnownCompounds(result, resultIds, evaluationIds, correctHits, gibbsParallel.getGraph());
                listOfResults.add(result);
            }
        }

        jobManager.shutdown();

        System.out.println("final results");
        statisticsOfKnownCompounds(listOfResults, resultIds, evaluationIds, correctHits);
    }


    protected void testGraphGeneration(Path treeDir, Path mgfFile, Path libraryHitsPath, EdgeScorer[] edgeScorers, GibbsSamplerOptions opts) throws IOException, ExecutionException, InterruptedException {
//        if (GibbsMFCorrectionNetwork.iniAssignMostLikely) throw new RuntimeException("don't initialize MF candidates by most likely");
        int workerCount = Runtime.getRuntime().availableProcessors();
//            workerCount = 6;
        //Zloty
        if (Runtime.getRuntime().availableProcessors()>20){
            workerCount /= 2;
        }


        //all possible netto changes of MolecularFormulas using on of the reactions.
        Set<MolecularFormula> netSingleReactionDiffs = Arrays.stream(ZodiacUtils.parseReactions(reactionStepSize)).map(r -> r.netChange()).collect(Collectors.toSet());

        Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidatesEval(treeDir, mgfFile, Integer.MAX_VALUE, workerCount, true); //remove candidates later when adding dummy

        parseLibraryHits(libraryHitsPath, mgfFile, candidatesMap);

        Map<String, LibraryHit> correctHits = identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);


        double useFreq = 0.0; //use x*100 percent of knowledge
        //todo don't use as strict information
        Set<String> evaluationIds = extractEvaluationIds(candidatesMap, correctHits, useFreq, netSingleReactionDiffs);




        System.out.println("adding dummy node");
        ZodiacUtils.addNotExplainableDummy(candidatesMap, maxCandidates, LOG);




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


        JobManager jobManager = new JobManager(workerCount);
        //1. are their any changes after running gibbs?
        {
            GraphBuilder<FragmentsCandidate> graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);
            jobManager.submitJob(graphBuilder);
            Graph<FragmentsCandidate> graph = graphBuilder.awaitResult();
            GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel(graph, 1);
            System.out.println("number of candidates: "+gibbsParallel.getGraph().getSize());

            Graph graphBefore = gibbsParallel.getGraph();

            System.out.println("start");
            gibbsParallel.setIterationSteps(iterationSteps, burnInIterations);
            jobManager.submitJob(gibbsParallel);
            gibbsParallel.awaitResult();

            Graph graphAfter = gibbsParallel.getGraph();

            compare(graphBefore, graphAfter);
        }



        //2. differences for different EdgeFilter
        {
            edgeFilter = new EdgeThresholdFilter(0.9);
            GraphBuilder<FragmentsCandidate> graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);
            jobManager.submitJob(graphBuilder);
            Graph<FragmentsCandidate> graph = graphBuilder.awaitResult();
            GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel(graph,  1);
            System.out.println("number of candidates: "+gibbsParallel.getGraph().getSize());


            Graph graphSimpleThres = gibbsParallel.getGraph();

            if(opts.getProbabilityDistribution().toLowerCase().equals("exponential")) {
                probabilityDistribution = new ExponentialDistribution(opts.isMedian());
            } else if(opts.getProbabilityDistribution().toLowerCase().equals("pareto")) {
                probabilityDistribution = new ParetoDistribution(0.134, opts.isMedian());
            } else {
                if(!opts.getProbabilityDistribution().toLowerCase().equals("lognormal") && !opts.getProbabilityDistribution().toLowerCase().equals("log-normal")) {
                    System.out.println("unkown distribution function");
                    return;
                }

                probabilityDistribution = new LogNormalDistribution(opts.isMedian());
            }

            //todo changed !!!?!?!??!?!?!
            double minimumOverlap = 0.00D; //changed from 0.1

            CommonFragmentAndLossScorer commonFragmentAndLossScorer;
            if (false) {
                commonFragmentAndLossScorer = new CommonFragmentAndLossWithTreeScoresScorer(minimumOverlap);
            } else {
                commonFragmentAndLossScorer = new CommonFragmentAndLossScorer(minimumOverlap);
            }

            EdgeScorer scoreProbabilityDistributionEstimator;
            if (opts.getParameters()==null || opts.getParameters().length()<=0){
                scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionEstimator(commonFragmentAndLossScorer, probabilityDistribution, opts.getThresholdFilter());
            } else {
                double[] parameters = Arrays.stream(opts.getParameters().replace("\"", "").replace("'", "").split(",")).mapToDouble(s->Double.parseDouble(s)).toArray();
                System.out.println("parameters: "+Arrays.toString(parameters));
                if (probabilityDistribution instanceof ExponentialDistribution){
                    probabilityDistribution = new ExponentialDistribution(parameters[0]);
                } else if (probabilityDistribution instanceof LogNormalDistribution){
                    probabilityDistribution = new LogNormalDistribution(parameters[0], parameters[1]);
                } else {
                    throw new RuntimeException("cannot set parameters for given distribution");
                }

                scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionFix(commonFragmentAndLossScorer, probabilityDistribution, opts.getThresholdFilter());
            }

            edgeScorers = new EdgeScorer[]{scoreProbabilityDistributionEstimator};


            edgeFilter = new EdgeThresholdMinConnectionsFilter(0.9, 1, 10);
            graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);
            jobManager.submitJob(graphBuilder);
            Graph<FragmentsCandidate> graphComplexThres = graphBuilder.awaitResult();
            gibbsParallel = new GibbsParallel(graph, 1);
            System.out.println("number of candidates: "+gibbsParallel.getGraph().getSize());

            compare(graphSimpleThres, graphComplexThres);


        }

        jobManager.shutdown();

    }

    /*
    doing some nice comparison report of 2 Graphs
     */
    protected void compare(Graph<FragmentsCandidate> graph1, Graph<FragmentsCandidate> graph2){
        //assert same node set?
        //number of edges
        //edges subset in both directions


        //edge scores different?
        // especially. Are their different scores for unused edges?
        // compare thresholds

        System.out.printf("number of compounds:\t%d\t%d%n", graph1.numberOfCompounds(), graph2.numberOfCompounds());
        System.out.printf("number of mf candidates:\t%d\t%d%n", graph1.getPossibleFormulas1D().length, graph2.getPossibleFormulas1D().length);
        System.out.printf("number of mf candidates:\t%d\t%d%n", graph1.getSize(), graph2.getSize());

        int numberOfEdges1 = numberOfEdges(graph1);
        int numberOfEdges2 = numberOfEdges(graph2);
        System.out.printf("number of edges:\t%d\t%d%n", numberOfEdges1, numberOfEdges2);

        int[][] connections1 = graph1.getConnections();
        int[][] connections2 = graph2.getConnections();
        int numberOfCommons = 0;
        for (int i = 0; i < connections1.length; i++) {
            int[] c1 = connections1[i];
            int[] c2 = connections2[i];
            numberOfCommons += numberOfCommon(c1, c2);
        }
        numberOfCommons /=2; //both directions
        System.out.printf("number of common edges:\t%d%n", numberOfCommons);
        System.out.printf("number of common unique edges:\t%d\t%d%n", (numberOfEdges1-numberOfCommons), (numberOfEdges2-numberOfCommons));




        //same nodes?
        Scored<FragmentsCandidate>[] candidates1 = graph1.getPossibleFormulas1D();
        Scored<FragmentsCandidate>[] candidates2 = graph2.getPossibleFormulas1D();
        int differentScore = 0;
        int differentMF = 0;
        int differentCandiate = 0;
        for (int i = 0; i < candidates1.length; i++) {
            Scored<FragmentsCandidate> c1 = candidates1[i];
            Scored<FragmentsCandidate> c2 = candidates2[i];
            if (Math.abs(c1.getScore()-c2.getScore())>1e-16) ++differentScore;
            if (!c1.getCandidate().getFormula().equals(c2.getCandidate().getFormula())) ++differentMF;
            if (!c1.getCandidate().getFragments().equals(c2.getCandidate().getFragments())) ++differentCandiate;
        }

        System.out.println("nodes");
        System.out.printf("different mf:%d%n", differentMF);
        System.out.printf("different candidates:%d%n", differentCandiate);
        System.out.printf("different score:%d%n", differentScore);



        //edge scores
        System.out.println("used edge scores");
        int sameScores = 0;
        int biggerScores1 = 0;
        int biggerScores2 = 0;
        for (int i = 0; i < connections1.length; i++) {
            int[] c1 = connections1[i];
            int[] c2 = connections2[i];
            int[] commons = extractCommons(c1, c2);
            for (int c : commons) {
                double score1 = graph1.getLogWeight(i, c);
                double score2 = graph2.getLogWeight(i, c);
                if (Math.abs(score1-score2)<1e-16) ++sameScores;
                else if (score1>score2) ++biggerScores1;
                else ++biggerScores2;
            }
        }
        System.out.printf("edges with same scores:\t%d%n", sameScores);
        System.out.printf("edges with larger scores:\t%d\t%d%n", biggerScores1, biggerScores2);


        //all same edge weights!?? just for equal graphs
        System.out.println("all edge scores");
        sameScores = 0;
        biggerScores1 = 0;
        biggerScores2 = 0;

        for (int i = 0; i < graph1.getSize(); i++) {
            int[] c1 = graph1.getLogWeightConnections(i);
            int[] c2 = graph2.getLogWeightConnections(i);
            int[] commons = extractCommons(c1, c2);
            for (int c : commons) {
                double score1 = graph1.getLogWeight(i, c);
                double score2 = graph2.getLogWeight(i, c);
                if (Math.abs(score1-score2)<1e-16) ++sameScores;
                else if (score1>score2) ++biggerScores1;
                else ++biggerScores2;
            }
        }
        System.out.printf("(non-)edges with same scores:\t%d%n", sameScores);
        System.out.printf("(non-)edges with larger scores:\t%d\t%d%n", biggerScores1, biggerScores2);
        
        //threshold
        System.out.println("thresholds");
        int sameThresholds = 0;
        int biggerThresholds1 = 0;
        int biggerThresholds2 = 0;
        double[] thresholds1 = IntStream.range(0, graph1.getSize()).mapToDouble(i->graph1.getEdgeThreshold(i)).toArray();
        double[] thresholds2 = IntStream.range(0, graph2.getSize()).mapToDouble(i->graph2.getEdgeThreshold(i)).toArray();
        for (int i = 0; i < thresholds1.length; i++) {
            double t1 = thresholds1[i];
            double t2 = thresholds2[i];
            if (Math.abs(t1-t2)<1e-16) ++sameThresholds;
            else if (t1>t2) ++biggerThresholds1;
            else ++biggerThresholds2;

        }

        System.out.printf("same thresolds:\t%d%n", sameThresholds);
        System.out.printf("larger thresolds:\t%d\t%d%n", biggerThresholds1, biggerThresholds2);

    }

    private int numberOfEdges(Graph graph) {
        int[][] connections = graph.getConnections();
        int numberOfConnections = Arrays.stream(connections).mapToInt(c -> c.length).sum()/2;

        int numberOfConnections2 = IntStream.range(0,graph.getSize()).map(i -> graph.getNumberOfConnections(i)).sum()/2;

        assert numberOfConnections==numberOfConnections2;

        return numberOfConnections;
    }

    private int numberOfCommon(int[] c1, int[] c2) {
        int[] C1 = Arrays.copyOf(c1, c1.length);
        int[] C2 = Arrays.copyOf(c2, c2.length);
        Arrays.sort(C1);
        Arrays.sort(C2);

        int i=0, j=0;
        int common = 0;
        while (i<C1.length && j<C2.length) {
            if (C1[i]<C2[j]) ++i;
            else if (C1[i]>C2[j]) ++j;
            else {
                ++common;
                ++i;++j;
            }
        }
        return common;
    }

    private int[] extractCommons(int[] c1, int[] c2) {
        int[] C1 = Arrays.copyOf(c1, c1.length);
        int[] C2 = Arrays.copyOf(c2, c2.length);
        Arrays.sort(C1);
        Arrays.sort(C2);

        int i=0, j=0;
        TIntArrayList list = new TIntArrayList();
        while (i<C1.length && j<C2.length) {
            if (C1[i]<C2[j]) ++i;
            else if (C1[i]>C2[j]) ++j;
            else {
                list.add(C1[i]);
                ++i;++j;
            }
        }
        return list.toArray();
    }

    private boolean isSiriusWorkspace(Path dir){
        //only tests if this is not a simple collection of json trees
        if (dir.toFile().getName().endsWith("zip")) return true;
        for (File file : dir.toFile().listFiles()) {
            if (file.getName().equals("version.txt")) return true;
        }
        return false;
    }

    protected void doEvaluation(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers) throws IOException, ExecutionException, InterruptedException {
        if (!GibbsMFCorrectionNetwork.iniAssignMostLikely) throw new RuntimeException("initialize MF candidates by most likely");
        int workerCount = Runtime.getRuntime().availableProcessors();
//            workerCount = 6;
        //Zloty
        if (Runtime.getRuntime().availableProcessors()>20){
            workerCount /= 2;
        }


//        //reactions

        //all possible netto changes of MolecularFormulas using on of the reactions.
        Set<MolecularFormula> netSingleReactionDiffs = Arrays.stream(ZodiacUtils.parseReactions(reactionStepSize)).map(r -> r.netChange()).collect(Collectors.toSet());




//        Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidatesEval(treeDir, mgfFile, Integer.MAX_VALUE, workerCount, true); //remove candidates later when adding dummy
        //changed assigning CompoundQuality
        Map<String, List<FragmentsCandidate>> candidatesMap;
        if (isSiriusWorkspace(treeDir)){
            System.out.println("Reading from Sirius workspace");
            candidatesMap = parseMFCandidatesFromWorkspace(treeDir, mgfFile);
        } else {
            candidatesMap = parseMFCandidates(treeDir, mgfFile, Integer.MAX_VALUE, workerCount, true); //remove candidates later when adding dummy
        }


        System.out.println(candidatesMap.size()+" compounds");


        parseLibraryHits(libraryHitsPath, mgfFile, candidatesMap);


        Map<String, LibraryHit> correctHits = identifyCorrectLibraryHits(candidatesMap, netSingleReactionDiffs);


//        System.out.println("measuredMass trueMass");
//        for (LibraryHit libraryHit : correctHits.values()) {
//            double measured = libraryHit.getQueryExperiment().getIonMass();
//            double trueMass = libraryHit.getIonType().addIonAndAdduct(libraryHit.getMolecularFormula().getMass());
//            System.out.println(measured+" "+trueMass);
//        }
//        System.out.println("...................");



        double useFreq = 0.0; //use x*100 percent of knowledge
        //todo don't use as strict information
        Set<String> evaluationIds = extractEvaluationIds(candidatesMap, correctHits, useFreq, netSingleReactionDiffs);




        System.out.println("adding dummy node");
        if (is3Phase) ZodiacUtils.addNotExplainableDummy(candidatesMap, Integer.MAX_VALUE, LOG);
        else ZodiacUtils.addNotExplainableDummy(candidatesMap, maxCandidates, LOG);




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


        JobManager jobManager = new JobManager(workerCount);
        if (is3Phase){
            int numberOfCandidatesInFirstRound = maxCandidates;
            ThreePhaseGibbsSampling threePhaseGibbsSampling = new ThreePhaseGibbsSampling(ids, candidatesArray, numberOfCandidatesInFirstRound, nodeScorers, edgeScorers, edgeFilter, jobManager, 1);
            System.out.println("start");
            threePhaseGibbsSampling.run(iterationSteps, burnInIterations);

            Scored<FragmentsCandidate>[][] result = threePhaseGibbsSampling.getChosenFormulas();
            System.out.println("standard");
            statisticsOfKnownCompounds(result, threePhaseGibbsSampling.getIds(), evaluationIds, correctHits, threePhaseGibbsSampling.getGraph());

            if (outputFile!=null) writeBestFormulas(threePhaseGibbsSampling.getChosenFormulas(), threePhaseGibbsSampling.getGraph(), outputFile);
        }
        else if (is2Phase){
            TwoPhaseGibbsSampling<FragmentsCandidate> twoPhaseGibbsSampling = new TwoPhaseGibbsSampling<>(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter,1, FragmentsCandidate.class);
            System.out.println("start");
            twoPhaseGibbsSampling.setIterationSteps(iterationSteps, burnInIterations);
            jobManager.submitJob(twoPhaseGibbsSampling);
            twoPhaseGibbsSampling.awaitResult();
            Scored<FragmentsCandidate>[][] result = twoPhaseGibbsSampling.getChosenFormulas();
            System.out.println("standard");
            statisticsOfKnownCompounds(result, twoPhaseGibbsSampling.getIds(), evaluationIds, correctHits, twoPhaseGibbsSampling.getGraph());

            if (outputFile!=null) writeBestFormulas(twoPhaseGibbsSampling.getChosenFormulas(), twoPhaseGibbsSampling.getGraph(), outputFile);
        } else {
            //GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, workerCount, 20);
            //changed

            GraphBuilder<FragmentsCandidate> graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);
            jobManager.submitJob(graphBuilder);
            Graph<FragmentsCandidate> graph = graphBuilder.awaitResult();
            GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel(graph, 1);


            if (graphOutputDir!=null) writeMFNetworkToDir(graphOutputDir, gibbsParallel.getGraph(), edgeScorers);




            System.out.println("start");
            gibbsParallel.setIterationSteps(iterationSteps, burnInIterations);
            jobManager.submitJob(gibbsParallel);
            gibbsParallel.awaitResult();
            Scored<FragmentsCandidate>[][] result = gibbsParallel.getChosenFormulas();
            System.out.println("standard");
            statisticsOfKnownCompounds(result, ids, evaluationIds, correctHits, gibbsParallel.getGraph());

            if (outputFile!=null){
                writeBestFormulas(gibbsParallel.getChosenFormulasBySampling(), gibbsParallel.getGraph(), outputFile);
                Path zodiacOutput = outputFile.resolveSibling("zodiac_results_output_"+outputFile.getFileName().toString());
                Scored<FragmentsCandidate>[][] bestInitial = getBestInitialAssignments(ids, candidatesMap);
                writeZodiacOutput(ids, bestInitial, gibbsParallel.getChosenFormulasBySampling(), gibbsParallel.getGraph(), zodiacOutput);
            }
        }

        jobManager.shutdown();
    }

    protected void doCVEvaluation(Path treeDir, Path mgfFile, Path libraryHitsPath, Path outputFile, EdgeScorer[] edgeScorers) throws IOException, ExecutionException {
        if (!GibbsMFCorrectionNetwork.iniAssignMostLikely) throw new RuntimeException("initialize MF candidates by most likely");
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
        Set<MolecularFormula> netSingleReactionDiffs = Arrays.stream(ZodiacUtils.parseReactions(reactionStepSize)).map(r -> r.netChange()).collect(Collectors.toSet());

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
        ZodiacUtils.addNotExplainableDummy(candidatesMap, maxCandidates,LOG);




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

            int repetitions = workerCount;
            JobManager jobManager = new JobManager(workerCount);
            GraphBuilder<FragmentsCandidate> graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);
            jobManager.submitJob(graphBuilder);
            Graph<FragmentsCandidate> graph = graphBuilder.awaitResult();
            GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel<FragmentsCandidate>(graph, repetitions);


            System.out.println("start");
            gibbsParallel.setIterationSteps(iterationSteps, burnInIterations);
            jobManager.submitJob(gibbsParallel);
            gibbsParallel.awaitResult();
            Scored<FragmentsCandidate>[][] standardresult = gibbsParallel.getChosenFormulas();
            System.out.println("standard");
            statisticsOfKnownCompounds(standardresult, ids, evaluationIds, correctHits, gibbsParallel.getGraph());


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

    public static void writeZodiacOutput(String[] ids, Scored<FragmentsCandidate>[][] initial, Scored<FragmentsCandidate>[][] result, Graph<FragmentsCandidate> graph, Path outputPath) throws IOException {
        writeZodiacOutput(ids, initial, result, graph, null, outputPath);
    }

    private final static int NUMBER_OF_HITS = Integer.MAX_VALUE;
    public static void writeZodiacOutput(String[] ids, Scored<FragmentsCandidate>[][] initial, Scored<FragmentsCandidate>[][] result, Graph<FragmentsCandidate> graph, Map<String, String[]> representativeToCluster, Path outputPath) throws IOException {
        int[] connectingPeaks = graph.getMaxConnectedCompoundsCounts();
        String[] ids2 = graph.getIds();
        if (ids.length!=ids2.length) throw new RuntimeException("unexpected number of solved instances");


        BufferedWriter writer = Files.newBufferedWriter(outputPath, Charset.defaultCharset());
        //changed connectingPeaks -> connectedCompounds
        writer.write("id" + SEP + "SiriusMF" + SEP + "SiriusScore" + SEP + "connectedCompounds" + SEP + "ZodiacMF" + SEP + "ZodiacScore");
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            final String siriusMF = initial[i][0].getCandidate().getFormula().formatByHill();
            final double siriusScore = initial[i][0].getScore();

            final int connections = connectingPeaks[i];
            if (!id.equals(ids2[i])) throw new RuntimeException("different ids: "+id+" vs "+ids2[i]);

            if (representativeToCluster==null){
                String summeryLine = createSummaryLine(id, siriusMF, siriusScore, connections, result[i]);
                writer.write("\n");
                writer.write(summeryLine);
            } else {
                String[] clusterIds = representativeToCluster.get(id);
                for (String clusterId : clusterIds) {
                    String summeryLine = createSummaryLine(clusterId, siriusMF, siriusScore, connections, result[i]);
                    writer.write("\n");
                    writer.write(summeryLine);
                }
            }

        }

        writer.close();

    }

    private static String createSummaryLine(String id, String siriusMF, double siriusScore, int numberConnections, Scored<FragmentsCandidate>[] result){
        StringBuilder builder = new StringBuilder();
        builder.append(id);
        builder.append(SEP);
        builder.append(siriusMF);
        builder.append(SEP);
        builder.append(Double.toString(siriusScore));
        builder.append(SEP);
        builder.append(numberConnections);

        for (int j = 0; j < Math.min(result.length, NUMBER_OF_HITS); j++) {
            Scored<FragmentsCandidate> currentResult = result[j];
            final String mf = currentResult.getCandidate().getFormula().formatByHill();
            final double score = currentResult.getScore();

            if (score <= 0) break; //don't write MF with 0 probability

            builder.append(SEP);
            builder.append(mf);
            builder.append(SEP);
            builder.append(Double.toString(score));
        }
        return builder.toString();
    }

    private void writeBestFormulas(Scored<FragmentsCandidate>[][] results, Graph<FragmentsCandidate> graph, Path outputFile) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset(), new OpenOption[0]);
        String SEP = "\t";

        //adding biotransformations very likely makes it slow
        System.out.println("parsing reactions");
        Reaction[] reactions = ZodiacUtils.parseReactions(reactionStepSize);
        Map<MolecularFormula, List<Reaction>> netSingleReactionDiffs = new HashMap<>();
        for (Reaction reaction : reactions) {
            MolecularFormula net = reaction.netChange();
            List<Reaction> list = netSingleReactionDiffs.get(net);
            if (list==null){
                list = new ArrayList<>();
                netSingleReactionDiffs.put(net, list);
            }
            list.add(reaction);
        }
        System.out.println("number of transformations: "+reactions.length);
        System.out.println("number of unique mf: "+netSingleReactionDiffs.size());

        for(int i = 0; i < results.length; ++i) {
            Scored<FragmentsCandidate>[] result = results[i];
            Scored<FragmentsCandidate> best = result[0];
            FragmentsCandidate initalBest = graph.getPossibleFormulas()[i][0].getCandidate(); //todo correctly sorted?
            String id = best.getCandidate().getExperiment().getName();
            String formula = best.getCandidate().getFormula().formatByHill();
            String iniFormula = initalBest.getFormula().formatByHill();
            int numberOfEdges = graph.getConnections(graph.getAbsoluteFormulaIdx(i, 0)).length;
            int numberOfCandidates = result.length;
            boolean isCorrect = best.getCandidate().isCorrect();
            double score = best.getScore();
            String libraryHit = best.getCandidate().hasLibraryHit()?best.getCandidate().getLibraryHit().getMolecularFormula().formatByHill():null;
            double cosine = best.getCandidate().hasLibraryHit()?best.getCandidate().getLibraryHit().getCosine():-1;
            StringJoiner joiner = new StringJoiner(SEP);
            joiner.add(id); joiner.add(iniFormula); joiner.add(formula); joiner.add(String.valueOf(score)); joiner.add(String.valueOf(numberOfCandidates)); joiner.add(String.valueOf(numberOfEdges)); joiner.add(String.valueOf(isCorrect));
            if (libraryHit!=null){
                int pos = -1;
                String correctMF = null;
                String correctStruct = best.getCandidate().getLibraryHit().getStructure();
                for (int j = 0; j < result.length; j++) {
                    if (result[j].getCandidate().isCorrect){
                        pos = j+1;
                        correctMF = result[j].getCandidate().getFormula().formatByHill();
                        break;
                    };

                }
                joiner.add(libraryHit);
                joiner.add(String.valueOf(cosine));
                joiner.add(correctStruct);
                joiner.add(String.valueOf(pos));
                if (pos>=0){
                    joiner.add(correctMF);

                }

                List<Reaction> possibleReactions = matchingReactions(best.getCandidate().getFormula(), best.getCandidate().getLibraryHit().getMolecularFormula(), netSingleReactionDiffs);

                if (possibleReactions!=null){
                    for (Reaction possibleReaction : possibleReactions) {
                        joiner.add(possibleReaction.toString());
                    }
                }

            }
            writer.write(joiner.toString()); writer.write("\n");
//            writer.write(id + SEP + iniFormula + SEP + formula + SEP + score + SEP + numberOfEdges + SEP + isCorrect + (libraryHit!=null?SEP+libraryHit:"") + "\n");
        }

        writer.close();
    }


    private List<Reaction> matchingReactions(MolecularFormula candidate, MolecularFormula libraryHit, Map<MolecularFormula, List<Reaction>> netSingleReactionDiffs) {
        MolecularFormula net = candidate.subtract(libraryHit);
        if (net.getMass()<0) net = net.negate();
        return netSingleReactionDiffs.get(net);
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
//        System.out.println("allowedDifferences "+allowedDifferences.size());
        Set<String> usedIds = new HashSet<>();
        for (String id : ids) {
            LibraryHit libraryHit = correctHitsMap.get(id);
            MolecularFormula correctMF = libraryHit.getMolecularFormula();
            List<FragmentsCandidate> candidates = candidatesMap.get(id);
            if (candidates==null){
                System.out.println();
//                throw new RuntimeException("all candidates have been removed: "+id);
                LOG.error("all candidates have been removed: "+id);
            } else {
//                System.out.println("candidates size "+candidates.size()+" for "+id);
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
                        //measuredMass trueMass
                        double measured = candidate.getExperiment().getIonMass();
                        double trueMass = candidate.getIonType().addIonAndAdduct(candidate.getFormula().getMass());
                        System.out.println(measured+" "+trueMass+" "+candidate.getFormula()+" "+candidate.getIonType().getIonization()+"\n");
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
                if (correctHits>1){
                    List<String> corrects = new ArrayList<>();
                    for (FragmentsCandidate candidate : candidates) {
                        if (candidate.isCorrect()) corrects.add(candidate.getFormula().formatByHill());
                        candidate.setCorrect(false);
                        candidate.setInTrainingSet(false);
                        candidate.setInEvaluationSet(false);
                    }
                    LOG.warn("exclude "+id+
                            ". multiple potentially correct hits: "+Arrays.toString(corrects.toArray())+
                            ". Library hit is: "+libraryHit.getMolecularFormula());

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

    @Deprecated
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
            LibraryHit libraryHit = new LibraryHit(experiment, formula, structure, ionType, cosine, sharedPeaks, quality, ionType.neutralMassToPrecursorMass(formula.getMass()));
            for (C candidate : candidatesList) {
                candidate.setLibraryHit(libraryHit);
            }
        }
    }

    private static boolean isAllIdsOrdered(List<String> ids){
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            try {
                int integer = Integer.parseInt(id);
                if (integer!=(i+1)) return false;
            } catch (NumberFormatException e){
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public static  <C extends Candidate & HasLibraryHit> void parseLibraryHits(Path libraryHitsPath, Path mgfFile, Map<String, List<C>> candidatesMap) throws IOException {
        try {
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

            if (isAllIdsOrdered(featureIDs)) System.out.println("all experiment ids in ascending order without any missing");

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
                        System.err.println("cannot parse molecular formula of library hit #SCAN# "+scanNumber);
                        continue;
                    }

                    final String structure = (isInchi(cols[indices[1]]) ? cols[indices[1]] : cols[indices[2]]);
                    final PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(cols[indices[3]]);
                    final double cosine = Double.parseDouble(cols[indices[4]]);
                    final int sharedPeaks = Integer.parseInt(cols[indices[5]]);
                    final LibraryHitQuality quality = LibraryHitQuality.valueOf(cols[indices[6]]);
                    LibraryHit libraryHit = new LibraryHit(experiment, formula, structure, ionType, cosine, sharedPeaks, quality, ionType.neutralMassToPrecursorMass(formula.getMass()));
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


//    public static  List<LibraryHit> parseLibraryHits(Path libraryHitsPath, Path mgfFile) throws IOException {
//        try {
//            List<String> featureIDs = new ArrayList<>();
//            try(BufferedReader reader = Files.newBufferedReader(mgfFile)){
//                String line;
//                String lastID = null;
//                while ((line=reader.readLine())!=null){
//                    if (line.toLowerCase().startsWith("feature_id=")){
//                        String id = line.split("=")[1];
//                        if (!id.equals(lastID)){
//                            featureIDs.add(id);
//                            lastID = id;
//                        }
//                    }
//                }
//            }
//
//
////            if (isAllIdsOrdered(featureIDs)) System.out.println("all experiment ids in ascending order without any missing");
//
//
//            final MsExperimentParser parser = new MsExperimentParser();
//            List<Ms2Experiment> experiments = parser.getParser(mgfFile.toFile()).parseFromFile(mgfFile.toFile());
//
//
//            //todo clean string !?!?
//            final Map<String, Ms2Experiment> experimentMap = new HashMap<>();
//            for (Ms2Experiment experiment : experiments) {
//                String name = cleanString(experiment.getName());
//                if (experimentMap.containsKey(name)) throw new RuntimeException("experiment name duplicate: "+name);
//                experimentMap.put(name, experiment);
//            }
//
//            //todo change nasty hack
//
//
//            List<String> lines = Files.readAllLines(libraryHitsPath, Charset.defaultCharset());
//            String[] header = lines.remove(0).split("\t");
////        String[] ofInterest = new String[]{"Feature_id", "Formula", "Structure", "Adduct", "Cosine", "SharedPeaks", "Quality"};
//            String[] ofInterest = new String[]{"#Scan#", "INCHI", "Smiles", "Adduct", "MQScore", "SharedPeaks", "Quality"};
//            int[] indices = new int[ofInterest.length];
//            for (int i = 0; i < ofInterest.length; i++) {
//                int idx = arrayFind(header, ofInterest[i]);
//                if (idx<0){
//                    int[] more = arrayFindSimilar(header, ofInterest[i]);
//                    if (more.length!=1) throw new RuntimeException("Cannot parse spectral library hits file. Column "+ofInterest[i]+" not found.");
//                    else idx = more[0];
//                }
//                indices[i] = idx;
//            }
//
//
//            List<LibraryHit> libraryHits = new ArrayList<>();
//            for (String line : lines) {
//                try {
//                    String[] cols = line.split("\t");
//                    final int scanNumber = Integer.parseInt(cols[indices[0]]);
//                    final String featureId = featureIDs.get(scanNumber-1); //starting with 1!
//
//                    final Ms2Experiment experiment = experimentMap.get(featureId);
//                    final MolecularFormula formula = getFormulaFromStructure(cols[indices[1]].replace("\"", ""), cols[indices[2]].replace("\"", ""));
//
//                    if (formula==null){
//                        System.err.println("cannot parse molecular formula of library hit #SCAN# "+scanNumber);
//                        continue;
//                    }
//
//                    final String structure = (isInchi(cols[indices[1]]) ? cols[indices[1]] : cols[indices[2]]);
//                    final PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(cols[indices[3]]);
//                    final double cosine = Double.parseDouble(cols[indices[4]]);
//                    final int sharedPeaks = Integer.parseInt(cols[indices[5]]);
//                    final LibraryHitQuality quality = LibraryHitQuality.valueOf(cols[indices[6]]);
//                    LibraryHit libraryHit = new LibraryHit(experiment, formula, structure, ionType, cosine, sharedPeaks, quality);
//                    libraryHits.add(libraryHit);
//                } catch (Exception e) {
//                    System.err.println("Warning: Cannot parse library hit. Reason: "+ e.getMessage());
//                }
//
//            }
//
//            return libraryHits;
//        } catch (Exception e){
//            throw new IOException("cannot parse library hits. Reason "+e.getMessage());
//        }
//
//    }

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


    public <C extends Candidate>  void guessIonizationAndRemove(Map<String, List<C>> candidateMap, PrecursorIonType[] ionTypes){
        List<String> idList = new ArrayList<>(candidateMap.keySet());
        Sirius sirius = new Sirius();
        for (String id : idList) {
            List<C> candidates = candidateMap.get(id);
            Ms2Experiment experiment = candidates.get(0).getExperiment();
            PrecursorIonType[] guessed = sirius.guessIonization(experiment, ionTypes).getGuessedIonTypes();

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


    private int[] statisticsOfKnownCompounds(List<Scored<FragmentsCandidate>[][]> listOfResults, String ids[], Set<String> evaluationIDs, Map<String, LibraryHit> correctHitsMap){
        Set<MolecularFormula>[] bestMFallRuns = new Set[ids.length];
        initialize(bestMFallRuns);

        int correctId = 0;
        int wrongId = 0;
        for (Scored<FragmentsCandidate>[][] result : listOfResults) {
            for (int i = 0; i < result.length; i++) {
                Scored<FragmentsCandidate>[] candidatesScored = result[i];
                String id = ids[i];

                bestMFallRuns[i].add(candidatesScored[0].getCandidate().getFormula());

                if (correctHitsMap.containsKey(id) && evaluationIDs.contains(id)){
                    if (candidatesScored[0].getCandidate().isCorrect())  ++correctId;
                    else ++wrongId;

                }
            }
        }
        System.out.println("on average correct identifications: "+correctId+" ("+(1d*correctId/(correctId+wrongId))+") of "+(correctId+wrongId));

        int numberOfUnambiguous = 0;
        int numberOfAmbiguous = 0;
        for (Set<MolecularFormula> bestMFs : bestMFallRuns) {
            if (bestMFs.size()==1) ++numberOfUnambiguous;
            else ++numberOfAmbiguous;
        }
        System.out.println(numberOfAmbiguous+" ("+(1d*numberOfAmbiguous/(numberOfAmbiguous+numberOfUnambiguous))+") of "+(numberOfAmbiguous+numberOfUnambiguous)+" of compounds have different best hit over all runs");

        return new int[]{correctId, wrongId};
    }

    private void initialize(Set<MolecularFormula>[] setArray){
        for (int i = 0; i < setArray.length; i++) {
            setArray[i] = new HashSet<>();
        }
    }


    private int[] statisticsOfKnownCompounds(Scored<FragmentsCandidate>[][] result, String ids[], Set<String> evaluationIDs, Map<String, LibraryHit> correctHitsMap, Graph<FragmentsCandidate> graph){
        return statisticsOfKnownCompounds(result, ids, null, evaluationIDs, correctHitsMap, graph, false);
    }

    /**
     * print ranks of compounds
     * @param result sorted!
     * @param ids
     * @param correctHitsMap
     * @return
     */
    private int[] statisticsOfKnownCompounds(Scored<FragmentsCandidate>[][] result, String ids[], Map<String, ResultInformation> zodiacResults, Set<String> evaluationIDs, Map<String, LibraryHit> correctHitsMap, Graph<FragmentsCandidate> graph, boolean onlyGoodQuality){
        int bestIsDummyCount = 0;
        int total = 0;
        int badQualityCounter = 0;
        for (int i = 0; i < result.length; i++) {
            Scored<FragmentsCandidate>[] candidatesScored = result[i];
            if (candidatesScored.length==0) continue;
            if (onlyGoodQuality) {
                //todo problem: this is only the ones with candidates
                CompoundQuality quality = zodiacResults.get(ids[i]).compoundQuality;
                if (!quality.isGoodQuality()){
                    ++badQualityCounter;
                    continue;
                }
            }
            ++total;
            if (DummyFragmentCandidate.isDummy(candidatesScored[0].getCandidate())){
                ++bestIsDummyCount;
            }


        }
        System.out.println("used dummies: "+bestIsDummyCount + " of " + total);
        if (onlyGoodQuality) System.out.println("not-good-quality: "+badQualityCounter);

        int correctId = 0;
        int wrongId = 0;
        for (int i = 0; i < result.length; i++) {
            Scored<FragmentsCandidate>[] candidatesScored = result[i];
            String id = ids[i];

            if (onlyGoodQuality && !zodiacResults.get(ids[i]).compoundQuality.isGoodQuality()) continue;

            if (correctHitsMap.containsKey(id) && evaluationIDs.contains(id)){
                MolecularFormula correct = null;
                MolecularFormula library = correctHitsMap.get(id).getMolecularFormula();
                int pos = 1;
                int correctPos = Integer.MAX_VALUE;
                int dummyPos = Integer.MAX_VALUE;
                for (Scored<FragmentsCandidate> candidateScored : candidatesScored) {
                    if (candidateScored.getCandidate().isCorrect()) {
                        correctPos = pos;
                        correct = candidateScored.getCandidate().getFormula();
                    } else if (DummyFragmentCandidate.isDummy(candidateScored.getCandidate())){
                        dummyPos = pos;
                    }
                    pos++;
                }

                int numberOfConnectedCompoundsBest=-1;
                int maxNumberOfConnectedCompounds=-1;
                if (graph!=null) {
                    numberOfConnectedCompoundsBest = graph.getNumberOfConnectedCompounds(i, candidatesScored[0].getCandidate());
                    maxNumberOfConnectedCompounds = graph.getMaxNumberOfConnectedCompounds(i);
                }

                if (correctPos==1) ++correctId;
                else ++wrongId;
                if (dummyPos==1){
                    System.out.println(id + " best is dummy.");
                }
                if (correctPos>candidatesScored.length){
                    System.out.println(id + " not found | best has "+candidatesScored[0].getScore());
                } else {
                    System.out.println(id + " found at " + correctPos + " (" + candidatesScored[correctPos-1].getScore()+ ") of " + candidatesScored.length
                            +"  | best has "+candidatesScored[0].getScore()
                            +" | best: "+candidatesScored[0].getCandidate().getFormula()
                            +" | correct: "+correct
                            +" | library: "+library
                            +(graph==null?"":(" || "+numberOfConnectedCompoundsBest+" | "+maxNumberOfConnectedCompounds))
                    );
                }
            }
        }
        System.out.println(correctId+" correct out of "+(correctId+wrongId));
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
                MolecularFormula correct = correctHitsMap.get(id).getMolecularFormula();
                int pos = 1;
                for (FragmentsCandidate candidate : candidates) {
                    if (candidate.isCorrect()) {
                        break;
                    }
                    pos++;
                }
                if (pos>candidates.length){
//                    System.out.println(id + " ( "+candidates[0].getExperiment().getIonMass()+" mz) not found");
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

    public static Map<String, List<FragmentsCandidate>> parseMFCandidatesFromWorkspace(Path workspaceDir, Path originalMsInformation) throws IOException {
        return parseMFCandidatesFromWorkspace(workspaceDir, originalMsInformation, Integer.MAX_VALUE);
    }

    /**
     *
     * @param workspaceDir
     * @param originalMsInformation e.g. the mgf file used as input for Sirius
     * @return
     * @throws IOException
     */
    public static Map<String, List<FragmentsCandidate>> parseMFCandidatesFromWorkspace(Path workspaceDir, Path originalMsInformation, int maxCandidates) throws IOException {
        File file = workspaceDir.toFile();
        final List<ExperimentResult> results = new ArrayList<>();


        final DirectoryReader.ReadingEnvironment env;
        if (file.isDirectory()) {
            env = new SiriusFileReader(file);
        } else {
            env = new SiriusWorkspaceReader(file);
        }
        final DirectoryReader reader = new DirectoryReader(env);

        while (reader.hasNext()) {
            final ExperimentResult result = reader.next();
            results.add(result);
        }

        final MsExperimentParser parser = new MsExperimentParser();
        List<Ms2Experiment> rawExperiments = parser.getParser(originalMsInformation.toFile()).parseFromFile(originalMsInformation.toFile());
        Map<String, List<Ms2Experiment>> nameToExperiment = new HashMap<>();
        for (Ms2Experiment rawExperiment : rawExperiments) {
            String name = rawExperiment.getName();
            List<Ms2Experiment> experimentList = nameToExperiment.get(name);
            if (experimentList==null){
                //should always be the case?
                experimentList = new ArrayList<>();
                nameToExperiment.put(name, experimentList);
            }
            experimentList.add(rawExperiment);
        }

        List<Ms2Experiment> allExperiments = new ArrayList<>();
        for (ExperimentResult result : results) {
            MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(result.getExperiment());
            String name = mutableMs2Experiment.getName();
            List<Ms2Experiment> experimentList = nameToExperiment.get(name);
            Ms2Experiment experiment2 = null;
            if (experimentList.size()==1){
                experiment2 = experimentList.get(0);
            } else if (experimentList.size()>1){
                for (Ms2Experiment experiment : experimentList) {
                    if (Math.abs(mutableMs2Experiment.getIonMass()-experiment.getIonMass())<1e-15){
                        experiment2 = experiment;
                        break;
                    }
                }
            }
            if (experiment2==null){
                LOG.error("cannot find original MS data for compound in sirius workspace: "+mutableMs2Experiment.getName());
            } else {
                mutableMs2Experiment.setMergedMs1Spectrum(experiment2.getMergedMs1Spectrum());
                mutableMs2Experiment.setMs1Spectra(experiment2.getMs1Spectra());
                mutableMs2Experiment.setMs2Spectra(experiment2.getMs2Spectra());
            }
            allExperiments.add(mutableMs2Experiment);

        }
        Ms2Dataset dataset = new MutableMs2Dataset(allExperiments, "default", Double.NaN, (new Sirius("default")).getMs2Analyzer().getDefaultProfile());
        Ms2DatasetPreprocessor preprocessor = new Ms2DatasetPreprocessor(true);
        dataset = preprocessor.preprocess(dataset);
        allExperiments = dataset.getExperiments();

//        //changed: test just by chance assign compound bad quality
//        for (Ms2Experiment experiment : allExperiments) {
//            if (Math.random()<0.1){
//                if (CompoundQuality.isNotBadQuality(experiment)){
//                    CompoundQuality.setProperty(experiment, SpectrumProperty.PoorlyExplained);
//                    System.out.println("make bad quality: "+experiment.getName());
//                }
//            }
//        }


        int pos = 0;
        Map<String, List<FragmentsCandidate>> candidatesMap = new HashMap<>();
        for (ExperimentResult result : results) {
            List<FTree> trees = new ArrayList<>();
            Ms2Experiment experiment = allExperiments.get(pos++); //use experiments with assigned quality
            for (IdentificationResult identificationResult : result.getResults()) {
//                trees.add(identificationResult.getRawTree());
                trees.add(identificationResult.getResolvedTree()); //todo use rawTree or resolvedTree?!
            }


            if (!atLeastOneTreeExplainsSomeIntensity(trees, 0.5)){
                CompoundQuality.setProperty(experiment, SpectrumProperty.PoorlyExplained);
            }
            if (!atLeastOneTreeExplainsSomePeaks(trees, 3)){
                CompoundQuality.setProperty(experiment, SpectrumProperty.PoorlyExplained);
            }

            List<FragmentsCandidate> candidates = FragmentsCandidate.createAllCandidateInstances(trees, experiment);

            Collections.sort(candidates);
            if (candidates.size() > maxCandidates) candidates = candidates.subList(0, maxCandidates);
            if (candidates.size() > 0) candidatesMap.put(experiment.getName(), candidates);

        }

        return candidatesMap;
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
            if (experimentMap.containsKey(name)) throw new RuntimeException("experiment name duplicate: "+name);
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


                        FTree tree = null;
                        try {
                            tree = new GenericParser<FTree>(new FTJsonReader()).parseFromFile(treePath.toFile()).get(0);
                        } catch (RuntimeException e) {
                            System.out.println("cannot read tree "+treePath.getFileName().toString());
                            continue;
                        } catch (IOException e){
                            throw new RuntimeException(e);
                        }


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


                        FTree tree = null;
                        try {
                            tree = new GenericParser<FTree>(new FTJsonReader()).parseFromFile(treePath.toFile()).get(0);
                        } catch (RuntimeException e) {
                            System.out.println("cannot read tree "+treePath.getFileName().toString());
                            continue;
                        } catch (IOException e){
                            throw new RuntimeException(e);
                        }


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
//
//        System.out.println("keys size "+keys.size());
//        System.out.println("all compounds: "+experiments.size()+" | used compounds: "+listMap.size());

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


    public void writeMFNetworkToDir(Path outputDir, Graph<FragmentsCandidate> graph, EdgeScorer[] edgeScorers ) throws IOException {
        if (!Files.exists(outputDir)) Files.createDirectory(outputDir);

        BufferedWriter edgeWriter = Files.newBufferedWriter(outputDir.resolve("edges.csv"));
        BufferedWriter nodesWriter = Files.newBufferedWriter(outputDir.resolve("nodesInfo.csv"));


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


    /**
     * @param ids
     * @param explanationsMap
     * @return best initial formulas with RELATIVE SCORE!!
     */
    public static Scored<FragmentsCandidate>[][] getBestInitialAssignments(String[] ids, Map<String, List<FragmentsCandidate>> explanationsMap) {
        Scored<FragmentsCandidate>[][] array = new Scored[ids.length][];
        for (int i = 0; i < ids.length; i++) {
            List<FragmentsCandidate> smfList = explanationsMap.get(ids[i]);
            Scored<FragmentsCandidate>[] smfarray = new Scored[smfList.size()];

            if (true) {
                //normalize
                double max = Double.NEGATIVE_INFINITY;
                for (int j = 0; j < smfList.size(); ++j) {
                    final Candidate candidate = smfList.get(j);
                    double score = candidate.getScore();
                    if (score > max) {
                        max = score;
                    }
                }

                double sum = 0.0D;
                double[] scores = new double[smfList.size()];

                for (int j = 0; j < smfList.size(); ++j) {
                    final Candidate candidate = smfList.get(j);
                    double expS = Math.exp(1d * (candidate.getScore() - max));
                    sum += expS;
                    scores[j] = expS;
                }

                for (int j = 0; j < smfList.size(); ++j) {
                    smfarray[j] = new Scored<FragmentsCandidate>(smfList.get(j), scores[j] / sum);
                }

            } else {
                for (int j = 0; j < smfarray.length; j++) {
                    smfarray[j] = new Scored<FragmentsCandidate>(smfList.get(j), smfList.get(j).getScore());
                }
            }

            Arrays.sort(smfarray, Collections.<Scored<FragmentsCandidate>>reverseOrder());
            array[i] = smfarray;
        }
        return array;
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
