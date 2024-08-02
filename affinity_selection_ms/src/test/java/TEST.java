import de.unijena.bioinf.cmlDesign.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class TEST {

    public static double[] readBBMassesOfOneSet(File file) throws IOException {
        try(BufferedReader fileReader = Files.newBufferedReader(file.toPath())){
            ArrayList<Double> masses = new ArrayList<>();
            String currentLine = fileReader.readLine();
            while(currentLine != null){
                currentLine = currentLine.replaceAll("\\s\\n", "");
                masses.add(Double.parseDouble(currentLine));
                currentLine = fileReader.readLine();
            }

            double[] massesArray = new double[masses.size()];
            for(int i = 0; i < masses.size(); i++) massesArray[i] = masses.get(i);
            return massesArray;
        }
    }

    public static void main(String[] args){
        try {
            File cwd = new File("C:\\Users\\Nutzer\\Documents\\Bioinformatik_PhD\\AS-MS-Project\\Ergebnisse\\Evaluation_Entropy_Like_Function");
            File file = new File(cwd, "normal_bb_masses.txt");
            double[] blowupFactors = new double[]{1e3,1e4,1e5,1e6};
            double ppm = 5d;

            double[] fstBBMasses = readBBMassesOfOneSet(file);
            double[][] bbMasses = new double[][]{fstBBMasses, {100d}};

            for(double blowupFactor : blowupFactors) {
                int[][] intBBMasses = CMLUtils.convertBBMassesToInteger(bbMasses, blowupFactor);

                EntropyLikeCalculator entropyCalc = new EntropyLikeCalculator(ppm, blowupFactor);
                System.out.println("Blowup-Factor: "+blowupFactor+"\tScore/Entropy: "+entropyCalc.evaluate(intBBMasses));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
