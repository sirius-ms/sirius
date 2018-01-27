package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.GibbsSamplerMain;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.ExponentialDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.EdgeScorings;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.projectspace.DirectoryReader;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import de.unijena.bioinf.sirius.projectspace.SiriusFileReader;
import de.unijena.bioinf.sirius.projectspace.SiriusWorkspaceReader;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * Created by ge28quv on 18/05/17.
 */
public class Zodiac {
    private static final  org.slf4j.Logger LOG = LoggerFactory.getLogger(Zodiac.class);
    private Path outputPath;
    private Path libraryHitsFile;
    private Path workSpacePath;
    private ZodiacOptions options;
    private int maxCandidates;


    public Zodiac(ZodiacOptions options) {
        this.workSpacePath = Paths.get(options.getSirius());
        this.libraryHitsFile = (options.getLibraryHitsFile() == null ? null : Paths.get(options.getLibraryHitsFile()));
        this.outputPath = Paths.get(options.getOutput());
        this.options = options;
    }


    public void run() {
        maxCandidates = (options.getNumberOfCandidates() == null ? Integer.MAX_VALUE : options.getNumberOfCandidates());
        Path originalSpectraPath = Paths.get(options.getSpectraFile());
        try {
            //todo For the official release zodiac should become a job an create subjobs in the jobmanager for multithreading
//            int workerCount = PropertyManager.getNumberOfCores();
            int workerCount = options.getNumOfCores()>0 ? options.getNumOfCores() : (new SystemInfo()).getHardware().getProcessor().getPhysicalProcessorCount()-1;

//        //reactions
            Reaction[] reactions = GibbsSamplerMain.parseReactions(1);
            Set<MolecularFormula> netSingleReactionDiffs = new HashSet<>();
            for (Reaction reaction : reactions) {
                netSingleReactionDiffs.add(reaction.netChange());
            }


            LOG.info("Read sirius input");
//            List<ExperimentResult> input = newLoad(workSpacePath.toFile());


            Map<String, List<FragmentsCandidate>> candidatesMap = GibbsSamplerMain.parseMFCandidatesFromWorkspace(workSpacePath, originalSpectraPath);

            LOG.info("number of compounds: " + candidatesMap.size());


            if (libraryHitsFile != null)
                GibbsSamplerMain.parseLibraryHits(libraryHitsFile, originalSpectraPath, candidatesMap);
            setKnownCompounds(candidatesMap, netSingleReactionDiffs);

            GibbsSamplerMain.addNotExplainableDummy(candidatesMap, maxCandidates);


            String[] ids = getIdsOfCompoundsWithCandidates(candidatesMap);
            FragmentsCandidate[][] candidatesArray = new FragmentsCandidate[ids.length][];

            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];
                candidatesArray[i] = candidatesMap.get(id).toArray(new FragmentsCandidate[0]);
            }


            NodeScorer[] nodeScorers;
            boolean useLibraryHits = (libraryHitsFile != null);
            double libraryScore = 1d;//todo which lambda to use!?
            if (useLibraryHits) {
                nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d), new LibraryHitScorer(libraryScore, 0.3, netSingleReactionDiffs)};
            } else {
                nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d)};
            }


            EdgeFilter edgeFilter = null;

            if (options.getThresholdFilter() > 0.0D && options.getLocalFilter() > 0.0D) {
                edgeFilter = new EdgeThresholdMinConnectionsFilter(options.getThresholdFilter(), options.getLocalFilter(), options.getMinLocalConnections());
            } else if (options.getThresholdFilter() > 0.0D) {
                edgeFilter = new EdgeThresholdFilter(options.getThresholdFilter());
            } else if (options.getLocalFilter() > 0.0D) {
                edgeFilter = new LocalEdgeFilter(options.getLocalFilter());
            }
            if (edgeFilter == null) {
                edgeFilter = new EdgeThresholdFilter(0);
            }


            boolean estimateByMedian = true;
            ScoreProbabilityDistribution probabilityDistribution = null;
            if (options.getProbabilityDistribution().equals(EdgeScorings.exponential)) {
                probabilityDistribution = new ExponentialDistribution(estimateByMedian);
            } else if (options.getProbabilityDistribution().equals(EdgeScorings.lognormal)) {
                probabilityDistribution = new LogNormalDistribution(estimateByMedian);
            } else {
                System.err.println("probability distribution is unknwown. Use 'lognormal' or 'exponential'.");
            }



            double minimumOverlap = 0.0D;
            ScoreProbabilityDistributionEstimator commonFragmentAndLossScorer = new ScoreProbabilityDistributionEstimator(new CommonFragmentAndLossScorer(minimumOverlap), probabilityDistribution, options.getThresholdFilter());
            EdgeScorer[] edgeScorers = new EdgeScorer[]{commonFragmentAndLossScorer};

            TwoPhaseGibbsSampling<FragmentsCandidate> twoPhaseGibbsSampling = new TwoPhaseGibbsSampling<>(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, workerCount, options.getSeparateRuns());

            //validate Graph
            Graph<FragmentsCandidate> graph = twoPhaseGibbsSampling.getGraph();

            GraphValidationMessage validationMessage = graph.validate();

            if (validationMessage.isError()) {
                LoggerFactory.getLogger(this.getClass()).error(validationMessage.getMessage());
                return;
            } else if (validationMessage.isWarning()) {
                LoggerFactory.getLogger(this.getClass()).warn(validationMessage.getMessage());
            }

            twoPhaseGibbsSampling.run(options.getIterationSteps(), options.getBurnInSteps());
            graph = twoPhaseGibbsSampling.getGraph(); //update, get complete graph

            Scored<FragmentsCandidate>[][] bestInitial = GibbsSamplerMain.getBestInitialAssignments(ids, candidatesMap);

            Scored<FragmentsCandidate>[][] result = twoPhaseGibbsSampling.getChosenFormulas();

            GibbsSamplerMain.writeZodiacOutput(ids, bestInitial, result, graph, outputPath.resolve("zodiac_summary.csv"));
            writeSpectra(ids, result, outputPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected static List<ExperimentResult> newLoad(File file) throws IOException {
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
        return results;
    }


    private static void writeSpectra(String[] ids, Scored<FragmentsCandidate>[][] result, Path outputPath) throws IOException {
        for (int i = 0; i < ids.length; i++) {

            final String id = ids[i];

            final Scored<FragmentsCandidate>[] currentResults = result[i];
            final Scored<FragmentsCandidate> bestResult = currentResults[0];

            if (DummyFragmentCandidate.isDummy(bestResult.getCandidate())) continue;

            MutableMs2Experiment experiment = new MutableMs2Experiment(bestResult.getCandidate().getExperiment());
            experiment.setMolecularFormula(bestResult.getCandidate().getFormula());
            experiment.setPrecursorIonType(bestResult.getCandidate().getIonType());
            Path file = outputPath.resolve(Integer.toString(i + 1) + "_" + id + ".ms");
            final BufferedWriter writer = Files.newBufferedWriter(file, Charset.defaultCharset());
            new JenaMsWriter().write(writer, experiment);
            writer.close();

        }

    }

//    private final static String SEP = "\t";
//    private final static int NUMBER_OF_HITS = Integer.MAX_VALUE;
//
//    private static void writeZodiacOutput(String[] ids, Scored<FragmentsCandidate>[][] initial, Scored<FragmentsCandidate>[][] result, Graph<FragmentsCandidate> graph, Path outputPath) throws IOException {
//        int[] connectingPeaks = graph.getMaxConnectionCounts();
//        String[] ids2 = graph.getIds();
//
//        BufferedWriter writer = Files.newBufferedWriter(outputPath, Charset.defaultCharset());
//        //changed connectingPeaks -> connectedCompounds
//        writer.write("id" + SEP + "SiriusMF" + SEP + "SiriusScore" + SEP + "connectedCompounds" + SEP + "ZodiacMF" + SEP + "ZodiacScore");
//        for (int i = 0; i < ids.length; i++) {
//            final StringBuffer buffer = new StringBuffer();
//
//            final String id = ids[i];
//            final String siriusMF = initial[i][0].getCandidate().getFormula().formatByHill();
//            final double siriusScore = initial[i][0].getScore();
//
//            final int connections = connectingPeaks[i];
//            if (!id.equals(ids2[i])) throw new RuntimeException("different ids");
//
//
//            buffer.append(id);
//            buffer.append(SEP);
//            buffer.append(siriusMF);
//            buffer.append(SEP);
//            buffer.append(Double.toString(siriusScore));
//            buffer.append(SEP);
//            buffer.append(connections);
//
//            final Scored<FragmentsCandidate>[] currentResults = result[i];
//            for (int j = 0; j < Math.min(currentResults.length, NUMBER_OF_HITS); j++) {
//                Scored<FragmentsCandidate> currentResult = currentResults[j];
//                final String mf = currentResult.getCandidate().getFormula().formatByHill();
//                final double score = currentResult.getScore();
//
//                if (score <= 0) break; //don't write MF with 0 probability
//
//                buffer.append(SEP);
//                buffer.append(mf);
//                buffer.append(SEP);
//                buffer.append(Double.toString(score));
//            }
//
//            writer.write("\n");
//            writer.write(buffer.toString());
//
//        }
//
//        writer.close();
//
//    }

    public static Map<String, List<FragmentsCandidate>> parseMFCandidates(List<ExperimentResult> experimentResults, int maxCandidates) throws IOException {
        final Map<String, List<FragmentsCandidate>> listMap = new HashMap<>();
        for (ExperimentResult experimentResult : experimentResults) {
            Ms2Experiment experiment = experimentResult.getExperiment();
            List<IdentificationResult> identificationResults = experimentResult.getResults();
            //todo beautify trees!!!!
            List<FTree> trees = new ArrayList<>();
            for (IdentificationResult identificationResult : identificationResults) {
                trees.add(identificationResult.getResolvedTree());
            }
            List<FragmentsCandidate> candidates = FragmentsCandidate.createAllCandidateInstances(trees, experiment);
            Collections.sort(candidates);
            if (candidates.size() > maxCandidates) candidates = candidates.subList(0, maxCandidates);

            if (candidates.size() > 0) listMap.put(experiment.getName(), candidates);


        }


        return listMap;
    }

    private void setKnownCompounds(Map<String, List<FragmentsCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences) {
        Set<String> ids = candidatesMap.keySet();
        for (String id : ids) {
            final List<FragmentsCandidate> candidateList = candidatesMap.get(id);
            if (!candidateList.get(0).hasLibraryHit()) continue;

            final LibraryHit libraryHit = candidateList.get(0).getLibraryHit();

            //todo at least 5 peaks match, no cosine threshold?
            if (libraryHit.getSharedPeaks() < 5) continue;

            MolecularFormula correctMF = libraryHit.getMolecularFormula();
            List<FragmentsCandidate> candidates = candidatesMap.get(id);

            //todo does the ionization of library hit and compound have to match!?
            for (FragmentsCandidate candidate : candidates) {
                boolean matches = candidate.getFormula().equals(correctMF);
                if (!matches) {
                    MolecularFormula diff = candidate.getFormula().subtract(correctMF);
                    if (diff.getMass() < 0) diff = diff.negate();
                    matches = allowedDifferences.contains(diff);
                }
                if (matches) {
                    candidate.setCorrect(true);
                    System.out.println("Compound " + id + " has library hit. candidate MF is " + candidate.getFormula() + ". Library hit is " + correctMF);
                }
                candidate.setInTrainingSet(true);


            }
        }
    }

    private <C> String[] getIdsOfCompoundsWithCandidates(Map<String, List<C>> candidatesMap) {
        List<String> ids = new ArrayList<>();
//        for (ExperimentResult experimentResult : input) {
//            if (experimentResult.getExperiment()==null) continue;
//            final String id = experimentResult.getExperiment().getName();
//            if (candidatesMap.containsKey(id)) ids.add(id);
//        }
        //changed
        for (String key : candidatesMap.keySet()) {
            if (candidatesMap.get(key).size() > 0) ids.add(key);
        }
        return ids.toArray(new String[0]);
    }


    private <T> boolean arrayContains(T[] array, T object) {
        return this.arrayFind(array, object) >= 0;
    }

    private <T> int arrayFind(T[] array, T object) {
        for (int i = 0; i < array.length; ++i) {
            Object t = array[i];
            if (t.equals(object)) {
                return i;
            }
        }

        return -1;
    }


//    /**
//     * @param ids
//     * @param explanationsMap
//     * @return best initial formulas with RELATIVE SCORE!!
//     */
//    private static Scored<FragmentsCandidate>[][] getBestInitialAssignments(String[] ids, Map<String, List<FragmentsCandidate>> explanationsMap) {
//        Scored<FragmentsCandidate>[][] array = new Scored[ids.length][];
//        for (int i = 0; i < ids.length; i++) {
//            List<FragmentsCandidate> smfList = explanationsMap.get(ids[i]);
//            Scored<FragmentsCandidate>[] smfarray = new Scored[smfList.size()];
//
//            if (true) {
//                //normalize
//                double max = Double.NEGATIVE_INFINITY;
//                for (int j = 0; j < smfList.size(); ++j) {
//                    final Candidate candidate = smfList.get(j);
//                    double score = candidate.getScore();
//                    if (score > max) {
//                        max = score;
//                    }
//                }
//
//                double sum = 0.0D;
//                double[] scores = new double[smfList.size()];
//
//                for (int j = 0; j < smfList.size(); ++j) {
//                    final Candidate candidate = smfList.get(j);
//                    double expS = Math.exp(1d * (candidate.getScore() - max));
//                    sum += expS;
//                    scores[j] = expS;
//                }
//
//                for (int j = 0; j < smfList.size(); ++j) {
//                    smfarray[j] = new Scored<FragmentsCandidate>(smfList.get(j), scores[j] / sum);
//                }
//
//            } else {
//                for (int j = 0; j < smfarray.length; j++) {
//                    smfarray[j] = new Scored<FragmentsCandidate>(smfList.get(j), smfList.get(j).getScore());
//                }
//            }
//
//            Arrays.sort(smfarray, Collections.<Scored<FragmentsCandidate>>reverseOrder());
//            array[i] = smfarray;
//        }
//        return array;
//    }


}
