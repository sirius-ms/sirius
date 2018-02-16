package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.GibbsSampling.GibbsSamplerMain;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.ExponentialDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.EdgeScorings;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Ms2DatasetPreprocessor;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.projectspace.DirectoryReader;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import de.unijena.bioinf.sirius.projectspace.SiriusFileReader;
import de.unijena.bioinf.sirius.projectspace.SiriusWorkspaceReader;
import org.apache.commons.math3.analysis.function.Exp;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;


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

        //todo problem with name/id simplification
        //todo force name = id
        maxCandidates = (options.getNumberOfCandidates() == null ? Integer.MAX_VALUE : options.getNumberOfCandidates());
        Path originalSpectraPath = Paths.get(options.getSpectraFile());
        try {
            //todo For the official release zodiac should become a job an create subjobs in the jobmanager for multithreading
//            int workerCount = PropertyManager.getNumberOfCores();
            int workerCount = options.getNumOfCores()>0 ? options.getNumOfCores() : (new SystemInfo()).getHardware().getProcessor().getPhysicalProcessorCount()-1;
            PropertyManager.PROPERTIES.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(workerCount));


            //create output dir
            if (Files.exists(outputPath)){
                if (!Files.isDirectory(outputPath)){
                    LOG.error("specified output path must be a directory.");
                    return;
                }
            } else {
                Files.createDirectories(outputPath);
            }


            List<ExperimentResult> experimentResults = newLoad(workSpacePath.toFile());
            //tdo reads original experiments twice!
            experimentResults = updateQuality(experimentResults, originalSpectraPath);


            List<LibraryHit> anchors = (libraryHitsFile==null)?null:GibbsSamplerMain.parseLibraryHits(libraryHitsFile, originalSpectraPath); //only specific GNPS format


            NodeScorer[] nodeScorers;
            boolean useLibraryHits = (libraryHitsFile != null);
            double libraryScore = 1d;//todo which lambda to use!?
            if (useLibraryHits) {
                Reaction[] reactions = GibbsSamplerMain.parseReactions(1);
                Set<MolecularFormula> netSingleReactionDiffs = new HashSet<>();
                for (Reaction reaction : reactions) {
                    netSingleReactionDiffs.add(reaction.netChange());
                }
                nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d), new LibraryHitScorer(libraryScore, 0.3, netSingleReactionDiffs)};
            } else {
                nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d)};
            }


            EdgeFilter edgeFilter = null;

            if (options.getThresholdFilter() > 0.0D && (options.getLocalFilter() > 0.0D || options.getMinLocalConnections() > 0d)){
                int numberOfCandidates = Math.max(options.getLocalFilter(),1);
                int numberOfConnections = options.getMinLocalConnections()>0?options.getMinLocalConnections():10;
                edgeFilter = new EdgeThresholdMinConnectionsFilter(options.getThresholdFilter(), numberOfCandidates, numberOfConnections);
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



            de.unijena.bioinf.GibbsSampling.Zodiac zodiac = new de.unijena.bioinf.GibbsSampling.Zodiac(experimentResults, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates);


            ZodiacResultsWithClusters zodiacResult = zodiac.compute(options.getIterationSteps(), options.getBurnInSteps(), options.getSeparateRuns());
            CompoundResult<FragmentsCandidate>[] result = zodiacResult.getResults();

            Graph<FragmentsCandidate> graph = zodiacResult.getGraph(); //update, get complete graph

            String[] ids = zodiacResult.getIds();


            Map<String, ExperimentResult> experimentResultMap = createMap(experimentResults);
            Map<String, String[]> representativeToCluster = zodiacResult.getRepresentativeToCluster();
            Scored<IdentificationResult>[] bestInitial = bestInitial(ids, experimentResultMap);
            writeZodiacOutput(ids, bestInitial, result, outputPath.resolve("zodiac_summary.csv"));
            writeClusters(representativeToCluster, outputPath.resolve("clusters.csv"));
            writeSpectra(ids, result, experimentResultMap, outputPath);

        } catch (IOException e) {
            LOG.error("Error while running ZODIAC: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private List<ExperimentResult> updateQuality(List<ExperimentResult> experimentResults, Path originalMsInformation) throws IOException {
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
        for (ExperimentResult result : experimentResults) {
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

        int pos = 0;
        List<ExperimentResult> newExperimentResults = new ArrayList<>();
        for (ExperimentResult result : experimentResults) {
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


            newExperimentResults.add(new ExperimentResult(experiment, result.getResults()));
        }

        return newExperimentResults;
    }

    public static boolean atLeastOneTreeExplainsSomeIntensity(List<FTree> trees, double threshold){
        for (FTree tree : trees) {
            final double intensity = tree.getAnnotationOrThrow(TreeScoring.class).getExplainedIntensity();
            if (intensity>threshold) return true;
        }
        return false;
    }

    public static boolean atLeastOneTreeExplainsSomePeaks(List<FTree> trees, int threshold){
        for (FTree tree : trees) {
            if (tree.numberOfVertices()>=threshold) return true;
        }
        return false;
    }

    private final static int NUMBER_OF_HITS = Integer.MAX_VALUE;
    private final static String SEP = "\t";
    public static void writeZodiacOutput(String[] ids, Scored<IdentificationResult>[] initial, CompoundResult<FragmentsCandidate>[] result, Path outputPath) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputPath, Charset.defaultCharset());
        writer.write("id" + SEP + "SiriusMF" + SEP + "SiriusScore" + SEP + "connectedCompounds" + SEP + "ZodiacMF" + SEP + "ZodiacScore");
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            final String id2 = result[i].getId();

            if (!id.equals(id2)) throw new RuntimeException("different ids: "+id+" vs "+id2);

            final String siriusMF = initial[i].getCandidate().getMolecularFormula().formatByHill();
            final double siriusScore = initial[i].getScore();

            int connections = result[i].getAnnotationOrThrow(Connectivity.class).getNumberOfConnectedCompounds();
            String summeryLine = createSummaryLine(id, siriusMF, siriusScore, connections, result[i].getCandidates());
            writer.write("\n");
            writer.write(summeryLine);
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

    private Scored<IdentificationResult>[] bestInitial(String[] ids, Map<String, ExperimentResult> experimentResultMap){
        Scored<IdentificationResult>[] best = new Scored[ids.length];
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            ExperimentResult result = experimentResultMap.get(id);


            //normalize
            double max = Double.NEGATIVE_INFINITY;
            for (IdentificationResult identificationResult : result.getResults()) {
                double score = identificationResult.getScore();
                if (score > max) {
                    max = score;
                }


            }

            double sum = 0.0D;
            double[] scores = new double[result.getResults().size()];
            for (int j = 0; j < result.getResults().size(); ++j) {
                final IdentificationResult identificationResult = result.getResults().get(j);
                double expS = Math.exp(1d * (identificationResult.getScore() - max));
                sum += expS;
                scores[j] = expS;
            }

            //save best with probability
            best[i] = new Scored<>(result.getResults().get(0), scores[0]/sum);

        }
        return best;
    }

    private Map<String, ExperimentResult> createMap(List<ExperimentResult> experimentResults){
        Map<String, ExperimentResult> map = new HashMap<>();
        for (ExperimentResult experimentResult : experimentResults) {
            map.put(experimentResult.getExperimentName(), experimentResult);
        }
        return map;
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

    private static void writeClusters(Map<String, String[]> representativeToCluster, Path outputPath) throws IOException {
        final BufferedWriter writer = Files.newBufferedWriter(outputPath, Charset.defaultCharset());
        writer.write("representative\tcluster_ids");
        for (Map.Entry<String, String[]> stringEntry : representativeToCluster.entrySet()) {
            String repId = stringEntry.getKey();
            String[] ids = stringEntry.getValue();
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(repId);
            for (String id : ids) {
                joiner.add(id);
            }
            writer.write("\n");
            writer.write(joiner.toString());

        }
        writer.close();
    }


    /*
    experimentMap necessary since FragmentsCandidate might be the cluster representative.
     */
    private static void writeSpectra(String[] ids, CompoundResult<FragmentsCandidate>[] result, Map<String, ExperimentResult> experimentMap, Path outputPath) throws IOException {
        for (int i = 0; i < ids.length; i++) {
            final Scored<FragmentsCandidate>[] currentResults = result[i].getCandidates();
            final Scored<FragmentsCandidate> bestResult = currentResults[0];

            if (DummyFragmentCandidate.isDummy(bestResult.getCandidate())) continue;

            final String id = ids[i];
            MutableMs2Experiment experiment = new MutableMs2Experiment(experimentMap.get(id).getExperiment());
            experiment.setMolecularFormula(bestResult.getCandidate().getFormula());
            experiment.setPrecursorIonType(bestResult.getCandidate().getIonType());
            Path file = outputPath.resolve(Integer.toString(i + 1) + "_" + id + ".ms");
            final BufferedWriter writer = Files.newBufferedWriter(file, Charset.defaultCharset());
            new JenaMsWriter().write(writer, experiment);
            writer.close();

        }

    }




}
