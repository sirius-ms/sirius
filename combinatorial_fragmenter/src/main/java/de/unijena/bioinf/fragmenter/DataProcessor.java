package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.MsIO;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DataProcessor {

    private final File spectraDir;
    private final File fTreeDir;
    private final File outputDir;
    private final String[] fileNames;

    private static final long SHUFFLE_SEED = 42;

    public DataProcessor(File spectraDir, File fTreeDir, File outputDir, int numPartitions, int idxPartition){
        if(spectraDir.isDirectory() && fTreeDir.isDirectory() && outputDir.isDirectory()){
            this.spectraDir = spectraDir;
            this.fTreeDir = fTreeDir;
            this.outputDir = outputDir;

            /* Assumptions:
             * 1. 'spectraDir' and 'fTreeDir' contain the same amount of files
             * 2. For each spectrum in 'spectraDir' there is the corresponding FTree in 'fTreeDir' with the same name
             *
             * Now: FILTERING
             * Exclude all fTree files whose results are already processed and stored in 'outputDir'.
             * An fTree file and a result file share the same ending ".json".
             */
            System.out.println("Filter out already processed instances in case you want to compute new subtrees.");
            String[] resultFileNames = outputDir.list();
            List<String> filteredFTreeFileNames = Arrays.stream(fTreeDir.list()).
                    filter(fileName -> {
                        for(String resultFileName : resultFileNames){
                            if(fileName.equals(resultFileName)) return false;
                        }
                        return true;
                    }).collect(Collectors.toList());
            System.out.println(filteredFTreeFileNames.size()+" instances remain after filtering.");

            /* PARTITION:
             * Create the 'idxPartition'-th partition of 'filteredFTreeFileNames.
             */
            System.out.println("Partition the data set into "+numPartitions+" partitions.");

            /* First, sort 'filteredFTreeFileNames' lexicographically - just in case that
             * 'File.list()' is not reproducible due to system properties.
             * Second, shuffle 'filteredFTreeFileNames' to distribute the instances equally between the partitions.
             */
            Collections.sort(filteredFTreeFileNames);
            Collections.shuffle(filteredFTreeFileNames, new Random(SHUFFLE_SEED));

            int lengthPartition = filteredFTreeFileNames.size() / numPartitions;
            int rest = filteredFTreeFileNames.size() - (lengthPartition * numPartitions);

            int startIndex, endIndex; // startIndex inclusive, endIndex exclusive
            if(idxPartition < rest){
                startIndex = idxPartition * lengthPartition + idxPartition;
                endIndex = startIndex + lengthPartition + 1;
            }else{
                startIndex = idxPartition * lengthPartition + rest;
                endIndex = startIndex + lengthPartition;
            }

            this.fileNames = new String[endIndex - startIndex];
            for(int i = startIndex; i < endIndex; i++){
                this.fileNames[i - startIndex] = filteredFTreeFileNames.get(i).replaceFirst("\\.json", "");
            }
            System.out.println("The "+(idxPartition+1)+"-th partition with "+this.fileNames.length+" instances was created.");
        }else{
            throw new RuntimeException("The given abstract path names don't exist or aren't a directory.");
        }
    }

    public DataProcessor(File spectraDir, File fTreeDir, File outputDir){
        this(spectraDir, fTreeDir, outputDir, 1, 0);
    }

    public DataProcessor(File spectraDir, File fTreeDir, File outputDir, int numInstancesToProcess){
        if(spectraDir.isDirectory() && fTreeDir.isDirectory() && outputDir.isDirectory()){
            this.spectraDir = spectraDir;
            this.fTreeDir = fTreeDir;
            this.outputDir = outputDir;

            // FILTERING:
            // - a fTree and a CombinatorialSubtree are stored in a JSON file
            // - if there is a file in 'outputDir' whose filename exists in 'fTreeDir',
            //   it is assumed that these files belong to the same instance
            System.out.println("Filter out already processed instances in case you want to compute new subtrees.");
            String[] resultFileNames = this.outputDir.list();
            List<String> filteredFTreeFileNames = Arrays.stream(fTreeDir.list()).
                    filter(fileName -> {
                        for(String resultFileName : resultFileNames){
                            if(fileName.equals(resultFileName)) return false;
                        }
                        return true;
                    }).collect(Collectors.toList());
            System.out.println(filteredFTreeFileNames.size()+" instances remain after filtering.");

            if(filteredFTreeFileNames.size() < numInstancesToProcess){
                numInstancesToProcess = filteredFTreeFileNames.size();
            }
            System.out.println("Sort and shuffle these instances and choose "+numInstancesToProcess+" Instances.");
            Collections.sort(filteredFTreeFileNames);
            Collections.shuffle(filteredFTreeFileNames, new Random(SHUFFLE_SEED));

            this.fileNames = new String[numInstancesToProcess];
            for(int i = 0; i < numInstancesToProcess; i++){
                this.fileNames[i] = filteredFTreeFileNames.get(i).replaceFirst("\\.json", "");
            }
        }else{
            throw new RuntimeException("The given abstract path names don't exist or aren't a directory.");
        }
    }

    private MolecularGraph readMolecule(String fileName) throws IOException, InvalidSmilesException, UnknownElementException {
        File file = new File(this.spectraDir, fileName);
        try(BufferedReader fileReader = new BufferedReader(new FileReader(file))) {

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

    private ArrayList<String[]> divideInstancesIntoBatches(int numOfBatches){
        if(this.fileNames.length <= numOfBatches){
            ArrayList<String[]> batches = new ArrayList<>(this.fileNames.length);
            for(String fileName : this.fileNames){
                String[] batch = new String[]{fileName};
                batches.add(batch);
            }
            return batches;
        }else{
            ArrayList<String[]> batches = new ArrayList<>(numOfBatches);
            int numInstancesPerBatch = this.fileNames.length / numOfBatches;
            int rest = this.fileNames.length - (numInstancesPerBatch * numOfBatches);

            for(int i = 0; i < numOfBatches; i++){
                int startIndex, endIndex; // startIndex is inclusive, endIndex is exclusive
                if(i < rest){
                    startIndex = i * (numInstancesPerBatch + 1);
                    endIndex = startIndex + numInstancesPerBatch + 1;
                }else{
                    startIndex = i * numInstancesPerBatch + rest;
                    endIndex = startIndex + numInstancesPerBatch;
                }
                String[] batch = new String[endIndex - startIndex];
                for(int j = startIndex; j < endIndex; j++){
                    batch[j - startIndex] = this.fileNames[j];
                }
                batches.add(batch);
            }
            return batches;
        }
    }

    private CombinatorialSubtreeCalculator getComputedSubtreeCalculator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring, CombinatorialFragmenter.Callback2 fragmentationConstraint, SubtreeComputationMethod subtreeCompMethod) throws Exception{
        CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(molecule, scoring);
        CombinatorialGraph graph = fragmenter.createCombinatorialFragmentationGraph(fragmentationConstraint);
        CombinatorialGraphManipulator.addTerminalNodes(graph, scoring, fTree);
        return this.getComputedSubtreeCalculator(fTree, graph, scoring, subtreeCompMethod);
    }

    private CombinatorialSubtreeCalculator getComputedSubtreeCalculator(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring, SubtreeComputationMethod subtreeCompMethod) throws Exception {
        CombinatorialSubtreeCalculator subtreeCalc;
        switch(subtreeCompMethod){
            case ILP:
                PCSTFragmentationTreeAnnotator ilpCalc = new PCSTFragmentationTreeAnnotator(fTree, graph, scoring);
                ilpCalc.initialize(null);
                ilpCalc.computeSubtree();
                subtreeCalc = ilpCalc;
                break;
            case PRIM:
                PrimSubtreeCalculator primCalc = new PrimSubtreeCalculator(fTree, graph, scoring);
                primCalc.initialize(null);
                primCalc.computeSubtree();
                subtreeCalc = primCalc;
                break;
            case INSERTION:
                InsertionSubtreeCalculator insertionCalc = new InsertionSubtreeCalculator(fTree, graph ,scoring);
                insertionCalc.initialize(null);
                insertionCalc.computeSubtree();
                subtreeCalc = insertionCalc;
                break;
            case CRITICAL_PATH_1:
                CriticalPathSubtreeCalculator cp1Calc = new CriticalPathSubtreeCalculator(fTree, graph, scoring, true);
                cp1Calc.initialize(null);
                cp1Calc.computeSubtree();
                subtreeCalc = cp1Calc;
                break;
            case CRITICAL_PATH_2:
                CriticalPathSubtreeCalculator cp2Calc = new CriticalPathSubtreeCalculator(fTree, graph, scoring, false);
                cp2Calc.initialize(null);
                cp2Calc.computeSubtree();
                subtreeCalc = cp2Calc;
                break;
            case CRITICAL_PATH_3:
                CriticalPathInsertionSubtreeCalculator cp3Calc = new CriticalPathInsertionSubtreeCalculator(fTree, graph, scoring);
                cp3Calc.initialize(null);
                cp3Calc.computeSubtree();
                subtreeCalc = cp3Calc;
                break;
            default:
                CriticalPathSubtreeCalculator defaultCalc = new CriticalPathSubtreeCalculator(fTree, graph, scoring, true);
                defaultCalc.initialize(null);
                defaultCalc.computeSubtree();
                subtreeCalc = defaultCalc;
                break;
        }
        return subtreeCalc;
    }

    private void unreference(MolecularGraph molecule, FTree fTree, CombinatorialFragmenterScoring scoring, CombinatorialSubtreeCalculator subtreeCalc){
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
        for(String fileName : this.fileNames){
            Callable<Object> task = Executors.callable(() -> {
                try {
                    MolecularGraph molecule = this.readMolecule(fileName + ".ms");
                    FTree fTree = this.readFTree(fileName + ".json");
                    DirectedBondTypeScoring scoring = new DirectedBondTypeScoring(molecule);

                    CombinatorialSubtreeCalculator subtreeCalc = this.getComputedSubtreeCalculator(fTree, molecule, scoring, fragmentationConstraint, subtreeCompMethod);

                    CombinatorialSubtreeCalculatorJsonWriter.writeResultsToFile(subtreeCalc, new File(this.outputDir, fileName + ".json"));
                    this.unreference(molecule, fTree, scoring, subtreeCalc);
                }catch (Exception e) {
                    System.out.println("An error occurred during processing instance "+fileName);
                    File resultFile = new File(this.outputDir, fileName + ".json");
                    if(resultFile.exists()){
                        boolean wasDeleted = resultFile.delete();
                        if(wasDeleted) {
                            System.out.println(fileName+".json was successfully deleted.");
                        }else{
                            System.out.println("Could not delete "+fileName+".json.");
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

        System.out.println(futures.size()+" tasks of "+tasks.size()+" have been processed and the executor service will be shutdown.");
        executor.shutdown();
    }

    public void compareSubtreeComputationMethods(CombinatorialFragmenter.Callback2 fragmentationConstraint) throws InterruptedException, IOException, ExecutionException {
        // We know: this.fileNames contains the filenames of all instances that have to be processed.
        // For each instance, we want to compare all subtree computation methods.
        // That means, we want to compute the fragmentation graph and the subtrees, their score and running times and
        // the tanimoto coefficient between each heuristic subtree and the optimal subtree (ILP).
        // We store these results in a CSV file which is saved in the specified output directory.

        // INITIALISATION:
        // Initialise the ExecutorService:
        final int NUM_AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_AVAILABLE_PROCESSORS);

        // Create an array with all SubtreeComputationMethods and determine the index of the ILP method:
        final SubtreeComputationMethod[] methods = SubtreeComputationMethod.values(); // array of the methods in the order they're declared
        final int ilpIdx = SubtreeComputationMethod.ILP.ordinal();

        // CREATING ALL TASKS:
        // Each instance in this.fileNames corresponds to one task.
        // Each task does the following:
        // - load the molecule and the FTree from their files in 'spectraDir' and 'fTreeDir', and create a scoring object
        // - create a CombinatorialGraph with terminal nodes and measure the running time of construction
        // - compute the CombinatorialSubtree for each method (ILP, Insertion, Prim, Critical Path1-3)
        // - measure the running the times, the score and the tanimoto coefficient against the ILP solution
        // - save all data in a string matching the CSV ordering and return it
        System.out.println("Collect all tasks...");
        ArrayList<Callable<String>> tasks = new ArrayList<>(this.fileNames.length);
        for(String fileName : this.fileNames){
            Callable<String> task = () -> {
                // 1.) Initialise the molecule, the FTree and the scoring object:
                MolecularGraph molecule = this.readMolecule(fileName+".ms");
                FTree fTree = this.readFTree(fileName+".json");
                DirectedBondTypeScoring scoring = new DirectedBondTypeScoring(molecule);

                // 2.) Create the CombinatorialGraph and add the terminal nodes to it:
                // Measure the running time for constructing such fragmentation graph.
                long timeStamp, constructionRuntime;
                CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(molecule, scoring);
                timeStamp = System.currentTimeMillis();
                CombinatorialGraph graph = fragmenter.createCombinatorialFragmentationGraph(fragmentationConstraint);
                CombinatorialGraphManipulator.addTerminalNodes(graph, scoring, fTree);
                constructionRuntime = System.currentTimeMillis() - timeStamp;

                // 3.) Compute the CombinatorialSubtree of 'graph' with each SubtreeComputationMethod:
                // Save the score, the running time and the tanimoto coefficient between the ILP solution for each method.
                CombinatorialSubtree[] subtrees = new CombinatorialSubtree[methods.length];
                long[] runningTimes = new long[methods.length];
                double[] scores = new double[methods.length];
                double[] tanimotoScores = new double[methods.length];

                // 3.1: For each method, compute the subtree, measure the runtime and score:
                for(int i = 0; i < methods.length; i++){
                    SubtreeComputationMethod method = methods[i];
                    timeStamp = System.currentTimeMillis();
                    CombinatorialSubtreeCalculator subtreeCalc = this.getComputedSubtreeCalculator(fTree, graph, scoring, method);

                    runningTimes[i] = System.currentTimeMillis() - timeStamp;
                    subtrees[i] = subtreeCalc.getSubtree();
                    scores[i] = subtreeCalc.getScore();
                }

                // 3.2: For each method, compute the tanimoto coefficient between the subtree and the ILP subtree:
                CombinatorialSubtree ilpSubtree = subtrees[ilpIdx];
                TIntIntHashMap edgeValue2edgeIdx = graph.edgeValue2Index();
                int maxBitSetLength = graph.maximalBitSetLength();

                for(int i = 0; i < methods.length; i++){
                    CombinatorialSubtree subtree = subtrees[i];
                    tanimotoScores[i] = CombinatorialSubtreeManipulator.tanimoto(subtree, ilpSubtree, edgeValue2edgeIdx, maxBitSetLength);
                }

                // 4.) Save all data into one string:
                // The string should look like this:
                // "<instance name>,<constrRunningtime>,{Running times},{Scores},{Tanimoto-Scores}"
                StringBuilder strBuilder = new StringBuilder(fileName+","+constructionRuntime);
                for(int i = 0; i < methods.length; i++) strBuilder.append(","+runningTimes[i]);
                for(int i = 0; i < methods.length; i++) strBuilder.append(","+scores[i]);
                for(int i = 0; i < methods.length; i++) strBuilder.append(","+tanimotoScores[i]);

                return strBuilder.toString();
            };
            tasks.add(task);
        }
        System.out.println("All tasks were collected and will be executed now...");
        List<Future<String>> futures = executor.invokeAll(tasks);

        System.out.println("All tasks are computed. Store all data into a CSV file and save it into "+this.outputDir+".");
        StringBuilder startingString = new StringBuilder("instance_name,construction_runtime");
        for(int i = 0; i < methods.length; i++) startingString.append(","+methods[i].name()+"_runtime");
        for(int i = 0; i < methods.length; i++) startingString.append(","+methods[i].name()+"_score");
        for(int i = 0; i < methods.length; i++) startingString.append(","+methods[i].name()+"_tanimoto");

        File csvOutputFile = new File(this.outputDir, "subtreeCompMethod_comparison_results.csv");
        try(BufferedWriter fileWriter = Files.newBufferedWriter(csvOutputFile.toPath())){
            fileWriter.write(startingString.toString());

            for(Future<String> future : futures){
                String resultString = future.get();

                fileWriter.newLine();
                fileWriter.write(resultString);
            }
        }
        System.out.println("The executor service will be shutdown.");
        executor.shutdown();
    }

    public void runStructureIdentification(CombinatorialFragmenter.Callback2 fragmentationConstraint, SubtreeComputationMethod subtreeCompMethod){
        throw new UnsupportedOperationException("This structure identification method is not supported.");
    }

    public enum SubtreeComputationMethod{
        ILP,
        PRIM,
        INSERTION,
        CRITICAL_PATH_1,
        CRITICAL_PATH_2,
        CRITICAL_PATH_3
    }

}
