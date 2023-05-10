import de.unijena.bioinf.cmlDesign.CMLUtils;
import de.unijena.bioinf.cmlDesign.EntropyLikeCalculator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;


public class TEST {


    public static double[][] readBBMasses(File bbMassesFile) throws IOException {
        try(BufferedReader fileReader = Files.newBufferedReader(bbMassesFile.toPath())){
            ArrayList<ArrayList<Double>> bbMassesList = new ArrayList<>();
            fileReader.readLine(); // first row contains only the column names

            String currentLine = fileReader.readLine();
            while(currentLine != null && currentLine.length() > 0){
                String[] row = currentLine.split("\t");
                if(Integer.parseInt(row[0]) == 1){
                    ArrayList<Double> newBBMasses = new ArrayList<>();
                    bbMassesList.add(newBBMasses);
                }
                double bb_mass = Double.parseDouble(row[2]);
                bbMassesList.get(bbMassesList.size()-1).add(bb_mass);
                currentLine = fileReader.readLine();
            }

            double[][] bbMasses = new double[bbMassesList.size()][];
            for(int idx = 0; idx < bbMassesList.size(); idx++){
                bbMasses[idx] = doubleArrayListToDoubleArray(bbMassesList.get(idx));
            }
            return bbMasses;
        }
    }

    public static double[] doubleArrayListToDoubleArray(ArrayList<Double> list){
        double[] doubleArray = new double[list.size()];
        for(int idx = 0; idx < doubleArray.length; idx++){
            doubleArray[idx] = list.get(idx);
        }
        return doubleArray;
    }

    public static void writeBinEdgesAndNumMolsPerBin(File outputFile, int[] binEdges, int[] numMolsPerBin) throws IOException {
        try(BufferedWriter fileWriter = Files.newBufferedWriter(outputFile.toPath())){
            fileWriter.write("lowerBound\tupperBound\t#mols");
            fileWriter.newLine();

            for(int binIdx = 0; binIdx < numMolsPerBin.length; binIdx++){
                fileWriter.write(binEdges[binIdx]+"\t"+binEdges[binIdx+1]+"\t"+numMolsPerBin[binIdx]);
                fileWriter.newLine();
            }
        }
    }

    public static void main(String[] args){
        double[][] bbMasses = new double[][]{{1d},{1d},{1d}};
        double blowupFactor = 1e6;
        double ppm = 5;
        int[][] intBBMasses = CMLUtils.convertBBMassesToInteger(bbMasses, blowupFactor);

        EntropyLikeCalculator calc = new EntropyLikeCalculator(ppm, blowupFactor);
        System.out.println(calc.evaluate(intBBMasses));
    }
}
