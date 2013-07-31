package de.unijena.bioinf.GCMSAnalysis.cli;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.EIIntensityDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.GCMSFragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.MostRelevantPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.EICommonLossEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.GCMSAnalysis.GCMSFactory;
import de.unijena.bioinf.GCMSAnalysis.GCMSMeasurementProfile;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaGCMSExperiment;
import de.unijena.bioinf.babelms.ms.JenaGCMSParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    final static boolean VERBOSE = true;

    public static int NUMBEROFCPUS = 4;

    private static boolean mpKnown;

    //elements
    private static boolean useHalogens;
    private static boolean useChlorine;
    private static boolean useDerivates;
    private static boolean usePFB;
    private static boolean useTMS;


    //scoreIsotopes
    private static boolean scoreIsotopes = false; //by now scoreIsotopes are not scored
    private static boolean removeiso;

    //algorithm
    private static boolean ilp;
    private static int peaksToScore;
    private static boolean forest;
    private static double strictFilter;

    //output
    private static int numberOfTrees; //how many trees to compute
    private static File outputDir;




    public static void main(String... args){
        final Cli<Options> cli = CliFactory.createCli(Options.class);
        final Options options;
        try {
            options = cli.parseArguments(args);
        } catch (ArgumentValidationException e) {
            final PrintStream out;
            if (e instanceof HelpRequestedException) {
                out = System.out;
            } else {
                out = System.err;
                out.println("Error while parsing command line arguments: " + e.getMessage());
            }
            out.println(cli.getHelpMessage());
            out.println("usage:"); //todo help message
            return;
        }

        //molecule peak known|unknown
        String mpKnownString = options.getMPKnown().toLowerCase();
        if (mpKnownString == null){
            System.err.println("--mp option not set.");
            System.err.println(cli.getHelpMessage());
            System.exit(1);
        } else if (mpKnownString.equals("unknown")){
            mpKnown = false;
        } else if (mpKnownString.equals("known")){
            mpKnown = true;
        } else {
            System.err.println("Error parsing --mp option. Only 'known' and 'unknown' are allowed not "+mpKnownString+".");
            System.err.println(cli.getHelpMessage());
            System.exit(1);
        }

//        //parent mass
//        if (options.getParentMass() != null){
//            if (!mpKnown) {
//                System.err.println("Error parsing --pm option. Molecule peak unknown but mass set.");
//                System.err.println(cli.getHelpMessage());
//                System.exit(1);
//            }
//            parentMass = options.getParentMass();
//        }
//        //pmd
//        if (options.getMolecularIonFormula() != null){
//            if (!mpKnown) {
//                System.err.println("Error parsing --pmd option. Molecule peak unknown but formula set.");
//                System.err.println(cli.getHelpMessage());
//                System.exit(1);
//            }
//            pmd = MolecularFormula.parse(options.getMolecularIonFormula());
//        }
        if (options.getPeaksToScore() == null){
            peaksToScore = Integer.MAX_VALUE;
        } else if (options.getPeaksToScore()<=0){
            System.err.println("Error parsing --peaks option. \n Number of peaks to score has to be positive.");
            System.err.println(cli.getHelpMessage());
            System.exit(1);
        }  else {
            peaksToScore = options.getPeaksToScore();
        }
        removeiso = options.isRemoveIsotopePeaks();
        strictFilter = options.getFilterLowIntensity();
        forest = options.isForest();
        ilp = options.isILP();


        //todo some option have to exclude each other? -> test
        useHalogens = options.isAddHalogens();
        useChlorine = options.isAddCl();
        useDerivates = options.isAddDerivate();
        useTMS = options.isAddTms();
        usePFB = options.isAddPfb();

        if (options.getOutputSize().toLowerCase().equals("best")){
            numberOfTrees = 1;
        } else if (options.getOutputSize().toLowerCase().equals("all")){
            numberOfTrees = Integer.MAX_VALUE;
        } else {
            numberOfTrees = Integer.parseInt(options.getOutputSize());
        }

        if (options.getOutputDir() == null) {
            outputDir = new File(options.getInputPath());
            if (!outputDir.isDirectory()){
                System.err.println("Please set output directory.");
                System.err.println(cli.getHelpMessage());
                System.exit(1);
            }
        } else {
            outputDir = new File(options.getOutputDir());
            if (!outputDir.isDirectory()){
                System.err.println("Output path is no directory");
                System.err.println(cli.getHelpMessage());
                System.exit(1);
            }
        }



        //initializing
        GCMSFactory factory = new GCMSFactory();
        factory.setUseChlorine(useChlorine);
        factory.setUseHalogens(useHalogens);
        factory.setUsePFB(usePFB);
        factory.setUseTMS(useTMS);
        factory.setUseDerivates(useDerivates);

        GCMSFragmentationPatternAnalysis pipeline = factory.getGCMSAnalysis();
        if (ilp){
            final GurobiSolver solver = new GurobiSolver();
            solver.setNumberOfCPUs(NUMBEROFCPUS);
            pipeline.setTreeBuilder(solver);
        }
        if (peaksToScore != Integer.MAX_VALUE) pipeline.getPostProcessors().add(new MostRelevantPeaksFilter(peaksToScore));
        if (strictFilter>0d) pipeline.getPostProcessors().add(new NoiseThresholdFilter(strictFilter/100)); //todo in percent??
        if (removeiso) pipeline.setRemoveIsotopePeaks(removeiso);
        if (scoreIsotopes) {
            //todo implement
        }

        ProcessedInput processedInput;
        MolecularFormula expectedFormula = null;
        List<FragmentationTree> trees;



        File inputPath = new File(options.getInputPath());
        File[] inputFiles;
        if (inputPath.isFile()){
            inputFiles = new File[]{inputPath};
        } else {
            inputFiles = inputPath.listFiles();
        }

        GenericParser<JenaGCMSExperiment> parser = new GenericParser<JenaGCMSExperiment>(new JenaGCMSParser(mpKnown));
        for (File inputFile : inputFiles) {
            try {
                if (VERBOSE) System.out.println("parse file: "+inputFile.getAbsolutePath());

                JenaGCMSExperiment gcmsExperiment = parser.parseFile(inputFile);
                gcmsExperiment.setMeasurementProfile(new GCMSMeasurementProfile(useHalogens, useChlorine, usePFB, useTMS));

                processedInput = pipeline.preprocessing(gcmsExperiment);

                trees = new ArrayList<FragmentationTree>();


                if (mpKnown){
                    ScoredMolecularFormula correctFormula = null;
                    // search for correct formula in decompositions
                    expectedFormula = gcmsExperiment.getMolecularFormula();
                    if (expectedFormula==null){
                        System.err.println("Not able to parse formula from file "+inputFile);
                        continue;
                    }
                    for (ScoredMolecularFormula f : processedInput.getParentMassDecompositions()) {
                        if (f.getFormula().equals(expectedFormula)) {
                            correctFormula = f;
                            break;
                        }
                    }
                    if (correctFormula==null){
                       System.err.println("Parsed formula \'"+expectedFormula+"\' from file "+inputFile+" not found as parent mass decomposition.");
                        continue;
                    }
                    final FragmentationGraph graph = pipeline.buildGraph(processedInput, correctFormula);
                    FragmentationTree correctTree = pipeline.computeTree(graph);
                    trees.add(correctTree);
                } else if (forest){
                    FragmentationGraph dummyGraph = pipeline.buildDummyGraph(processedInput);
                    FragmentationTree dummyTree = pipeline.computeTree(dummyGraph);
                    pipeline.setNiceDummyNodeAnnotation(dummyTree);
                    trees.add(dummyTree);
                } else {
                    FragmentationGraph dummyGraph = pipeline.buildDummyGraph(processedInput);
                    List<FragmentationTree> bestScoredTrees = pipeline.computeMultipleTrees(dummyGraph, numberOfTrees);
                    for (FragmentationTree tree : bestScoredTrees) {
                        pipeline.setNiceDummyNodeAnnotation(tree);
                    }
                    trees.addAll(bestScoredTrees);

                    //todo ilp????
                }

                for (int i = 0; i < Math.min(trees.size(), numberOfTrees); i++) {
                    writeTreeToFile(new File(outputDir.getAbsolutePath()+System.getProperty("file.separator")+"Tree-"+i+".dot"), trees.get(i), pipeline, inputFile);
                }

            } catch (IOException e){
                System.err.println("Error while parsing "+inputFile);
                e.printStackTrace();
            }
        }




    }


    protected static void writeTreeToFile(File f, FragmentationTree tree, FragmentationPatternAnalysis pipeline, File inputFile) {
        FileWriter fw = null;
        try {
            fw =  new FileWriter(f);
            final TreeAnnotation ano = new TreeAnnotation(tree, pipeline);
            new FTDotWriter().writeTree(fw, tree, ano.getAdditionalProperties(),  ano.getVertexAnnotations(), ano.getEdgeAnnotations());
        } catch (IOException e) {
            System.err.println("Error while writing tree in " + f + " for input " + inputFile);
            e.printStackTrace();
        } finally {
            if (fw != null) try {
                fw.close();
            } catch (IOException e) {
                System.err.println("Error while writing tree in " + f + " for input " + inputFile);
                e.printStackTrace();
            }
        }
    }
}
