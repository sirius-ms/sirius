package de.unijena.bioinf.fragmenter;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class DataProcessorExecutor {

    private enum ProcessingMethod{
        COMPUTE_SUBTREES,
        COMPARISON,
        STRUCTURE_RANKING
    }

    @Option(name = "-scoring", aliases = {"--scoringFile"}, usage = "Specify the scoring parameters that should be used.", required = true)
    private String scoringFile;

    @Option(name = "-spectra", aliases = {"--spectraDir"}, usage = "The spectra files for computing the combinatorial subtrees.")
    private String spectraDir;

    @Option(name = "-predictions", aliases = "--predictionsDir", usage = "The files containing predicted molecular structures " +
            "for ranking these structures with the combinatorial fragmenter.")
    private String predictionsDir;

    @Option(name = "-fTrees", aliases = "--fTreeDir", usage = "The fragmentation trees for computing the combinatorial subtrees.", required = true)
    private String fTreeDir;

    @Option(name = "-o", aliases = "--outputDir", usage = "The output directory in which the computed data will be saved.", required = true)
    private String outputDir;

    @Option(name = "-pm", aliases = "--processingMethod", usage = "This parameter defines which data should be produced.", required = true)
    private ProcessingMethod processingMethod;

    @Option(name = "-scm", aliases = {"--subtreeComputationMethod", "--subtreeCompMethod"}, usage = "The method for computing the combinatorial subtree.")
    private SubtreeComputationMethod subtreeComputationMethod;

    @Option(name = "-d", aliases = {"--fd", "--fragmentationDepth"}, usage = "The fragmentation depth used for computing the subtrees.", required = true)
    private int fragmentationDepth;

    @Option(name = "-allData", aliases = "--processAllData", usage = "A boolean value that asks if all instances in the spectra and fTree directories should be processed.", forbids = {"-np", "-idx"})
    private boolean processAllData;

    @Option(name = "-np", aliases = {"--numberPartitions"}, usage = "The number of partitions in which the data set will be divided.", forbids = {"-allData"})
    private int numPartitions;

    @Option(name = "-idx", aliases = "--partitionIdx", usage = "The partition which will be processed.", depends = {"-np"}, forbids = {"-allData"})
    private int idxPartition;


    public static void main(String[] args){
        DataProcessorExecutor executor = new DataProcessorExecutor();
        CmdLineParser parser = new CmdLineParser(executor);
        try{
            parser.parseArgument(args);

            // INITIALISATION:
            // Initialise mandatory arguments:
            File scoringFile = new File(executor.scoringFile);
            File fTreeDir = new File(executor.fTreeDir);
            File outputDir = new File(executor.outputDir);

            ProcessingMethod processingMethod = executor.processingMethod;
            int fragmentationDepth = executor.fragmentationDepth;

            // Initialise the fragmentation constraint:
            CombinatorialFragmenter.Callback2 fragmentationConstraint = (node, nnodes, nedges) -> node.getDepth() < fragmentationDepth;

            // Load the scoring model from the given file and initialise the parameters of the DirectedBondTypeScoring:
            DirectedBondTypeScoring.loadScoringFromFile(scoringFile);

            // CONSTRUCTOR-CALLING + PROCESSING:
            // A specific constructor of DataProcessor will be called depending on the ProcessingMethod:
            Scanner scanner = new Scanner(System.in);

            if(processingMethod.equals(ProcessingMethod.COMPUTE_SUBTREES)){
                if(executor.spectraDir != null){
                    File spectraDir = new File(executor.spectraDir);

                    DataProcessor dataProcessor;
                    if(executor.processAllData){
                        dataProcessor = new DataProcessor(spectraDir,null,fTreeDir, outputDir);
                    }else{
                        dataProcessor = new DataProcessor(spectraDir, null, fTreeDir, outputDir, executor.numPartitions, executor.idxPartition);
                    }
                    promptEnterKeyToContinue(scanner,"The DataProcessor is initialised. Press ENTER to continue.");
                    dataProcessor.computeCombinatorialSubtrees(fragmentationConstraint, executor.subtreeComputationMethod);
                }else{
                    throw new RuntimeException("The directory containing the spectra was not set.");
                }
            }else if(processingMethod.equals(ProcessingMethod.COMPARISON)){
                if(executor.spectraDir != null){
                    File spectraDir = new File(executor.spectraDir);

                    String outputFileName = typeInString(scanner,"Enter the name of the output file: ");
                    Collection<String> processedInstances = readProcessedInstanceFileNames(new File(outputDir, outputFileName));

                    DataProcessor dataProcessor;
                    if(executor.processAllData){
                        dataProcessor = new DataProcessor(spectraDir, null, fTreeDir, outputDir, processedInstances);
                    }else{
                        dataProcessor = new DataProcessor(spectraDir, null, fTreeDir, outputDir, processedInstances, executor.numPartitions, executor.idxPartition);
                    }
                    promptEnterKeyToContinue(scanner,"The DataProcessor is initialised. Press ENTER to continue.");
                    dataProcessor.compareSubtreeComputationMethods(fragmentationConstraint, outputFileName);
                }else{
                    throw new RuntimeException("The directory containing the spectra was not set.");
                }
            }else{
                if(executor.predictionsDir != null){
                    File predictionsDir = new File(executor.predictionsDir);

                    DataProcessor dataProcessor;
                    if(executor.processAllData){
                        dataProcessor = new DataProcessor(null, predictionsDir, fTreeDir, outputDir);
                    }else{
                        dataProcessor = new DataProcessor(null, predictionsDir, fTreeDir, outputDir, executor.numPartitions, executor.idxPartition);
                    }
                    promptEnterKeyToContinue(scanner,"The DataProcessor is initialised. Press ENTER to continue.");
                    dataProcessor.runStructureRanking(fragmentationConstraint, executor.subtreeComputationMethod);
                }else{
                    throw new RuntimeException("The directory containing the predicted structure files was not set.");
                }
            }
            scanner.close();
        } catch (CmdLineException e) {
            System.err.println("The arguments were not set correctly.");
            e.printStackTrace();
            parser.printUsage(System.err);
        } catch (IOException e) {
            System.err.println("An error occurred while loading the scoring model or reading/writing the results.");
            e.printStackTrace();
        } catch (InterruptedException | ExecutionException e ) {
            System.err.println("An error occurred during the computation of an instance.");
            e.printStackTrace();
        }
    }

    private static void promptEnterKeyToContinue(Scanner scanner, String msg){
        System.out.print(msg);
        scanner.nextLine();
    }

    private static String typeInString(Scanner scanner, String msg){
        System.out.print(msg);
        return scanner.nextLine();
    }

    private static Collection<String> readProcessedInstanceFileNames(File outputFile) throws IOException{
        if(outputFile.isFile()){
            try(BufferedReader fileReader = Files.newBufferedReader(outputFile.toPath())){
                // The outputFile is a CSV file with 'instance_name' as its first column and ',' as delimiter.
                // The first line is a description of the columns -skip it.
                fileReader.readLine();

                // Now, create a List to store all instance names and iterate over the file:
                ArrayList<String> processedInstanceFileNames = new ArrayList<>();
                String currentLine = fileReader.readLine();

                while(currentLine != null){
                    String instanceFileName = currentLine.split(",")[0];
                    processedInstanceFileNames.add(instanceFileName);
                    currentLine = fileReader.readLine();
                }
                return processedInstanceFileNames;
            }
        }else{
            return Collections.emptyList();
        }
    }

}
