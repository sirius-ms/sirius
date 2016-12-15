package de.unijena.bioinf.FragmentationTreeConstruction;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FasterMultithreadedTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.sirius.Sirius;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class TestMain {

    public static void main(String[] args) {

        final Sirius sirius;


        File folder = new File("/home/florian/Uni/projektmodul/gnps/");
        File[] listOfFiles = folder.listFiles();
        int[] output = new int[100];

        try {
            sirius = new Sirius("qtof");
            new GurobiSolver();
            System.out.println(sirius.getMs2Analyzer().getTreeBuilder().getClass().getSimpleName());

            final FasterMultithreadedTreeComputation fmtc = new FasterMultithreadedTreeComputation(sirius.getMs2Analyzer());
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    System.out.println(file.getName());
                    final Ms2Experiment experiment = sirius.parseExperiment(new File(file.getPath())).next();

                    final ProcessedInput input = sirius.getMs2Analyzer().preprocessing(experiment);

                    fmtc.setInput(input);

                    float quality = (fmtc.startComputation())*100;
                    output[(int)quality-1] +=1;
                }
                FileWriter writer = new FileWriter("output.csv");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(Integer.toString(output[0]));

                for (int i = 1; i < 100; i++){
                    stringBuilder.append(","+Integer.toString(output[i]));
                }
//                System.out.println(stringBuilder);
                writer.write(stringBuilder.toString());
                writer.close();
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

}
