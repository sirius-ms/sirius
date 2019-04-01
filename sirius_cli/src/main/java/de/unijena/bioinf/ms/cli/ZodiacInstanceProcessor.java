package de.unijena.bioinf.ms.cli;


import de.unijena.bioinf.sirius.ExperimentResult;

public class ZodiacInstanceProcessor implements InstanceProcessor<ExperimentResult> {
    /*protected static Logger LOG = LoggerFactory.getLogger(ZodiacInstanceProcessor.class);
    protected ZodiacOptions options;
    protected FilenameFormatter filenameFormatter;

    private ScoreProbabilityDistribution probabilityDistribution;
    private List<LibraryHit> anchors;
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


    protected List<ExperimentResult> updateQuality(List<ExperimentResult> experimentResults, Path originalMsInformation, double isolationWindowWidth, double isolationWindowShift, Path outputDir) throws IOException {
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
        Ms2Run dataset = new MutableMs2Run(allExperiments, "default", Double.NaN, (new Sirius("default")).getMs2Analyzer().getDefaultProfile());
        Ms2RunPreprocessor preprocessor = new Ms2RunPreprocessor(true);

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
            ((MutableMs2Run) dataset).setDatasetStatistics(fixedDatasetStatistics);
        }
        
        List<QualityAnnotator> qualityAnnotators = new ArrayList<>();
        qualityAnnotators.add(new NoMs1PeakAnnotator(Ms2RunPreprocessor.FIND_MS1_PEAK_DEVIATION));
        qualityAnnotators.add(new FewPeaksAnnotator(Ms2RunPreprocessor.MIN_NUMBER_OF_PEAKS));
        qualityAnnotators.add(new LowIntensityAnnotator(Ms2RunPreprocessor.FIND_MS1_PEAK_DEVIATION, 0.01, Double.NaN));
//        qualityAnnotators.add(new NotMonoisotopicAnnotatorUsingIPA(Ms2RunPreprocessor.FIND_MS1_PEAK_DEVIATION));
        double max2ndMostIntenseRatio = 0.33;
        double maxSummedIntensitiesRatio = 1.0;
        qualityAnnotators.add(new ChimericAnnotator(Ms2RunPreprocessor.FIND_MS1_PEAK_DEVIATION, max2ndMostIntenseRatio, maxSummedIntensitiesRatio));

        preprocessor.setQualityAnnotators(qualityAnnotators);


        if (isolationWindowWidth>0){
            double right = isolationWindowWidth/2d+isolationWindowShift;
            double left = -isolationWindowWidth/2d+isolationWindowShift;
            ((MutableMs2Run) dataset).setIsolationWindow(new SimpleRectangularIsolationWindow(left, right));
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


            if (!atLeastOneTreeExplainsSomeIntensity(trees, 0.5)){
                CompoundQuality.setProperty(experiment, SpectrumProperty.PoorlyExplained);
            }
            if (!atLeastOneTreeExplainsSomePeaks(trees, 5)){ //changed from 3
                CompoundQuality.setProperty(experiment, SpectrumProperty.PoorlyExplained);
            }


            newExperimentResults.add(new ExperimentResult(experiment, result.getResults()));
        }

        if (outputDir!=null){
            Path qualityPath = outputDir.resolve("spectra_quality.csv");
            Ms2Run dataset2 = new MutableMs2Run(allExperiments, "default", Double.NaN, (new Sirius("default")).getMs2Analyzer().getDefaultProfile());
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
        //todo problem with name/id simplification
        //todo force name = id
        int maxCandidates = (options.getNumberOfCandidates() == null ? Integer.MAX_VALUE : options.getNumberOfCandidates());


        //todo For the official release zodiac should become a job an create subjobs in the jobmanager for multithreading
//      int workerCount = PropertyManager.getNumberOfCores();
        int workerCount = options.getNumOfCores() > 0 ? options.getNumOfCores() : (new SystemInfo()).getHardware().getProcessor().getPhysicalProcessorCount() - 1;
        PropertyManager.PROPERTIES.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(workerCount));



        Path libraryHitsFile = (options.getLibraryHitsFile() == null ? null : Paths.get(options.getLibraryHitsFile()));
        try {
            anchors = (libraryHitsFile == null) ? null : ZodiacUtils.parseLibraryHits(libraryHitsFile, experimentResults, LOG); //only specific GNPS format
        } catch (IOException e) {
            LOG.error("Cannot load library hits from file.", e);
            return null;
        }



        //todo init here (not setup) and not in setup because it might store infos after one run!?
        NodeScorer[] nodeScorers;
        boolean useLibraryHits = (anchors != null);
        double libraryLambda = options.getLibraryScoreLambda();//todo which lambda to use!?
        double lowestCosine = options.getLowestCosine();
        if (useLibraryHits) {
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
        if (options.isEstimateDistribution()) {
            commonFragmentAndLossScorer = new ScoreProbabilityDistributionEstimator(new CommonFragmentAndLossScorer(minimumOverlap), probabilityDistribution, options.getThresholdFilter());
        } else {
            commonFragmentAndLossScorer = new ScoreProbabilityDistributionFix(new CommonFragmentAndLossScorer(minimumOverlap), probabilityDistribution, options.getThresholdFilter());
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

    private final static int NUMBER_OF_HITS = Integer.MAX_VALUE;
    private final static String SEP = "\t";
    public static void writeZodiacOutput(String[] ids, Scored<IdentificationResult>[] initial, CompoundResult<FragmentsCandidate>[] result, Path outputPath) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputPath, Charset.defaultCharset());
        writer.write("id" + SEP + "quality" + SEP + "precursorMass"+ SEP + "ionsByMs1" + SEP + "SiriusMF" + SEP + "SiriusScore" + SEP + "connectedCompounds" + SEP + "biggestTreeSize" + SEP + "maxExplainedIntensity" + SEP + "ZodiacMF" + SEP + "ZodiacMFIon"+ SEP + "ZodiacScore" + SEP + "treeSize");

        int maxCandidates = maxNumberOfCandidates(result);
        for (int i = 2; i <= maxCandidates; i++) {
            writer.write(SEP + "ZodiacMF" + String.valueOf(i) + SEP + "ZodiacMFIon" + String.valueOf(i) + SEP + "ZodiacScore" + String.valueOf(i) + SEP + "treeSize" + String.valueOf(i) );
        }

        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            final String id2 = result[i].getId();

            if (!id.equals(id2)) throw new RuntimeException("different ids: "+id+" vs "+id2);

            final String siriusMF = initial[i].getCandidate().getMolecularFormula().formatByHill();
            final double siriusScore = initial[i].getScore();

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
            //TODO hack: this don't have to be the iondetection used for SIRIUS computation in the first place!
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
        for (Scored<FragmentsCandidate> scoredCandidate : result) {
            FragmentsCandidate candidate = scoredCandidate.getCandidate();
            if (DummyFragmentCandidate.isDummy(candidate)) continue;
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
            final double treeSize;
            if (DummyFragmentCandidate.isDummy(candidate)){
                treeSize = -1;
            } else {
                treeSize = currentResult.getCandidate().getFragments().length;//number of fragments = treeSize
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



    *//*
    experimentMap necessary since FragmentsCandidate might be the cluster representative.
     *//*
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
    }*/

    @Override
    public boolean setup() {
        return false;
    }

    @Override
    public boolean validate() {
        return false;
    }

    @Override
    public void output(ExperimentResult result) {

    }
}
