import combinatorial_molecule_library_design.EntropyCalculator;
import combinatorial_molecule_library_design.EquidistantBinDistribution;
import combinatorial_molecule_library_design.MassDecomposer;
import combinatorial_molecule_library_design.MassDeviationDependentBinDistribution;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

public class TEST {

    public static <T> String arrayToString(T[] array){
        StringBuilder strBuilder = new StringBuilder();
        for(T x : array) strBuilder.append(x).append(" ");
        return strBuilder.toString();
    }

    public static <T> void printArray(T[] array){
         System.out.println(arrayToString(array));
    }
    public static void main(String[] args){
        double[][] bbMasses = new double[][]{{87.0320284, 97.052763844, 113.084063972, 137.058911844, 147.068413908, 163.063328528, 186.07931294},
                {57.021463716, 71.03711378, 97.052763844, 99.068413908, 101.047678464, 128.058577496, 129.042593084},
                {97.065339908, 123.024617968, 154.00596646, 165.055169148, 167.045667084}};
        double blowupFactor = 1E5;
        double ppm = 5;
        long timeStamp = System.currentTimeMillis();
        MassDeviationDependentBinDistribution dist = new MassDeviationDependentBinDistribution(bbMasses, blowupFactor, ppm);
        int[][] intBBMasses = dist.getBbMasses();
        EntropyCalculator entropyCalc = new EntropyCalculator(dist, x -> x);
        System.out.println(entropyCalc.evaluate(new int[][]{{intBBMasses[0][0]}, {intBBMasses[1][0]}, {intBBMasses[2][0]}}));
        long timeNeeded = System.currentTimeMillis() - timeStamp;
        System.out.println("The computation needed "+timeNeeded+"ms");
    }
}
