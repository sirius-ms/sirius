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
//        int[] output = new int[100];

        try {
            sirius = new Sirius("qtof");
            new GurobiSolver();
            System.out.println(sirius.getMs2Analyzer().getTreeBuilder().getClass().getSimpleName());
            StringBuilder stringBuilderCut = new StringBuilder();
            StringBuilder stringBuilderPrediction = new StringBuilder();
            StringBuilder stringBuilderResult = new StringBuilder();
            StringBuilder stringBuilderPosition = new StringBuilder();

            for (File file : listOfFiles) {

                if (file.isFile()) {
                    final FasterMultithreadedTreeComputation fmtc = new FasterMultithreadedTreeComputation(sirius.getMs2Analyzer());
                    System.out.println(file.getName());
                    final Ms2Experiment experiment = sirius.parseExperiment(new File(file.getPath())).next();

                    final ProcessedInput input = sirius.getMs2Analyzer().preprocessing(experiment);

                    fmtc.setInput(input);
                    FasterMultithreadedTreeComputation.Output out = fmtc.startComputation();

//                    float quality = (fmtc.startComputation())*100;
//                    output[(int)quality-1] +=1;
                    FileWriter writer = new FileWriter("output3.csv");

                    stringBuilderCut.append(out.cut+",");
                    stringBuilderPrediction.append(out.prediction+",");
                    stringBuilderResult.append(out.result+",");
                    stringBuilderPosition.append(out.index+",");

                    writer.write(stringBuilderCut.toString()+"\n");
                    writer.write(stringBuilderPrediction.toString()+"\n");
                    writer.write(stringBuilderResult.toString()+"\n");
                    writer.write(stringBuilderPosition.toString());
                    writer.close();
                }
//                Runtime.getRuntime().gc();//TODO take out again
//
//                for (int i = 1; i < 100; i++){
//                    stringBuilder.append(","+Integer.toString(output[i]));
//                }
////                System.out.println(stringBuilder);
//                writer.write(stringBuilder.toString());
//                writer.close();
            }


        } catch (IOException e) {
            System.out.println("Warning");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Warning");
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.out.println("Warning");
            e.printStackTrace();
        }

    }

}
