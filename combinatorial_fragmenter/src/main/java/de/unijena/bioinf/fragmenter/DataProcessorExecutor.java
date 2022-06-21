package de.unijena.bioinf.fragmenter;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class DataProcessorExecutor {

    private enum ProcessingMethod{
        COMPUTE_SUBTREES,
        COMPARISON,
        STRUCTURE_IDENTIFICATION
    }

    @Option(name = "-scoring", aliases = {"--scoringFile"}, usage = "Specify the scoring parameters that should be used.", required = true)
    private String scoringFile;

    @Option(name = "-spectra", aliases = {"--spectraDir"}, usage = "The spectra files for computing the combinatorial subtrees.", required = true)
    private String spectraDir;

    @Option(name = "-fTrees", aliases = "--fTreeDir", usage = "The fragmentation trees for computing the combinatorial subtrees.", required = true)
    private String fTreeDir;

    @Option(name = "-o", aliases = "--outputDir", usage = "The output directory in which the computed data will be saved.", required = true)
    private String outputDir;

    @Option(name = "-pm", aliases = "--processingMethod", usage = "This parameter defines which data should be produced.", required = true)
    private ProcessingMethod processingMethod;

    @Option(name = "-scm", aliases = {"--subtreeComputationMethod", "--subtreeCompMethod"}, usage = "The method for computing the combinatorial subtree.")
    private DataProcessor.SubtreeComputationMethod subtreeComputationMethod;

    @Option(name = "-d", aliases = {"--fd", "--fragmentationDepth"}, usage = "The fragmentation depth used for computing the subtrees.", required = true)
    private int fragmentationDepth;

    @Option(name = "-allData", aliases = "--processAllData", usage = "A boolean value that asks if all instances in the spectra and fTree directories should be processed.", forbids = {"-p", "-np", "-idx", "-n"})
    private boolean processAllData;

    @Option(name = "-p", aliases = "-partition", usage = "A boolean value that decided if the instances should be partitioned.", forbids = {"-allData", "-n"})
    private boolean partitionTheData;

    @Option(name = "-np", aliases = {"--numberPartitions"}, usage = "The number of partitions in which the data set will be divided.", depends = {"-p"}, forbids = {"-allData", "-n"})
    private int numPartitions;

    @Option(name = "-idx", aliases = "--partitionIdx", usage = "The partition which will be processed.", depends = {"-p", "-np"}, forbids = {"-allData", "-n"})
    private int idxPartition;

    @Option(name = "-n", aliases = "--numberOfInstances", usage = "The number of instances to process without partitioning.", forbids = {"-allData", "-p", "-np", "-idx"})
    private int numInstancesToProcess;


    public static void main(String[] args){
        DataProcessorExecutor executor = new DataProcessorExecutor();
        CmdLineParser parser = new CmdLineParser(executor);
        try{
            parser.parseArgument(args);

            // INITIALISATION:
            // Initialise mandatory arguments:
            File scoringFile = new File(executor.scoringFile);
            File spectraDir = new File(executor.spectraDir);
            File fTreeDir = new File(executor.fTreeDir);
            File outputDir = new File(executor.outputDir);

            ProcessingMethod processingMethod = executor.processingMethod;
            int fragmentationDepth = executor.fragmentationDepth;

            // Initialise the fragmentation constraint:
            CombinatorialFragmenter.Callback2 fragmentationConstraint = node -> node.getDepth() < fragmentationDepth;

            // Load the scoring model from the given file and initialise the parameters of the DirectedBondTypeScoring:
            DirectedBondTypeScoring.loadScoringFromFile(scoringFile);

            // Initialise the DataProcessor with the desired constructor:
            DataProcessor dataProcessor;
            if(executor.processAllData){
                dataProcessor = new DataProcessor(spectraDir, fTreeDir, outputDir);
            }else{
                if(executor.partitionTheData){
                    int numPartitions = executor.numPartitions;
                    int idxPartition = executor.idxPartition;
                    dataProcessor = new DataProcessor(spectraDir, fTreeDir, outputDir, numPartitions, idxPartition);
                }else{
                    int numInstancesToProcess = executor.numInstancesToProcess;
                    dataProcessor = new DataProcessor(spectraDir, fTreeDir, outputDir, numInstancesToProcess);
                }
            }
            promptEnterKeyToContinue("The DataProcessor is initialised. Press ENTER to start processing...");

            // COMPUTATION:
            //  Choose the desired processing method:
            if(executor.processingMethod.equals(ProcessingMethod.COMPARISON)){
                dataProcessor.compareSubtreeComputationMethods(fragmentationConstraint);
            }else{
                DataProcessor.SubtreeComputationMethod subtreeComputationMethod = executor.subtreeComputationMethod;
                if(executor.processingMethod.equals(ProcessingMethod.COMPUTE_SUBTREES)){
                    dataProcessor.computeCombinatorialSubtrees(fragmentationConstraint, subtreeComputationMethod);
                }else if(executor.processingMethod.equals(ProcessingMethod.STRUCTURE_IDENTIFICATION)){
                    dataProcessor.runStructureIdentification(fragmentationConstraint, subtreeComputationMethod);
                }
            }
        } catch (CmdLineException e) {
            System.err.println("The arguments were not set correctly.");
            e.printStackTrace();
            parser.printUsage(System.err);
        } catch (IOException e) {
            System.err.println("An error occurred while loading the scoring model.");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("An error occurred during the computation of an instance.");
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
