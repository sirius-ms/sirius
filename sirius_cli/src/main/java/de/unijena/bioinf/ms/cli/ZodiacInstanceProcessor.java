package de.unijena.bioinf.ms.cli;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.SimpleRectangularIsolationWindow;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.*;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.ChemistryBase.sirius.projectspace.Index;
import de.unijena.bioinf.GibbsSampling.ZodiacUtils;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.*;
import de.unijena.bioinf.GibbsSampling.model.scorer.*;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Ms2DatasetPreprocessor;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.projectspace.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ZodiacInstanceProcessor implements InstanceProcessor<ExperimentResult> {
    protected static Logger LOG = LoggerFactory.getLogger(ZodiacInstanceProcessor.class);
    protected ZodiacOptions options;
    protected FilenameFormatter filenameFormatter;

    private ScoreProbabilityDistribution probabilityDistribution;

    @Override
    public boolean setup() {

        //todo duplicate from Sirius options?
        filenameFormatter = null;
        if (options.getNamingConvention()!=null){
            String formatString = options.getNamingConvention();
            try {
                filenameFormatter = new StandardMSFilenameFormatter(formatString);
            } catch (ParseException e) {
                LOG.error("Cannot parse naming convention:\n" + e.getMessage(), e);
                return false;
            }
        } else {
            //default
            filenameFormatter = new StandardMSFilenameFormatter();
        }

        //create output dir
        if (options.getOutput()==null){
            LOG.error("Option is mandatory: --output -o value : output directory");
            return false;
        }
        Path outputPath = Paths.get(options.getOutput());
        if (Files.exists(outputPath)) {
            if (!Files.isDirectory(outputPath)) {
                LOG.error("specified output path must be a directory.");
                return false;
            }
        } else {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                LOG.error("Cannot create ZODIAC output directory: ", e);
            }
        }



        boolean estimateByMedian = true;
        probabilityDistribution = null;
        if (options.getProbabilityDistribution().equals(EdgeScorings.exponential)) {
            probabilityDistribution = new ExponentialDistribution(estimateByMedian);
        } else if (options.getProbabilityDistribution().equals(EdgeScorings.lognormal)) {
            probabilityDistribution = new LogNormalDistribution(estimateByMedian);
        } else {
            LOG.error("probability distribution is unknown. Use 'lognormal' or 'exponential'.");
            return false;
        }

        return true;
    }

    @Override
    public boolean validate() {
        return testOptions();
    }

    private boolean testOptions(){
        if (options.getOutput()==null){
            LOG.error("Option is mandatory: --output -o value : output directory");
            return false;
        }
        if (options.getSirius()==null){
            LOG.error("Option is mandatory: --sirius -s value : Sirius output directory or workspace. This is the input for Zodiac");
            return false;
        }
        return true;
    }

    @Override
    public void output(ExperimentResult result) {

    }


    //////////////////////////////////////////////////////////////////////////////////////////


    public ZodiacInstanceProcessor(ZodiacOptions options) {
        this.options = options;
    }



    //////////////////////////////////////////////////////////////////////////////////////////
    // compound quality //todo move somewhere else
    //////////////////////////////////////////////////////////////////////////////////////////


    protected List<ExperimentResult> updateQuality(List<ExperimentResult> experimentResults, double isolationWindowWidth, double isolationWindowShift, Path outputDir) throws IOException {
        List<Ms2Experiment> allExperiments = new ArrayList<>();
        for (ExperimentResult result : experimentResults) {
            allExperiments.add(result.getExperiment());
        }
        Ms2Dataset dataset = new MutableMs2Dataset(allExperiments, "default", Double.NaN, (new Sirius("default")).getMs2Analyzer().getDefaultProfile());
        Ms2DatasetPreprocessor preprocessor = new Ms2DatasetPreprocessor(true);

        if (options.getMedianNoiseIntensity()!=null) {
            double medianNoiseInt = options.getMedianNoiseIntensity();
            DatasetStatistics datasetStatistics= preprocessor.makeStatistics(dataset);
            double minMs1Intensity = datasetStatistics.getMinMs1Intensity();
            double maxMs1Intensity = datasetStatistics.getMaxMs1Intensity();
            double minMs2Intensity = datasetStatistics.getMinMs2Intensity();
            double maxMs2Intensity = datasetStatistics.getMaxMs2Intensity();
            double minMs2NoiseIntensity = medianNoiseInt;
            double maxMs2NoiseIntensity = medianNoiseInt;
            double meanMs2NoiseIntensity = medianNoiseInt;
            double medianMs2NoiseIntensity = medianNoiseInt;
            FixedDatasetStatistics fixedDatasetStatistics = new FixedDatasetStatistics(minMs1Intensity, maxMs1Intensity, minMs2Intensity, maxMs2Intensity, minMs2NoiseIntensity, maxMs2NoiseIntensity, meanMs2NoiseIntensity, medianMs2NoiseIntensity);
            ((MutableMs2Dataset) dataset).setDatasetStatistics(fixedDatasetStatistics);
        }

        List<QualityAnnotator> qualityAnnotators = new ArrayList<>();
        qualityAnnotators.add(new NoMs1PeakAnnotator(Ms2DatasetPreprocessor.FIND_MS1_PEAK_DEVIATION));
        qualityAnnotators.add(new FewPeaksAnnotator(Ms2DatasetPreprocessor.MIN_NUMBER_OF_PEAKS));
        qualityAnnotators.add(new LowIntensityAnnotator(Ms2DatasetPreprocessor.FIND_MS1_PEAK_DEVIATION, 0.01, Double.NaN));
//        qualityAnnotators.add(new NotMonoisotopicAnnotatorUsingIPA(Ms2DatasetPreprocessor.FIND_MS1_PEAK_DEVIATION));
        double max2ndMostIntenseRatio = 0.33;
//        double maxSummedIntensitiesRatio = 1.0;
        //changed
//        double maxSummedIntensitiesRatio = 0.66;
        double maxSummedIntensitiesRatio = 0.5;
        qualityAnnotators.add(new ChimericAnnotator(Ms2DatasetPreprocessor.FIND_MS1_PEAK_DEVIATION, max2ndMostIntenseRatio, maxSummedIntensitiesRatio));

        preprocessor.setQualityAnnotators(qualityAnnotators);


        if (isolationWindowWidth>0){
            double right = isolationWindowWidth/2d+isolationWindowShift;
            double left = -isolationWindowWidth/2d+isolationWindowShift;
            ((MutableMs2Dataset) dataset).setIsolationWindow(new SimpleRectangularIsolationWindow(left, right));
        }

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


//            if (!atLeastOneTreeExplainsSomeIntensity(trees, 0.5)){
            //changed
            if (!atLeastOneTreeExplainsSomeIntensity(trees, 0.8)){
                CompoundQuality.setProperty(experiment, SpectrumProperty.PoorlyExplained);
            }
            if (!atLeastOneTreeExplainsSomePeaks(trees, 5)){ //changed from 3
                CompoundQuality.setProperty(experiment, SpectrumProperty.PoorlyExplained);
            }


            newExperimentResults.add(new ExperimentResult(experiment, result.getResults()));
        }

        if (outputDir!=null){
            Path qualityPath = outputDir.resolve("spectra_quality.csv");
            Ms2Dataset dataset2 = new MutableMs2Dataset(allExperiments, "default", Double.NaN, (new Sirius("default")).getMs2Analyzer().getDefaultProfile());
            SpectrumProperty[] usedProperties = CompoundQuality.getUsedProperties(dataset2);
            preprocessor.writeExperimentInfos(dataset2, qualityPath, usedProperties);

//            if (dataset.getIsolationWindow()!=null){
//
//            }
            dataset.getIsolationWindow().writeIntensityRatiosToCsv(dataset, outputDir.resolve("isolation_window_intensities.csv"));

            Path summary = outputDir.resolve("data_summary.csv");
            System.out.println("write summary");
            preprocessor.writeDatasetSummary(dataset, summary);
            System.out.println("writing summary ended");
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

    //////////////////////////////////////////////////////////////////////////////////////////
    // create zodiac job
    //////////////////////////////////////////////////////////////////////////////////////////

    public ZodiacJJob makeZodiacJob(List<ExperimentResult> experimentResults){
        List<LibraryHit> anchors;
        Path libraryHitsFile = (options.getLibraryHitsFile() == null ? null : Paths.get(options.getLibraryHitsFile()));
        try {
            anchors = (libraryHitsFile == null) ? null : ZodiacUtils.parseLibraryHits(libraryHitsFile, experimentResults, LOG); //GNPS and in-house format
        } catch (IOException e) {
            LOG.error("Cannot load library hits from file.", e);
            return null;
        }
        return makeZodiacJob(experimentResults, anchors);
    }

    /**
     *
     * @param experimentResults
     * @param anchors may be null
     * @return
     */
    public ZodiacJJob makeZodiacJob(List<ExperimentResult> experimentResults, List<LibraryHit> anchors){
        //todo problem with name/id simplification
        //todo force name = id
        int maxCandidates = (options.getNumberOfCandidates() == null ? Integer.MAX_VALUE : options.getNumberOfCandidates());


        //todo For the official release zodiac should become a job an create subjobs in the jobmanager for multithreading
//      int workerCount = PropertyManager.getNumberOfCores();
        int workerCount = options.getNumOfCores() > 0 ? options.getNumOfCores() : (new SystemInfo()).getHardware().getProcessor().getPhysicalProcessorCount() - 1;
        PropertyManager.PROPERTIES.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(workerCount));



        if (options.isAllAnchorsHaveGoodQuality()){
            //todo this is a hack to use all library hits
            //lots of copy to ensure the "good quality" properties don't get passed to the next run.
            experimentResults = experimentResults.stream().map(e-> new ExperimentResult(new MutableMs2Experiment(e.getExperiment()), e.getResults())).collect(Collectors.toList());
            for (LibraryHit anchor : anchors) {
                for (ExperimentResult experimentResult : experimentResults) {
                    if (anchor.getQueryExperiment().getName().equals(experimentResult.getExperiment().getName())){
                        for (IdentificationResult result : experimentResult.getResults()) {
                            if (anchor.getMolecularFormula().subtract(result.getMolecularFormula()).equals(MolecularFormula.emptyFormula())){
                                experimentResult.getExperiment().setAnnotation(CompoundQuality.class, null);
                                CompoundQuality.setProperty(experimentResult.getExperiment(), SpectrumProperty.Good);
                            }

                        }
                    }
                }
            }
        }


        if (options.isFixAnchors()){
            //todo inefficient, just for eval?
            //remove additional candidates for compounds with anchors.
//            experimentResults = experimentResults.stream().map(e-> new ExperimentResult(new MutableMs2Experiment(e.getExperiment()), e.getResults())).collect(Collectors.toList());
            List<ExperimentResult> newExperimentResults = new ArrayList<>();

            Map<String, LibraryHit> anchorsMap = new HashMap<>();
            for (LibraryHit anchor : anchors) {
                if (anchorsMap.get(anchor.getQueryExperiment().getName())!=null) {
                    throw new IllegalArgumentException("multiple anchors for the same compounds id");
                }
                anchorsMap.put(anchor.getQueryExperiment().getName(), anchor);
            }
            for (ExperimentResult experimentResult : experimentResults) {
                String expName = experimentResult.getExperiment().getName();
                LibraryHit anchor = anchorsMap.get(expName);
                if (anchor!=null && anchor.getQueryExperiment().getName().equals(experimentResult.getExperiment().getName())){
                    IdentificationResult agreeingWithAnchor = null;
                    for (IdentificationResult result : experimentResult.getResults()) {
                        if (anchor.getMolecularFormula().subtract(result.getMolecularFormula()).equals(MolecularFormula.emptyFormula())){
                            agreeingWithAnchor = result;
                            break;
                        }
                    }
                    if (agreeingWithAnchor==null) {
                        LOG.warn("anchor MF not contained in candidate list: "+anchor.getMolecularFormula()+" for "+experimentResult.getExperiment().getName());
                        newExperimentResults.add(experimentResult);
                    } else {
                        ExperimentResult newResult = new ExperimentResult(experimentResult.getExperiment(), Collections.singletonList(agreeingWithAnchor), experimentResult.getExperimentSource(), experimentResult.getExperimentName());
                        newExperimentResults.add(newResult);

                    }
                } else {
                    newExperimentResults.add(experimentResult);
                }
            }

            experimentResults = newExperimentResults;


        }

        if (options.isRunGoodQualityOnly()) {
            //todo inefficient, just for eval?
            List<ExperimentResult> newExperimentResults = new ArrayList<>();

            for (ExperimentResult experimentResult : experimentResults) {
                if (CompoundQuality.isNotBadQuality(experimentResult.getExperiment())){
                    newExperimentResults.add(experimentResult);
                }
            }
            LOG.info("run with good quality compounds only: "+newExperimentResults.size()+" out of "+experimentResults.size());
            experimentResults = newExperimentResults;
        }

        //todo init here (not setup) and not in setup because it might store infos after one run!?
        NodeScorer[] nodeScorers;
        boolean useLibraryHits = (anchors != null && !options.isFixAnchors());
        double libraryLambda = options.getLibraryScoreLambda();//todo which lambda to use!?
        double lowestCosine = options.getLowestCosine();
        if (useLibraryHits) {
            LOG.info("use library hits as anchors.");
            Reaction[] reactions = ZodiacUtils.parseReactions(1);
            Set<MolecularFormula> netSingleReactionDiffs = new HashSet<>();
            for (Reaction reaction : reactions) {
                netSingleReactionDiffs.add(reaction.netChange());
            }
            nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d), new LibraryHitScorer(libraryLambda, lowestCosine, netSingleReactionDiffs)};
        } else {
            nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d)};
        }


        EdgeFilter edgeFilter = null;

        if (options.getThresholdFilter() > 0.0D && (options.getLocalFilter() > 0.0D || options.getMinLocalConnections() > 0d)) {
            int numberOfCandidates = Math.max(options.getLocalFilter(), 1);
            int numberOfConnections = options.getMinLocalConnections() > 0 ? options.getMinLocalConnections() : 10;
            edgeFilter = new EdgeThresholdMinConnectionsFilter(options.getThresholdFilter(), numberOfCandidates, numberOfConnections);
        } else if (options.getThresholdFilter() > 0.0D) {
            edgeFilter = new EdgeThresholdFilter(options.getThresholdFilter());
        } else if (options.getLocalFilter() > 0.0D) {
            edgeFilter = new LocalEdgeFilter(options.getLocalFilter());
        }
        if (edgeFilter == null) {
            edgeFilter = new EdgeThresholdFilter(0);
        }



        double minimumOverlap = 0.0D;
        ScoreProbabilityDistributionEstimator commonFragmentAndLossScorer;
        CommonFragmentAndLossScorer c;
//        if (options.isUseTreeScoresForScoring()){
//            c = new CommonFragmentAndLossWithTreeScoresScorer(minimumOverlap);
        //changed
        if (options.isUsePeakIntensityForScoring()){
//            c = new CommonFragmentAndLossScorerNoiseIntensityWeighted(minimumOverlap, options.getMedianNoiseIntensity());
            c = new CommonFragmentAndLossScorerNoiseIntensityWeighted(minimumOverlap);
        } else {
            c = new CommonFragmentAndLossScorer(minimumOverlap);
        }
        if (options.isEstimateDistribution()) {
            commonFragmentAndLossScorer = new ScoreProbabilityDistributionEstimator(c, probabilityDistribution, options.getThresholdFilter());
        } else {
            commonFragmentAndLossScorer = new ScoreProbabilityDistributionFix(c, probabilityDistribution, options.getThresholdFilter());
        }

//        SameIonizationScorer sameIonizationScorer = new SameIonizationScorer();
//        EdgeScorer[] edgeScorers = new EdgeScorer[]{commonFragmentAndLossScorer, sameIonizationScorer};

        EdgeScorer[] edgeScorers = new EdgeScorer[]{commonFragmentAndLossScorer};

        ZodiacJJob zodiacJJob = new ZodiacJJob(experimentResults, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, options.getIterationSteps(), options.getBurnInSteps(), options.getSeparateRuns(), options.isClusterCompounds(), !options.isOnlyOneStepZodiac());

        return zodiacJJob;


    }


    //////////////////////////////////////////////////////////////////////////////////////////
    // write results
    //////////////////////////////////////////////////////////////////////////////////////////


    public void writeResults(List<ExperimentResult> input, ZodiacResultsWithClusters zodiacResult) throws IOException {
        Path outputPath = Paths.get(options.getOutput());

        CompoundResult<FragmentsCandidate>[] result = zodiacResult.getResults();
        String[] ids = zodiacResult.getIds();

        Map<String, ExperimentResult> experimentResultMap = createMap(input);
        Map<String, String[]> representativeToCluster = zodiacResult.getRepresentativeToCluster();
        Scored<IdentificationResult>[] bestInitial = bestInitial(ids, experimentResultMap);
        writeZodiacOutput(ids, bestInitial, result, outputPath.resolve("zodiac_summary.csv"));
        writeClusters(representativeToCluster, outputPath.resolve("clusters.csv"));
        writeSpectra(ids, result, experimentResultMap, outputPath, filenameFormatter);
    }

    public void writeCrossvalidationAnchors(List<LibraryHit>[] libraryBatches) throws IOException {
        Path outputPath = Paths.get(options.getOutput());
        Path libraryHitsCVPath = outputPath.resolve("anchors_crossvalidation.csv");

        BufferedWriter writer = Files.newBufferedWriter(libraryHitsCVPath, Charset.defaultCharset());
        writer.write("batch" + SEP + "queryId" + SEP + "libraryPrecursorMass" + SEP + "queryPrecursorMass" + SEP + "queryEstimatedFormula" + SEP + "libraryStructure" + SEP + "libraryAdduct" + SEP + "cosine" + SEP + "sharePeaks");
        for (int i = 0; i < libraryBatches.length; i++) {
            List<LibraryHit> libraryBatch = libraryBatches[i];
            for (LibraryHit libraryHit : libraryBatch) {
                writer.newLine();
                StringJoiner joiner = new StringJoiner(SEP);
                joiner.add(Integer.toString(i));
                joiner.add(libraryHit.getQueryExperiment().getName());
                joiner.add(Double.toString(libraryHit.getPrecursorMz()));
                joiner.add(Double.toString(libraryHit.getQueryExperiment().getIonMass()));
                joiner.add(libraryHit.getMolecularFormula().formatByHill());
                joiner.add(libraryHit.getStructure()==null?"":libraryHit.getStructure());
                joiner.add(libraryHit.getIonType()==null?"":libraryHit.getIonType().toString());
                joiner.add(Double.toString(libraryHit.getCosine()));
                joiner.add(Integer.toString(libraryHit.getSharedPeaks()));
                writer.write(joiner.toString());
            }
        }
        writer.close();
    }

    public void writeResultsWithoutClusters(List<ExperimentResult> input, String[] zodiacIds, CompoundResult<FragmentsCandidate>[] zodiacResults) throws IOException {
        Path outputPath = Paths.get(options.getOutput());

        Map<String, ExperimentResult> experimentResultMap = createMap(input);
        Scored<IdentificationResult>[] bestInitial = bestInitial(zodiacIds, experimentResultMap);
        writeZodiacOutput(zodiacIds, bestInitial, zodiacResults, outputPath.resolve("zodiac_summary.csv"));
        writeSpectra(zodiacIds, zodiacResults, experimentResultMap, outputPath, filenameFormatter);
    }

    private final static int NUMBER_OF_HITS = Integer.MAX_VALUE;
    private final static String SEP = "\t";
    public static void writeZodiacOutput(String[] ids, Scored<IdentificationResult>[] initial, CompoundResult<FragmentsCandidate>[] result, Path outputPath) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputPath, Charset.defaultCharset());
        writer.write("id" + SEP + "quality" + SEP + "precursorMass"+ SEP + "ionsByMs1" + SEP + "SiriusMF" + SEP + "SiriusScore" + SEP + "numberOfCandidates" + SEP + "hasDummy" + SEP + "connectedCompounds" + SEP + "biggestTreeSize" + SEP + "maxExplainedIntensity" + SEP + "ZodiacMF" + SEP + "ZodiacMFIon"+ SEP + "ZodiacScore" + SEP + "treeSize" + SEP + "explainedIntensity");
        int maxCandidates = maxNumberOfCandidates(result);
        for (int i = 2; i <= maxCandidates; i++) {
            writer.write(SEP + "ZodiacMF" + String.valueOf(i) + SEP + "ZodiacMFIon" + String.valueOf(i) + SEP + "ZodiacScore" + String.valueOf(i) + SEP + "treeSize" + String.valueOf(i) + SEP + "explainedIntensity" + String.valueOf(i));
        }

        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            final String id2 = result[i].getId();

            if (!id.equals(id2)) throw new RuntimeException("different ids: "+id+" vs "+id2);

            final String siriusMF = initial[i]==null?null:initial[i].getCandidate().getMolecularFormula().formatByHill();
            final double siriusScore = initial[i]==null?Double.NaN:initial[i].getScore();

            int connections = result[i].getAnnotationOrThrow(Connectivity.class).getNumberOfConnectedCompounds();
            String summeryLine = createSummaryLine(id, siriusMF, siriusScore, connections, result[i].getCandidates());
            writer.newLine();
            writer.write(summeryLine);
        }

        writer.close();

    }

    private static int maxNumberOfCandidates(CompoundResult<FragmentsCandidate>[] result) {
        int max = 1;
        for (CompoundResult<FragmentsCandidate> fragmentsCandidateCompoundResult : result) {
            max = Math.max(fragmentsCandidateCompoundResult.getCandidates().length, max);
        }
        return max;
    }

    private static String createSummaryLine(String id, String siriusMF, double siriusScore, int numberConnections, Scored<FragmentsCandidate>[] result){
        String qualityString = "";
        Sirius sirius = new Sirius();
        double precursorMass = Double.NaN;
        String ionsByMs1 = "";
        if (result.length>0){
            Ms2Experiment experiment = result[0].getCandidate().getExperiment();
            //TODO hack: this don't have to be the ions used for SIRIUS computation in the first place!
            Sirius.GuessIonizationFromMs1Result guessIonization = getIonsFromMs1Hack(experiment, sirius);
            PrecursorIonType[] ms1IonModes = guessIonization.getGuessedIonTypes();
            if (ms1IonModes!=null && ms1IonModes.length>=1){
                ionsByMs1 = ms1IonModes[0].toString();
                for (int i = 1; i < ms1IonModes.length; i++) {
                    ionsByMs1 += ","+ms1IonModes[i].toString();
                }
            } else {
                ionsByMs1 = "None";
            }

            ionsByMs1 += ":"+guessIonization.getGuessingSource();

            CompoundQuality compoundQuality = experiment.getAnnotation(CompoundQuality.class, null);
            precursorMass = experiment.getIonMass();
            if (compoundQuality!=null) qualityString = compoundQuality.toString();
        }

        int biggestTreeSize = -1;
        double maxExplainedIntensity = 0;
        boolean hasDummy = false;
        for (Scored<FragmentsCandidate> scoredCandidate : result) {
            FragmentsCandidate candidate = scoredCandidate.getCandidate();
            if (DummyFragmentCandidate.isDummy(candidate)){
                hasDummy = true;
                continue;
            }
            final int treeSize = candidate.getFragments().length;
            final FTree tree = candidate.getAnnotation(FTree.class);
            final double intensity = tree.getAnnotationOrThrow(TreeScoring.class).getExplainedIntensity();
            biggestTreeSize = Math.max(treeSize,biggestTreeSize);
            maxExplainedIntensity = Math.max(maxExplainedIntensity, intensity);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(id);
        builder.append(SEP);
        builder.append(qualityString);
        builder.append(SEP);
        builder.append(String.valueOf(precursorMass));
        builder.append(SEP);
        builder.append(ionsByMs1);
        builder.append(SEP);
        builder.append(siriusMF);
        builder.append(SEP);
        builder.append(Double.toString(siriusScore));
        builder.append(SEP);
        builder.append(String.valueOf(result.length)); //numberOfCandidates
        builder.append(SEP);
        builder.append(String.valueOf(hasDummy));
        builder.append(SEP);
        builder.append(numberConnections);
        builder.append(SEP);
        builder.append(biggestTreeSize);
        builder.append(SEP);
        builder.append(maxExplainedIntensity);



        for (int j = 0; j < Math.min(result.length, NUMBER_OF_HITS); j++) {
            Scored<FragmentsCandidate> currentResult = result[j];
            FragmentsCandidate candidate = currentResult.getCandidate();
            final String mf = candidate.getFormula().formatByHill();
            final String ion = candidate.getIonType().toString();
            final double score = currentResult.getScore();
            final double treeSize, explIntensity;
            if (DummyFragmentCandidate.isDummy(candidate)){
                treeSize = -1;
                explIntensity = -1;
            } else {
                treeSize = currentResult.getCandidate().getFragments().length;//number of fragments = treeSize
                final FTree tree = candidate.getAnnotation(FTree.class);
                explIntensity = tree.getAnnotationOrThrow(TreeScoring.class).getExplainedIntensity();
            }

//            if (score <= 0) break; //don't write MF with 0 probability

            builder.append(SEP);
            builder.append(mf);
            builder.append(SEP);
            builder.append(ion);
            builder.append(SEP);
            builder.append(Double.toString(score));
            builder.append(SEP);
            builder.append(Double.toString(treeSize));
            builder.append(SEP);
            builder.append(Double.toString(explIntensity));
        }
        return builder.toString();
    }

    private static Sirius.GuessIonizationFromMs1Result getIonsFromMs1Hack(Ms2Experiment experiment, Sirius sirius){
        MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
        PossibleAdducts pa =  new PossibleAdducts(Iterables.toArray(PeriodicTable.getInstance().getKnownLikelyPrecursorIonizations(mutableMs2Experiment.getPrecursorIonType().getCharge()), PrecursorIonType.class));

        List<PrecursorIonType> allowedIonModes = new ArrayList<>();
        final Set<Ionization> ionModes = new HashSet<>(pa.getIonModes());
        for (Ionization ion : ionModes) {
            allowedIonModes.add(PrecursorIonType.getPrecursorIonType(ion));
        }
        return sirius.guessIonization(mutableMs2Experiment, allowedIonModes.toArray(new PrecursorIonType[0]));
    }

    public Scored<IdentificationResult>[] bestInitial(String[] ids, Map<String, ExperimentResult> experimentResultMap){
        Scored<IdentificationResult>[] best = new Scored[ids.length];
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            ExperimentResult result = experimentResultMap.get(id);

            if (result.getResults().size()==0){
                best[i] = null;
                continue;
            }

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

    public Map<String, ExperimentResult> createMap(List<ExperimentResult> experimentResults){
        Map<String, ExperimentResult> map = new HashMap<>();
        for (ExperimentResult experimentResult : experimentResults) {
            //todo use something else as id since name does not have to be unique
            map.put(experimentResult.getExperiment().getName(), experimentResult);
        }
        return map;
    }


    public static void writeClusters(Map<String, String[]> representativeToCluster, Path outputPath) throws IOException {
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
    private static void writeSpectra(String[] ids, CompoundResult<FragmentsCandidate>[] result, Map<String, ExperimentResult> experimentMap, Path outputPath, FilenameFormatter filenameFormatter) throws IOException {
        for (int i = 0; i < ids.length; i++) {
            final Scored<FragmentsCandidate>[] currentResults = result[i].getCandidates();
            final Scored<FragmentsCandidate> bestResult = currentResults[0];

            if (DummyFragmentCandidate.isDummy(bestResult.getCandidate())) continue;

            final String id = ids[i];
            ExperimentResult expResult = experimentMap.get(id);
            MutableMs2Experiment experiment = new MutableMs2Experiment(expResult.getExperiment());
            experiment.setMolecularFormula(bestResult.getCandidate().getFormula());
            experiment.setPrecursorIonType(bestResult.getCandidate().getIonType());
            String filename = makeFileName(expResult, filenameFormatter, i+1);
            Path file = outputPath.resolve(filename);
            final BufferedWriter writer = Files.newBufferedWriter(file, Charset.defaultCharset());
            new JenaMsWriter().write(writer, experiment);
            writer.close();

        }

    }

    protected static String makeFileName(ExperimentResult exp, FilenameFormatter filenameFormatter, int idx) {
        final int index = exp.getExperiment().getAnnotation(Index.class,Index.NO_INDEX).index;
        String name = filenameFormatter.formatName(exp, (index>=0 ? index : idx));
        if (!name.endsWith(".ms")) name += ".ms";
        return name;
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



}
