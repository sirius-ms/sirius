package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.babelms.MsIO;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DataProcessor {

    private File spectraDir;
    private File predictionsDir;

    private final File fTreeDir;
    private final File outputDir;
    private final String[] fileNames;

    private static final long SHUFFLE_SEED = 42;

    public DataProcessor(File spectraDir, File predictionsDir, File fTreeDir, File outputDir){
        this(spectraDir, predictionsDir, fTreeDir, outputDir, 1, 0);
    }

    public DataProcessor(File spectraDir, File predictionsDir, File fTreeDir, File outputDir, int numPartitions, int idxPartition){
        this(spectraDir, predictionsDir, fTreeDir, outputDir,
                Arrays.stream(Objects.requireNonNull(outputDir.list())).
                        map(processedInstanceFileName -> processedInstanceFileName.replaceFirst("\\..+$", "")).
                        collect(Collectors.toList()),
                numPartitions, idxPartition);
    }

    public DataProcessor(File spectraDir, File predictionsDir, File fTreeDir, File outputDir, Collection<String> processedInstances){
        this(spectraDir, predictionsDir, fTreeDir, outputDir, processedInstances, 1, 0);
    }

    public DataProcessor(File spectraDir, File predictionsDir, File fTreeDir, File outputDir, Collection<String> processedInstances, int numPartitions, int idxPartition){
        this(fTreeDir, outputDir, processedInstances, numPartitions, idxPartition);

        if(spectraDir != null){
            if(spectraDir.isDirectory()){
                this.spectraDir = spectraDir;
            }else{
                throw new RuntimeException("The given abstract path name denoting the directory containing the mass spectra " +
                        "does not exist or is not a directory.");
            }
        }
        if(predictionsDir != null){
            if(predictionsDir.isDirectory()){
                this.predictionsDir = predictionsDir;
            }else{
                throw new RuntimeException("The given abstract path name denoting the directory containing the predicted compounds" +
                        "does not exist or is not a directory.");
            }
        }
    }

    private DataProcessor(File fTreeDir, File outputDir, Collection<String> processedInstances, int numPartitions, int idxPartition) {
        if(fTreeDir.isDirectory() && outputDir.isDirectory()) {
            this.fTreeDir = fTreeDir;
            this.outputDir = outputDir;

            System.out.println("Filter out already processed instances.");
            List<String> filteredInstanceFileNames = this.filterOutProcessedInstances(processedInstances);
            System.out.println(filteredInstanceFileNames.size() + " instances remain after filtering.");

            System.out.println("Sort the remaining instances lexicographically and shuffle them with " +
                    "seed " + SHUFFLE_SEED + ".");
            Collections.sort(filteredInstanceFileNames);
            Collections.shuffle(filteredInstanceFileNames, new Random(SHUFFLE_SEED));

            System.out.println("Partition the instances into " + numPartitions + ".");
            this.fileNames = this.getPartitionOfInstances(filteredInstanceFileNames, numPartitions, idxPartition);
            System.out.println("The partition with index " + idxPartition + " was created and contains " +
                    this.fileNames.length + " instances.");
        }else{
            throw new RuntimeException("Whether the given abstract path name for the fragmentation tree" +
                    "directory or the output directory does not exist or is not a directory.");
        }
    }

    private List<String> filterOutProcessedInstances(Collection<String> processedInstanceFileNames) {
        // For each method (computation of subtrees, comparison or structure ranking), FTrees are always needed.
        return Arrays.stream(Objects.requireNonNull(this.fTreeDir.list())).
                map(fTreeFileName -> fTreeFileName.replaceFirst("\\.json", "")).
                filter(fileName -> {
                    for (String processedFileName : processedInstanceFileNames) {
                        if (fileName.equals(processedFileName)) return false;
                    }
                    return true;
                }).collect(Collectors.toList());
    }

    private String[] getPartitionOfInstances(List<String> instanceFileNames, int numPartitions, int idxPartition) {
        int numberOfInstances = instanceFileNames.size();
        if (numberOfInstances <= numPartitions) {
            if (idxPartition <= numberOfInstances - 1) {
                return new String[]{instanceFileNames.get(idxPartition)};
            } else {
                return new String[0];
            }
        } else {
            int lengthPartition = numberOfInstances / numPartitions;
            int rest = numberOfInstances - numPartitions * lengthPartition;

            int startIndex, endIndex;// 'startIndex' is inclusive, 'endIndex' is exclusive
            if (idxPartition < rest) {
                // the partition will take one additional element and
                // all successor partitions have one additional element from the rest as well
                startIndex = idxPartition * (lengthPartition + 1);
                endIndex = startIndex + lengthPartition + 1;
            } else {
                startIndex = idxPartition * lengthPartition + rest;
                endIndex = startIndex + lengthPartition;
            }

            String[] fileNames = new String[endIndex - startIndex];
            for (int i = 0; i < fileNames.length; i++) {
                fileNames[i] = instanceFileNames.get(startIndex + i);
            }
            return fileNames;
        }
    }

    private MolecularGraph readMoleculeFromMsFile(String fileName) throws IOException, InvalidSmilesException, UnknownElementException {
        File file = new File(this.spectraDir, fileName);
        try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {

            String currentLine = fileReader.readLine();
            String molecularFormula = null, smiles = null;
            boolean mfWasRead = false;
            boolean smilesWasRead = false;

            while (currentLine != null) {
                if (currentLine.startsWith(">formula")) {
                    molecularFormula = currentLine.split(" ")[1];
                    mfWasRead = true;
                } else if (currentLine.startsWith(">smiles")) {
                    smiles = currentLine.split(" ")[1];
                    smilesWasRead = true;
                }
                if (mfWasRead && smilesWasRead) break;
                currentLine = fileReader.readLine();
            }
            // Assumption: In any case, 'file' contains two lines that start with ">formula" and ">smiles".
            // --> the molecular formula and the smiles string have been read
            SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            return new MolecularGraph(MolecularFormula.parse(molecularFormula), smilesParser.parseSmiles(smiles));
        }
    }

    private FTree readFTree(String fileName) throws IOException {
        File file = new File(this.fTreeDir, fileName);
        FTree fTree = MsIO.readTreeFromFile(file);
        return fTree;
    }

    private CSIPredictionData readPredictionDataFromTSV(String fileName) throws IOException, UnknownElementException {
        File file = new File(this.predictionsDir, fileName);
        try(BufferedReader fileReader = Files.newBufferedReader(file.toPath())){
            // 1.: Get the index of the columns of interest:
            String[] columnNames = fileReader.readLine().split("\\t");
            int rankIdx = -1, csiScoreIdx = -1, confidenceScoreIdx = -1, mfIdx = -1, smilesIdx = -1;

            for(int i = 0; i < columnNames.length; i++){
                String columnName = columnNames[i];
                switch(columnName){
                    case "rank":
                        rankIdx = i;
                        break;
                    case "ConfidenceScore":
                        confidenceScoreIdx = i;
                        break;
                    case "CSI:FingerIDScore":
                        csiScoreIdx = i;
                        break;
                    case "molecularFormula":
                        mfIdx = i;
                        break;
                    case "smiles":
                        smilesIdx = i;
                        break;
                }
            }

            // 2.: Read all remaining lines in the file and return a list of String arrays:
            // 'fileReader' has already read the first line of 'file' which is the definition line containing the column names.
            // Now read the remaining lines which contain all the data. Each line corresponds to a predicted structure.
            ArrayList<String[]> lines = this.readAllRemainingLines(fileReader);

            // 3.: Now parse these lines and create a CSIPredictionData object:
            int numberOfStructures = lines.size();
            int[] ranks = new int[numberOfStructures];
            double[] csiScores = new double[numberOfStructures];
            double[] confidenceScores = new double[numberOfStructures];
            MolecularFormula[] molecularFormulas = new MolecularFormula[numberOfStructures];
            String[] smilesStrings = new String[numberOfStructures];

            for(int i = 0; i < numberOfStructures; i++){
                String[] currentLine = lines.get(i);
                ranks[i] = Integer.parseInt(currentLine[rankIdx]);
                csiScores[i] = Double.parseDouble(currentLine[csiScoreIdx]);
                confidenceScores[i] = (currentLine[confidenceScoreIdx].equals("N/A")) ? Double.NaN : Double.parseDouble(currentLine[confidenceScoreIdx]);
                molecularFormulas[i] = MolecularFormula.parse(currentLine[mfIdx]);
                smilesStrings[i] = currentLine[smilesIdx];
            }

            return new CSIPredictionData(numberOfStructures, ranks, csiScores, confidenceScores, molecularFormulas, smilesStrings);
        }
    }

    private ArrayList<String[]> readAllRemainingLines(BufferedReader fileReader) throws IOException{
        ArrayList<String[]> lines = new ArrayList<>();
        String currentLine = fileReader.readLine();
        while(currentLine != null){
            String[] data = currentLine.split("\\t");
            lines.add(data);

            currentLine = fileReader.readLine();
        }

        return lines;
    }

    private synchronized void appendStringToFile(File file, String str) throws IOException{
        try(BufferedWriter fileWriter = Files.newBufferedWriter(file.toPath(), StandardOpenOption.APPEND)){
            fileWriter.newLine();
            fileWriter.write(str);
        }
    }

    private void saveRankingData(File file, CSIPredictionData predictionData, int[] fragmenterRanks, double[] fragmenterScores) throws IOException{
        try(BufferedWriter fileWriter = Files.newBufferedWriter(file.toPath())){
            fileWriter.write("CSI_Rank\tfragmenterRank\tCSI:FingerIDScore\tfragmenterScore\tConfidenceScore\tmolecularFormula\tsmiles");

            for(int i = 0; i < predictionData.numberOfStructures; i++){
                fileWriter.newLine();
                fileWriter.write(predictionData.ranks[i]+"\t"+fragmenterRanks[i]+"\t"+predictionData.csiScores[i]+
                        "\t"+fragmenterScores[i]+"\t"+predictionData.confidenceScores[i]+"\t"+
                        predictionData.molecularFormulas[i].toString()+"\t"+predictionData.smilesStrings[i]);
            }
        }
    }

    private void unreference(MolecularGraph molecule, FTree fTree, CombinatorialFragmenterScoring scoring, CombinatorialSubtreeCalculator subtreeCalc) {
        molecule = null;
        fTree = null;
        scoring = null;
        subtreeCalc = null;
    }

    public void computeCombinatorialSubtrees(CombinatorialFragmenter.Callback2 fragmentationConstraint, SubtreeComputationMethod subtreeCompMethod) throws InterruptedException {
        // INITIALISATION:
        // Initialise the ExecutorService:
        System.out.println("Initialise ExecutorService...");
        final int NUM_CONCURRENT_THREADS = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CONCURRENT_THREADS);

        // LOOP - CREATE TASKS FOR EACH INSTANCE:
        // For each instance, create a task which computes the in silico fragmentation graph, computes the subtree using
        // CriticalPath1 and saves the result into a JSON file.
        System.out.println("Each instance corresponds to one task. Collect all tasks...");
        ArrayList<Callable<Object>> tasks = new ArrayList<>(this.fileNames.length);
        for (String fileName : this.fileNames) {
            Callable<Object> task = Executors.callable(() -> {
                try {
                    MolecularGraph molecule = this.readMoleculeFromMsFile(fileName + ".ms");
                    FTree fTree = this.readFTree(fileName + ".json");
                    EMFragmenterScoring2 scoring = new EMFragmenterScoring2(molecule, fTree);

                    final HashSet<MolecularFormula> mfSet = new HashSet<>();
                    for (Fragment ft : fTree.getFragmentsWithoutRoot()) {
                        mfSet.add(ft.getFormula());
                        mfSet.add(ft.getFormula().add(MolecularFormula.getHydrogen()));
                        mfSet.add(ft.getFormula().add(MolecularFormula.getHydrogen().multiply(2)));
                        if (ft.getFormula().numberOfHydrogens()>0) mfSet.add(ft.getFormula().subtract(MolecularFormula.getHydrogen()));
                        if (ft.getFormula().numberOfHydrogens()>1) mfSet.add(ft.getFormula().subtract(MolecularFormula.getHydrogen().multiply(2)));
                    }

                    final CriticalPathSubtreeCalculator subtreeCalc = new CriticalPathSubtreeCalculator(fTree, molecule, scoring, true);
                    subtreeCalc.setMaxNumberOfNodes(100000);
                    subtreeCalc.initialize((node, nnodes, nedges) -> {
                        if(mfSet.contains(node.getFragment().getFormula())) return true;
                        return (node.getTotalScore() > -5f);
                    });
                    subtreeCalc.computeSubtree();

                    CombinatorialSubtreeCalculatorJsonWriter.writeResultsToFile(subtreeCalc, new File(this.outputDir, fileName + ".json"));
                    this.unreference(molecule, fTree, scoring, subtreeCalc);
                } catch (Exception e) {
                    System.out.println("An error occurred during processing instance " + fileName);
                    File resultFile = new File(this.outputDir, fileName + ".json");
                    if (resultFile.exists()) {
                        boolean wasDeleted = resultFile.delete();
                        if (wasDeleted) {
                            System.out.println(fileName + ".json was successfully deleted.");
                        } else {
                            System.out.println("Could not delete " + fileName + ".json.");
                        }
                    }
                    e.printStackTrace();
                }
            });
            tasks.add(task);
        }

        // COMPUTING THE TASKS:
        // Wait, until all tasks terminated.
        System.out.println("Execute all tasks...");
        Collection<Future<Object>> futures = executor.invokeAll(tasks);

        System.out.println(futures.size() + " tasks of " + tasks.size() + " have been processed and the executor service will be shutdown.");
        executor.shutdown();
    }

    public void compareSubtreeComputationMethods(CombinatorialFragmenter.Callback2 fragmentationConstraint, String outputFileName) throws InterruptedException, IOException, ExecutionException {
        // We know: this.fileNames contains the filenames of all instances that have to be processed.
        // For each instance, we want to compare all subtree computation methods.
        // That means, we want to compute the fragmentation graph and the subtrees, their score and running times and
        // the tanimoto coefficient between each heuristic subtree and the optimal subtree (ILP).
        // We store these results in the specified CSV output file 'this.outputDir/outputFileName'.

        // INITIALISATION:
        // Initialise the ExecutorService:
        final int NUM_AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_AVAILABLE_PROCESSORS);

        // Create an array with all SubtreeComputationMethods and determine the index of the ILP method:
        final SubtreeComputationMethod[] methods = SubtreeComputationMethod.values(); // array of the methods in the order they're declared
        final int ilpIdx = SubtreeComputationMethod.ILP.ordinal();

        // Test if 'outputFileName' is an existing file in this.outputDir
        // and if not, create a new file and write the starting string to it:
        File outputFile = new File(this.outputDir, outputFileName);
        if(outputFile.createNewFile()){
            StringBuilder startingString = new StringBuilder("instance_name,construction_runtime");
            for (int i = 0; i < methods.length; i++) startingString.append("," + methods[i].name() + "_runtime");
            for (int i = 0; i < methods.length; i++) startingString.append("," + methods[i].name() + "_score");
            for (int i = 0; i < methods.length; i++) startingString.append("," + methods[i].name() + "_tanimoto");

            try(BufferedWriter fileWriter = Files.newBufferedWriter(outputFile.toPath())){
                fileWriter.write(startingString.toString());
            }
        }

        // CREATING ALL TASKS:
        // Each instance in this.fileNames corresponds to one task.
        // Each task does the following:
        // - load the molecule and the FTree from their files in 'spectraDir' and 'fTreeDir', and create a scoring object
        // - create a CombinatorialGraph with terminal nodes and measure the running time of construction
        // - compute the CombinatorialSubtree for each method (ILP, Insertion, Prim, Critical Path1-3)
        // - measure the running the times, the score and the tanimoto coefficient against the ILP solution
        // - save all data in a string, matching the CSV ordering, and write it to the specified output file.
        System.out.println("Collect all tasks...");
        ArrayList<Callable<Object>> tasks = new ArrayList<>(this.fileNames.length);
        for (String fileName : this.fileNames) {
            Callable<Object> task = Executors.callable(() -> {
                try {
                    // 1.) Initialise the molecule, the FTree and the scoring object:
                    MolecularGraph molecule = this.readMoleculeFromMsFile(fileName + ".ms");
                    FTree fTree = this.readFTree(fileName + ".json");
                    DirectedBondTypeScoring scoring = new DirectedBondTypeScoring(molecule);

                    // 2.) Create the CombinatorialGraph and add the terminal nodes to it:
                    // Measure the running time for constructing such fragmentation graph.
                    long timeStamp, constructionRuntime;
                    CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(molecule, scoring);

                    timeStamp = System.nanoTime();
                    CombinatorialGraph graph = fragmenter.createCombinatorialFragmentationGraph(fragmentationConstraint);
                    CombinatorialGraphManipulator.addTerminalNodes(graph, scoring, fTree);
                    constructionRuntime = System.nanoTime() - timeStamp;

                    // 3.) Compute the CombinatorialSubtree of 'graph' with each SubtreeComputationMethod:
                    // Save the score, the running time and the tanimoto coefficient between the ILP solution for each method.
                    CombinatorialSubtree[] subtrees = new CombinatorialSubtree[methods.length];
                    long[] runningTimes = new long[methods.length];
                    double[] scores = new double[methods.length];
                    double[] tanimotoScores = new double[methods.length];

                    // 3.1: For each method, compute the subtree, measure the runtime and score:
                    for (int i = 0; i < methods.length; i++) {
                        SubtreeComputationMethod method = methods[i];
                        timeStamp = System.nanoTime();
                        CombinatorialSubtreeCalculator subtreeCalc = SubtreeComputationMethod.getComputedSubtreeCalculator(fTree, graph, scoring, method);

                        runningTimes[i] = System.nanoTime() - timeStamp;
                        subtrees[i] = subtreeCalc.getSubtree();
                        scores[i] = subtreeCalc.getScore();
                    }

                    // 3.2: For each method, compute the tanimoto coefficient between the subtree and the ILP subtree:
                    CombinatorialSubtree ilpSubtree = subtrees[ilpIdx];
                    TObjectIntHashMap<BitSet> mergedEdgeBitSet2Index = graph.mergedEdgeBitSet2Index();
                    int maxBitSetLength = graph.maximalBitSetLength();

                    for (int i = 0; i < methods.length; i++) {
                        CombinatorialSubtree subtree = subtrees[i];
                        tanimotoScores[i] = CombinatorialSubtreeManipulator.tanimoto(subtree, ilpSubtree, mergedEdgeBitSet2Index, maxBitSetLength);
                    }

                    // 4.) Save all data into one string and write this string into the output file:
                    // The string should look like this:
                    // "<instance name>,<constrRunningtime>,{Running times},{Scores},{Tanimoto-Scores}"
                    StringBuilder strBuilder = new StringBuilder(fileName + "," + constructionRuntime);
                    for (int i = 0; i < methods.length; i++) strBuilder.append("," + runningTimes[i]);
                    for (int i = 0; i < methods.length; i++) strBuilder.append("," + scores[i]);
                    for (int i = 0; i < methods.length; i++) strBuilder.append("," + tanimotoScores[i]);

                    this.appendStringToFile(outputFile, strBuilder.toString());
                }catch(IOException | InvalidSmilesException | UnknownElementException e){
                    System.out.println("An error occurred during computing instance"+fileName);
                    e.printStackTrace();
                }
            });
            tasks.add(task);
        }
        System.out.println("All tasks were collected and will be executed now...");
        executor.invokeAll(tasks);

        System.out.println("The executor service will be shutdown.");
        executor.shutdown();
    }

    public void runStructureRanking(CombinatorialFragmenter.Callback2 fragmentationConstraint, SubtreeComputationMethod subtreeCompMethod) throws InterruptedException {
        // INITIALISATION:
        // Initialise the executor service:
        System.out.println("Initialise the executor service.");
        final int NUM_AVAILABLE_PROCESSOR = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_AVAILABLE_PROCESSOR);

        // COLLECT ALL TASKS:
        // For each instance in this.fileNames do:
        // - load the corresponding fragmentation tree and the predicted structures together with their data
        // - for each predicted structure:
        //      ->  create the fragmentation graph, add the terminal nodes (regarding the fTree) and
        //          remove nodes which are not connected to a terminal node
        //      ->  compute the rooted subtree in this graph and also its score
        // - assign each predicted structure a rank corresponding to its score
        // - write the new data into an TSV file
        System.out.println("Each instance corresponds to one task. Collect all tasks...");
        ArrayList<Callable<Object>> tasks = new ArrayList<>(this.fileNames.length);
        for(String fileName : this.fileNames){
            Callable<Object> task = Executors.callable(()-> {
                try{
                    // 1. Load the fragmentation tree and the prediction data:
                    FTree fTree = this.readFTree(fileName+".json");
                    CSIPredictionData predictionData = this.readPredictionDataFromTSV(fileName+".tsv");

                    // 2. Iterate over the predicted structures and compute their fragmentation graphs
                    // and their corresponding subtree using the specified SubtreeComputationMethod 'subtreeCompMethod'.
                    // For structure 'i', store the score of the subtree in an array at index 'i'.
                    SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
                    double[] scores = new double[predictionData.numberOfStructures];

                    for(int i = 0; i < predictionData.numberOfStructures; i++){
                        String smiles = predictionData.smilesStrings[i];
                        MolecularFormula mf = predictionData.molecularFormulas[i];

                        MolecularGraph candidateMolecule = new MolecularGraph(mf, smiParser.parseSmiles(smiles));
                        DirectedBondTypeScoring scoring = new DirectedBondTypeScoring(candidateMolecule);

                        CombinatorialSubtreeCalculator subtreeCalc = SubtreeComputationMethod.getComputedSubtreeCalculator(fTree, candidateMolecule, scoring, fragmentationConstraint, subtreeCompMethod);

                        scores[i] = subtreeCalc.getScore();
                    }

                    // 3. Compute the ranks of the candidate structures using the computed scores:
                    // Structure with rank 'i' corresponds to the structure with the i-th best score.
                    // --> the ranks correspond to the position of the structures in the sorted list
                    Integer[] indices = new Integer[predictionData.numberOfStructures];
                    for(int i = 0; i < predictionData.numberOfStructures; i++) indices[i] = i;
                    Arrays.sort(indices, (idx1, idx2) -> (int) Math.signum(scores[idx2] - scores[idx1]));

                    // after sorting: indices[i] = j
                    // --> structure with index 'j' is at index 'i' in the descending order of 'scores'
                    // --> structure 'j' has rank 'i+1'
                    int[] fragmenterRanks = new int[predictionData.numberOfStructures];
                    for(int i = 0; i < indices.length; i++){
                        int structureIdx = indices[i];
                        fragmenterRanks[structureIdx] = i+1;
                    }

                    // 4. Store the data in a TSV file:
                    this.saveRankingData(new File(this.outputDir, fileName+".tsv"), predictionData, fragmenterRanks, scores);
                } catch (IOException | UnknownElementException | InvalidSmilesException e) {
                    e.printStackTrace();
                }
            });
            tasks.add(task);
        }

        System.out.println("All tasks were collected and will be given to the executor service for their computation...");
        executor.invokeAll(tasks);

        System.out.println("All tasks have been processed. The executor service will be shutdown.");
        executor.shutdown();
    }

    private class CSIPredictionData{

        protected final int numberOfStructures;
        protected final int[] ranks;
        protected final double[] csiScores;
        protected final double[] confidenceScores;
        protected final MolecularFormula[] molecularFormulas;
        protected final String[] smilesStrings;

        public CSIPredictionData(int numberOfStructures, int[] ranks, double[] csiScores, double[] confidenceScores, MolecularFormula[] molecularFormulas, String[] smilesStrings){
            this.numberOfStructures = numberOfStructures;
            this.ranks = ranks;
            this.csiScores = csiScores;
            this.confidenceScores = confidenceScores;
            this.molecularFormulas = molecularFormulas;
            this.smilesStrings = smilesStrings;
        }

    }

    /*
    public static void main(String[] args){
        File spectraDir = new File(args[0]);
        File fTreeDir = new File(args[1]);
        File outputDir = new File(args[2]);

        DataProcessor dataProcessor = new DataProcessor(spectraDir, null, fTreeDir, outputDir);
        try {
            dataProcessor.computeCombinatorialSubtrees(null, null);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
     */

    public static void main(String[] args){
        try {
            final File subtreeDir = new File("/vol/clusterdata/nils/Results/new_subtrees");
            final File outputFile = new File("/vol/clusterdata/nils/Results/numFragmentationSteps_pullUps.txt");

            final String[] fileNames = Objects.requireNonNull(subtreeDir.list());

            final int NUM_CONCURRENT_THREADS = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(NUM_CONCURRENT_THREADS);
            ArrayList<Callable<List<Integer>>> tasks = new ArrayList<>(fileNames.length);
            for (final String fileName : fileNames) {
                Callable<List<Integer>> task = () -> {
                    final FileReader fileReader = new FileReader(new File(subtreeDir, fileName));
                    CombinatorialSubtree subtree = CombinatorialSubtreeCalculatorJsonReader.readTreeFromJson(fileReader);
                    return subtree.getTerminalNodes().stream().map(terminalNode -> {
                        CombinatorialNode peakExplainingNode = terminalNode.getIncomingEdges().get(0).getSource();
                        ArrayList<CombinatorialNode> nodesOnPath2Root = subtree.getAllParentsOf(peakExplainingNode);

                        // 1. Pull-up for only 'peakExplainingNode':
                        int minDepth = peakExplainingNode.getDepth();
                        for(CombinatorialNode node : nodesOnPath2Root){
                            if(peakExplainingNode.getFragment().isDirectFragmentOf(node.getFragment())){
                                int depth = node.getDepth() + 1;
                                if(depth < minDepth) minDepth = depth;
                            }
                        }

                        // 2. Pull-up for a parent of 'peakExplainingNode':
                        for(int i = 0; i < nodesOnPath2Root.size()-1; i++){
                            final CombinatorialNode v = nodesOnPath2Root.get(i);
                            for(int j = i+1; j < nodesOnPath2Root.size(); j++){
                                final CombinatorialNode u = nodesOnPath2Root.get(j);
                                if(v.getFragment().isDirectFragmentOf(u.getFragment())){
                                    int depth = u.getDepth() + 1 + peakExplainingNode.getDepth() - v.getDepth();
                                    if(depth < minDepth) minDepth = depth;
                                }
                            }
                        }

                        if(minDepth >= Integer.parseInt(args[0])){
                            printFragmentationPath(fileName, minDepth, terminalNode, peakExplainingNode, nodesOnPath2Root);
                        }

                        return minDepth;
                    }).toList();
                };
                tasks.add(task);
            }

            List<Future<List<Integer>>> futures = executor.invokeAll(tasks);
            executor.shutdown();
            try(BufferedWriter fileWriter = Files.newBufferedWriter(outputFile.toPath())){
                for(Future<List<Integer>> future : futures){
                    List<Integer> observedFragSteps = future.get();
                    for(int numFragSteps : observedFragSteps){
                        fileWriter.write(Integer.toString(numFragSteps));
                        fileWriter.newLine();
                    }
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static synchronized void printFragmentationPath(String fileName, int minDepth, CombinatorialNode terminalNode, CombinatorialNode peakExplainingNode, ArrayList<CombinatorialNode> nodesOnPath2Root){
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println("Instance: " + fileName + " | molecular formula: " + terminalNode.getFragment().getFormula()
                + " | Depth after pull up: " + minDepth + "\n");
        System.out.print(peakExplainingNode.getFragment().toSMILES());
        for(CombinatorialNode node : nodesOnPath2Root){
            System.out.print(" <-- " + node.getFragment().toSMILES());
        }
        System.out.println("\n\n----------------------------------------------------------------------------------------");
    }
}
