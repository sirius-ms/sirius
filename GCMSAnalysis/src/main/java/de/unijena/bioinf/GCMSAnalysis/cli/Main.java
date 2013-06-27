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
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.EICommonLossEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.GraphFragment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.GCMSAnalysis.GCMSFactory;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
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
    //todo noise berechnen?
    //todo relative intensity mit MAX = 1 ändern (Achtung bei scorer wie gcmsNoise..., die relInt verwenden)

    //todo Tms, Dms oder Pfb Abundancy > 1 verhindern?

    final static boolean VERBOSE = true;

    private static boolean mpKnown;

    //elements
    private static boolean useHalogens;
    private static boolean useChlorine;
    private static boolean useDerivates;
    private static boolean usePFB;
    private static boolean useTMS;
    private static boolean isPFBcompound;
    private static boolean isTMScompound;

    //scoring
    private static double heteroAverage = 0.5886335; // new: 0 1 old: 0.2 0.5
    private static double heteroDev = 0.5550574;

    //isotopes
    private static boolean isotopes = false; //by now isotopes are not scored
    private static boolean removeiso;

    //algorithm
    private static boolean ilp;
    private static int peaksToScore = Integer.MAX_VALUE;
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

        if (useHalogens || useChlorine){
            heteroAverage = 0.5269789;
            heteroDev = 0.521254;
        }


        //introduce new "elements"
        PeriodicTable periodicTable = PeriodicTable.getInstance();
        periodicTable.addElement("Pfb", "Pfb", 181.007665, 1); //C7H2F5
        periodicTable.addElement("Tms", "Tms", 73.047352, 1); // C3H9Si
        periodicTable.addElement("Dms", "Dms", 58.023877, 2); //C2H6Si



        //changed -> set true for testing
        useTMS = true;

        /*
        molecule peak known
         */
        mpKnown = true;

        GCMSFactory factory = new GCMSFactory();
        factory.setHeteroAverage(heteroAverage);
        factory.setHeteroDev(heteroDev);
        GCMSFragmentationPatternAnalysis pipeline = factory.getGCMSAnalysis();
        ProcessedInput processedInput;
        /*
        if unknown = null
         */
        MolecularFormula expectedFormula = null;
        List<FragmentationTree> trees;



        File inputPath = new File(options.getInputPath());
        //changed
        inputPath = new File("C:/gcms/69-72-7.txt");
        //inputPath = new File("C:/gcms/TMS.txt");
        File[] inputFiles;
        if (inputPath.isFile()){
            inputFiles = new File[]{inputPath};
        } else {
            inputFiles = inputPath.listFiles();
        }
        try {
            GenericParser<JenaGCMSExperiment> parser = new GenericParser<JenaGCMSExperiment>(new JenaGCMSParser(mpKnown));
            for (File inputFile : inputFiles) {
                System.out.println("parse file: "+inputFile.getAbsolutePath());

                JenaGCMSExperiment gcmsExperiment = parser.parseFile(inputFile);
                gcmsExperiment.setMeasurementProfile(new SimpleMeasurementProfile());

                System.out.println(".........");
                System.out.println("molFormula: "+gcmsExperiment.getMolecularFormula().toString());


                processedInput = pipeline.preprocessing(gcmsExperiment);

                System.out.println("expectedformula:"+processedInput.getExperimentInformation().getMolecularFormula());
                System.out.println("expectedformulaWeight:"+processedInput.getExperimentInformation().getMolecularFormula().getMass());
                System.out.println("parent: "+processedInput.getParentPeak().getMz());
                // search for correct formula in decompositions
                expectedFormula = gcmsExperiment.getMolecularFormula(); //todo kann null sein!
                ScoredMolecularFormula correctFormula = null;
                for (ScoredMolecularFormula f : processedInput.getParentMassDecompositions()) {
                    if (f.getFormula().equals(expectedFormula)) {
                        correctFormula = f;
                        break;
                    }
                }


                trees = new ArrayList<FragmentationTree>();

                FragmentationGraph graph = pipeline.buildGraph(processedInput, correctFormula);
                System.out.println("graphSize:"+graph.numberOfVertices());
                for (GraphFragment graphFragment : graph.getFragments()) {
                    System.out.println(graphFragment.getFormula());
                }
                FragmentationTree corretTree = pipeline.computeTree(graph, 0);
                System.out.println("formula:"+correctFormula.toString());
                System.out.println("treeScore:"+corretTree.getScore());
                System.out.println("treeSize:"+corretTree.numberOfVertices());
                trees.add(corretTree);
//                for (ScoredMolecularFormula smf : processedInput.getParentMassDecompositions()) {
//                    final FragmentationTree fragmentationTree = pipeline.computeTree(pipeline.buildGraph(processedInput, smf));
//                    if (fragmentationTree != null) trees.add(fragmentationTree);
//                    if (VERBOSE && fragmentationTree==null) System.out.println("No tree found for decomposition: "+smf.toString());
//                }

                for (int i = 0; i < trees.size(); i++) {
                    writeTreeToFile(new File(inputFile.getAbsolutePath()+"-Tree-"+i+".dot"), trees.get(i), pipeline, inputFile);
                }


            }
        } catch (IOException e){

        }




    }


    protected static void writeTreeToFile(File f, FragmentationTree tree, FragmentationPatternAnalysis pipeline, File inputFile) {
        FileWriter fw = null;
        try {
            fw =  new FileWriter(f);
            final TreeAnnotation ano = new TreeAnnotation(tree, pipeline);
            new FTDotWriter().writeTree(fw, tree, ano.getVertexAnnotations(), ano.getEdgeAnnotations());
        } catch (IOException e) {
            System.err.println("Error while writing in " + f + " for input " + inputFile);
            e.printStackTrace();
        } finally {
            if (fw != null) try {
                fw.close();
            } catch (IOException e) {
                System.err.println("Error while writing in " + f + " for input " + inputFile);
                e.printStackTrace();
            }
        }
    }

    private static class SimpleMeasurementProfile implements MeasurementProfile {

        private final double smallErrorPpm = 7;
        private final double largeErrorPPm = 25;
        private final double smallAbsError = 5e-3;
        private final double largeAbsError = 3e-2;
        private FormulaConstraints constraints;

        SimpleMeasurementProfile() {
            PeriodicTable periodicTable =  PeriodicTable.getInstance();
            //introduce new Elements
            List<Element> usedElements = new ArrayList<Element>(Arrays.asList(periodicTable.getAllByName("C", "H", "N", "O", "P", "S")));
            if (useHalogens){
                EICommonLossEdgeScorer.neutralLossList[3] += " HF HCl Br";
                EICommonLossEdgeScorer.neutralLossList[2] += " F Cl I";
                usedElements.addAll(Arrays.asList(periodicTable.getAllByName("Br", "F", "I", "Cl")));
            }
            if (useChlorine){
                if (!useHalogens) {
                    EICommonLossEdgeScorer.neutralLossList[3] += " HCl";
                    EICommonLossEdgeScorer.neutralLossList[2] += " Cl";
                    usedElements.add(periodicTable.getByName("Cl"));
                }
            }

            if (usePFB){
                EICommonLossEdgeScorer.neutralLossList[3] += " PfbOH PfbO Pfb";
                usedElements.add(periodicTable.getByName("Pfb"));
            }
            if (useTMS){
                EICommonLossEdgeScorer.neutralLossList[3] += " OTms";
                usedElements.add(periodicTable.getByName("Tms"));
                usedElements.add(periodicTable.getByName("Dms"));
            }


            for (Element usedElement : usedElements) {
                System.out.println("#"+usedElement.getSymbol());
            }
            ChemicalAlphabet simpleAlphabet = new ChemicalAlphabet(usedElements.toArray(new Element[0]));

            this.constraints = new FormulaConstraints(simpleAlphabet);


            //todo wirklich so strikt?
            FormulaFilter filter = new FormulaFilter() {
                @Override
                public boolean isValid(MolecularFormula formula) {
                    if (formula.rdbe()>=0) return true;
                    return false;
                }
            };
            constraints.addFilter(filter);

            //constraints.addFilter(new ValenceFilter(-0.5d));
        }

        @Override
        public EIIntensityDeviation getStandardMs1MassDeviation() {
            return new EIIntensityDeviation(smallErrorPpm, largeErrorPPm, smallAbsError, largeAbsError);
        }

        @Override
        public Deviation getAllowedMassDeviation() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Deviation getStandardMassDifferenceDeviation() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Deviation getStandardMs2MassDeviation() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public double getMedianNoiseIntensity() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public FormulaConstraints getFormulaConstraints() {
            return constraints;
        }

        @Override
        public double getExpectedIntensityDeviation() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

}
