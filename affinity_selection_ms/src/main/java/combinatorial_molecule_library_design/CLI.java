package combinatorial_molecule_library_design;

import combinatorial_molecule_library_design.io.BuildingBlockReader;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openscience.cdk.exception.CDKException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class CLI {

    @Option(name = "-bbs", aliases = {"--bbFile", "--buildingBlocks"}, required = true, usage = "")
    private String bbFilePath;
    @Option(name = "-l", aliases = {"--losses", "--massLosses"}, required = true, usage = "")
    private String massLossesString;
    @Option(name = "-ppm", required = true)
    private double ppm;
    @Option(name = "-b", aliases = "--blowupFactor", required = false, usage = "")
    private double blowupFactor;

    @Option(name = "-m", aliases = {"--evaluationMeasure", "--measure"}, usage = "")
    private EvaluationMeasure evaluationMeasure;
    @Option(name = "-d", aliases = {"--dist","--distribution"}, usage = "")
    private boolean showDist;
    @Option(name = "-eq", aliases = "--equidistantBins", usage = "")
    private boolean equidistantBins;
    @Option(name = "-bs", aliases = "--binSize", usage = "")
    private double binSize;


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
            double binSize = cli.binSize > 0 ? cli.binSize : 0.0001;
            int intBinSize = (int) (blowupFactor * binSize);
            if(cli.evaluationMeasure == null){
                cmlEvaluator = new EntropyLikeCalculator(ppm, blowupFactor);
            }else {
                switch (cli.evaluationMeasure) {
                    case BIN_ENTROPY:
                        cmlEvaluator = new EntropyCalculator(new EquidistantBinDistribution(intBBMasses, intBinSize));
                        break;
                    case MDB_ENTROPY:
                        cmlEvaluator = new EntropyCalculator(new MassDeviationDependentBinDistribution(intBBMasses, ppm));
                        break;
                    case AC:
                        cmlEvaluator = new NumCandidatesEvaluator(ppm);
                        break;
                    default:
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

        } catch (CmdLineException e) {
            System.out.println("The arguments were not set correctly.");
            e.printStackTrace();
            parser.printUsage(System.err);
        } catch (CDKException e) {
            System.out.println("A problem occurred reading the file containing the building blocks.");
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void drawHistogram(int[] numMoleculesPerBin, int[] binEdges){
        // TODO
        System.out.println("This function is currently not supported.");
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
