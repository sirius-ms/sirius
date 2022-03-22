package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.MsIO;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
            System.out.println("Filter out already processed instances.");
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
            throw new RuntimeException("The given abstract pathnames don't exist or aren't a directory.");
        }
    }

    public DataProcessor(File spectraDir, File fTreeDir, File outputDir){
        this(spectraDir, fTreeDir, outputDir, 1, 0);
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

    public void run(CombinatorialFragmenter.Callback2 fragmentationConstraint) throws InterruptedException {
        // INITIALISATION:
        EMFragmenterScoring.rearrangementProb = 1.0; // don't punish hydrogen rearrangements at first
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // LOOP - ITERATE OVER ALL FILES:
        // For each spectrum file and its corresponding FTree file, a new task, for computing the best subtree
        // in the calculated in silico fragmentation graph and saving this result, will be submitted.
        System.out.println("Collect all tasks...");
        ArrayList<Callable<Object>> tasks = new ArrayList<>(this.fileNames.length);
        for(String fileName : this.fileNames){
            Callable<Object> task = Executors.callable(() -> {
                try {
                    System.out.println("Starting task: "+fileName);
                    MolecularGraph molecule = this.readMolecule(fileName + ".ms");
                    FTree fTree = this.readFTree(fileName + ".json");
                    EMFragmenterScoring scoring = new EMFragmenterScoring(molecule);

                    System.out.println("Task "+fileName+": Initialize and compute subtree.");
                    CriticalPathSubtreeCalculator subtreeCalc = new CriticalPathSubtreeCalculator(fTree, molecule, scoring, true);
                    subtreeCalc.initialize(fragmentationConstraint);
                    subtreeCalc.computeSubtree();

                    System.out.println("Task "+fileName+": Save results.");
                    CombinatorialSubtreeCalculatorJsonWriter.writeResultsToFile(subtreeCalc, new File(this.outputDir, fileName + ".json"));
                }catch (UnknownElementException | IOException | CDKException e) {
                    System.out.println("Task "+fileName+": AN ERROR OCCURRED!");
                    e.printStackTrace();
                }
            });
            tasks.add(task);
        }
        // COMPUTING THE TASKS:
        // Wait, until all tasks terminated.
        System.out.println("Execute all tasks...");
        Collection<Future<Object>> futures = executor.invokeAll(tasks);

        System.out.println("All tasks have been processed and the executor service will be shutdown.");
        executor.shutdown();
    }

    public static void main(String[] args){
        try{
            File spectraDir = new File(args[0]);
            File fTreeDir = new File(args[1]);
            File outputDir = new File(args[2]);
            int fragmentationDepth = Integer.parseInt(args[3]);
            int numPartitions = Integer.parseInt(args[4]);
            int idxPartition = Integer.parseInt(args[5]);

            DataProcessor dataProcessor = new DataProcessor(spectraDir, fTreeDir, outputDir, numPartitions, idxPartition);
            promptEnterKeyToContinue("The DataProcessor is initialised. Press ENTER to start processing...");
            dataProcessor.run(node -> node.depth < fragmentationDepth);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void promptEnterKeyToContinue(String msg){
        try(Scanner scanner = new Scanner(System.in)){
            System.out.println(msg);
            scanner.nextLine();
        }
    }


}
