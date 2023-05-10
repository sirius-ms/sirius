import de.unijena.bioinf.cmlDesign.MassDeviationDependentBinDistribution;

import java.io.*;

public class MassDecomposerTest {

    public static void main(String[] args){
        double[][] bbMasses = new double[][]{{87.0320284, 97.052763844, 113.084063972, 137.058911844, 147.068413908, 163.063328528, 186.07931294},
                {57.021463716, 71.03711378, 97.052763844, 99.068413908, 101.047678464, 128.058577496, 129.042593084},
                {97.065339908, 123.024617968, 154.00596646, 165.055169148, 167.045667084}};
        double blowupFactor = 1;
        double ppm = 1e4;

        MassDeviationDependentBinDistribution dist = new MassDeviationDependentBinDistribution(bbMasses, blowupFactor, ppm);
        dist.computeNumMoleculesPerBin();


        int[] numMoleculesPerBin = dist.getNumMoleculesPerBin();
        int[] binEdges = dist.getBinEdges();

        File outputFile = new File("C:\\Users\\Nutzer\\Desktop\\dist_test.csv");
        try(BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputFile))){
            fileWriter.write("lb,ub,nm");
            fileWriter.newLine();

            for(int binIdx = 0; binIdx < numMoleculesPerBin.length; binIdx++){
                fileWriter.write(binEdges[binIdx]+","+binEdges[binIdx+1]+","+numMoleculesPerBin[binIdx]);
                fileWriter.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
