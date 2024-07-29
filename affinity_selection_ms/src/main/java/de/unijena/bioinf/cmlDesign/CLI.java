package de.unijena.bioinf.cmlDesign;

import de.unijena.bioinf.cmlDesign.io.BuildingBlockReader;
import de.unijena.bioinf.cmlDesign.io.BuildingBlockWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openscience.cdk.exception.CDKException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.regex.Pattern;

public class CLI {

    @Option(name = "-bbs", aliases = {"--bbFile", "--buildingBlocks"}, required = true,
            usage = "To determine the path to the .tsv file containing the building blocks as SMILES strings.")
    private String bbFilePath;
    @Option(name = "-l", aliases = {"--losses", "--massLosses"}, required = true,
            usage = "This command has to be used to specify the mass losses.\n " +
            "During the synthesis of the combinatorial molecule library specific substructures of the building blocks can fall of. " +
            "Thus, a mass loss must be specified for each set of building blocks.\n " +
            "Example: \"[18.010565, 18.010565, 17.002740]\"")
    private String massLossesString;
    @Option(name = "-ppm", required = true, usage = "To specify the mass accuracy in ppm.")
    private double ppm;
    @Option(name = "-b", aliases = "--blowupFactor",
            usage = "This factor is used to convert the masses into integer numbers\n. " +
            "The higher the factor, the more accurate the calculation. However, the calculation also becomes slower as the factor increases.\n " +
            "This factor is set to 10^5 by default.")
    private double blowupFactor;
    @Option(name = "-m", aliases = {"--evaluationMeasure", "--measure"},
            usage = "The measured used for assigning a score to a combinatorial molecule library.")
    private EvaluationMeasure evaluationMeasure;

    @Option(name = "-d", aliases = {"--dist","--distribution"},
            usage = "If specified, the distribution of the masses is shown as a histogram. " +
                    "Note: the histogram and the score don't have to correlate.")
    private boolean showDist;
    @Option(name = "-eq", aliases = "--equidistantBins",
            usage = "If specified, equidistant bins are computed for the histogram. " +
                    "Otherwise, the bin size increases linear corresponding to the mass accuracy and mass.")
    private boolean equidistantBins;
    @Option(name = "-bs", aliases = "--binSize", usage = "The bin size for equidistant bins. Default is 0.01.")
    private double binSize;

    @Option(name = "-opt", aliases = "--optimize",
            usage = "If specified, the optimal combinatorial molecule library regarding the specified score will be computed.\n " +
                    "This can be very time consuming and it is not recommended to use this option.")
    private boolean optimize;
    @Option(name = "--minBBSet", depends = "-opt",
            usage = "With this command the building blocks, which have to be used for constructing the library, can be specified.\n " +
            "You have to enter a matrix of building block indices (starting from 0) in increasing order. " +
                    "E.g.: \"[[0,1,2,3],[0,3,10,15],[1,8,9,12]]\"")
    private String minBBSetIndicesMatrixString;
    @Option(name = "--outputFile", depends = "-opt",
            usage = "Determine the path of the new .tsv file containing the optimal subset of building blocks.")
    private String optBBSubsetFile;


    private enum EvaluationMeasure{
        ENTROPY, BIN_ENTROPY, MDB_ENTROPY, AC
    }


    public static void main(String[] args){
        CLI cli = new CLI();
        CmdLineParser parser = new CmdLineParser(cli);
        try{
            parser.parseArgument(args);

            // INITIALISATION:
            File bbFile = new File(cli.bbFilePath);
            double[] massLosses = parseArray(cli.massLossesString);
            double ppm = cli.ppm;
            double blowupFactor = cli.blowupFactor > 0 ? cli.blowupFactor : 1e5;

            // Read the building block masses from the given 'bbFile' and convert these into integer masses:
            BuildingBlockReader bbReader = new BuildingBlockReader(bbFile, massLosses);
            bbReader.readFile();
            double[][] bbMasses = bbReader.getBbMasses();
            int[][] intBBMasses = CMLUtils.convertBBMassesToInteger(bbMasses, blowupFactor);

            // Initialise the CMLEvaluator which enables the evaluation of the given library:
            CMLEvaluator cmlEvaluator;
            double binSize = cli.binSize > 0 ? cli.binSize : 0.01;
            int intBinSize = (int) (blowupFactor * binSize);
            if(cli.evaluationMeasure == null){
                System.out.println("No measure specified. ENTROPY will be used for the CML evaluation.");
                cmlEvaluator = new EntropyLikeCalculator(ppm, blowupFactor);
            }else {
                switch (cli.evaluationMeasure) {
                    case BIN_ENTROPY:
                        System.out.println("\"BIN_ENTROPY\" will be used for the CML evaluation.");
                        cmlEvaluator = new EntropyCalculator(new EquidistantBinDistribution(intBBMasses, intBinSize));
                        break;
                    case MDB_ENTROPY:
                        System.out.println("MBD_ENTROPY will be used for the CML evaluation.");
                        cmlEvaluator = new EntropyCalculator(new MassDeviationDependentBinDistribution(intBBMasses, ppm));
                        break;
                    case AC:
                        System.out.println("AC will be used for the CML evaluation.");
                        cmlEvaluator = new NumCandidatesEvaluator(ppm);
                        break;
                    default:
                        System.out.println("ENTROPY will be used for the CML evaluation.");
                        cmlEvaluator = new EntropyLikeCalculator(ppm, blowupFactor);
                        break;
                }
            }

            // Compute the score of the given library:
            double score = cmlEvaluator.evaluate(intBBMasses);
            System.out.println("The score of the given library is "+score+".");

            if(cli.showDist){
                CMLDistribution cmlDist;
                if(cli.equidistantBins){
                    cmlDist = new EquidistantBinDistribution(intBBMasses, intBinSize);
                }else{
                    cmlDist = new MassDeviationDependentBinDistribution(intBBMasses, ppm);
                }
                cmlDist.computeNumMoleculesPerBin();
                drawHistogram(cmlDist.getNumMoleculesPerBin(), cmlDist.getBinEdges());
            }

            if(cli.optimize){
                System.out.println("Initialise the optimizer:");
                int[][] minBBSetIndices = parseIntegerMatrix(cli.minBBSetIndicesMatrixString);
                GreedySearch greedySearch = new GreedySearch(intBBMasses, minBBSetIndices, cmlEvaluator);

                System.out.println("The optimizer is initialised. Now: Start computation. This may take some while. ...");
                greedySearch.computeOptimalBBs();
                System.out.println("The optimal subsets of building blocks were computed. " +
                        "The score of the new library is"+greedySearch.getOptimalScore()+".");

                // Compute the new bbSmiles-Matrix:
                BitSet[] optBBsBitSets = greedySearch.getOptimalBBBitSets();
                String[][] bbSmiles = bbReader.getBbSmiles();
                String[][] optBBSmiles = new String[optBBsBitSets.length][];
                for(int row = 0; row < optBBsBitSets.length; row++){
                    optBBSmiles[row] = new String[optBBsBitSets[row].cardinality()];
                    int k = 0;
                    for(int j = optBBsBitSets[row].nextSetBit(0); j >= 0; j = optBBsBitSets[row].nextSetBit(j+1)){
                        optBBSmiles[row][k] = bbSmiles[row][j];
                        k++;
                    }
                }

                System.out.println("The results will be written into the specified .tsv file.");
                File outputFile = new File(cli.optBBSubsetFile);
                BuildingBlockWriter bbWriter = new BuildingBlockWriter(outputFile, optBBSmiles);
                bbWriter.write2File();
                System.out.println("FINISH");
            }
        } catch (CmdLineException e) {
            System.out.println("The arguments were not set correctly.");
            e.printStackTrace();
            parser.printUsage(System.err);
        } catch (CDKException | IOException e) {
            System.out.println("A problem occurred reading the file containing the building blocks.");
            e.printStackTrace();
        }
    }

    private static void drawHistogram(int[] numMoleculesPerBin, int[] binEdges){
        // Create the histogram:
        SimpleHistogramDataset dataset = new SimpleHistogramDataset("Number of candidates");
        dataset.setAdjustForBinSize(false);

        SimpleHistogramBin bin = new SimpleHistogramBin(binEdges[0], binEdges[1], true, true);
        bin.setItemCount(numMoleculesPerBin[0]);
        dataset.addBin(bin);
        for(int binIdx = 1; binIdx < numMoleculesPerBin.length; binIdx++){
            bin = new SimpleHistogramBin(binEdges[binIdx], binEdges[binIdx+1], false, true);
            bin.setItemCount(numMoleculesPerBin[binIdx]);
            dataset.addBin(bin);
        }

        JFreeChart histogram = ChartFactory.createHistogram("Distribution of molecule mass", "mass",
                "frequency", dataset);

        // Create a frame containing the histogram:
        JFrame frame = new JFrame("Molecule mass distribution of the combinatorial molecule library");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ChartPanel chartPanel = new ChartPanel(histogram);
        frame.add(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }


    private static double[] parseArray(String arrayStr){
        Pattern pattern = Pattern.compile("[\\[\\]\\s]");
        String[] a = pattern.matcher(arrayStr).replaceAll("").split(",");
        double[] parsedArray = new double[a.length];
        for(int i = 0; i < a.length; i++) parsedArray[i] = Double.parseDouble(a[i]);
        return parsedArray;
    }

    private static int[][] parseIntegerMatrix(String matrixStr){
        matrixStr = matrixStr.replaceAll("^\\[|\\s|]$", "");
        matrixStr = matrixStr.replaceAll("],\\[", "];[");
        String[] rows = matrixStr.split(";");

        int[][] matrix = new int[rows.length][];
        for(int row = 0; row < rows.length; row++){
            double[] parsedArray = parseArray(rows[row]);
            matrix[row] = new int[parsedArray.length];
            for(int i = 0; i < parsedArray.length; i++){
                matrix[row][i] = (int) parsedArray[i];
            }
        }
        return matrix;
    }
}
