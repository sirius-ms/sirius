package combinatorial_molecule_library_design;

import org.kohsuke.args4j.Option;

import java.util.regex.Pattern;

public class CLI {

    @Option(name = "-bbs", aliases = {"--bbFile", "--buildingBlocks"}, required = true, usage = "")
    private String bbFile;
    @Option(name = "-l", aliases = {"--losses", "--massLosses"}, required = true, usage = "")
    private String massLossesString;
    @Option(name = "-ppm", required = true)
    private double ppm;
    @Option(name = "-b", aliases = "--blowupFactor", required = false, usage = "")
    private double blowupFactor;

    @Option(name = "-m", aliases = {"--cmlEvaluationMeasure", "--measure"}, usage = "")
    private String cmlEvaluationMeasure;
    @Option(name = "-d", aliases = {"--dist","--distribution"}, usage = "")
    private boolean showDist;
    @Option(name = "-eq", aliases = "--equidistantBins", usage = "", depends = {"-d"})
    private boolean equidistantBins;

    @Option(name = "-opt", aliases = "--optimize", usage = "")
    private boolean optimize;
    @Option(name = "--minBBSet", depends = "-opt")
    private String minBBSetIndicesString;


    public static void main(String[] args){

    }

    private static void drawHistogram(int[] numMoleculesPerBin, int[] binEdges){
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
