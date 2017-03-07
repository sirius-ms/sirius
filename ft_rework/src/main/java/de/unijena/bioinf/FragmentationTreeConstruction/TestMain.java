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

        try {
            sirius = new Sirius("qtof");
            new GurobiSolver();
            System.out.println(sirius.getMs2Analyzer().getTreeBuilder().getClass().getSimpleName());
            StringBuilder stringBuilderCut = new StringBuilder();
            StringBuilder stringBuilderPrediction = new StringBuilder();
            StringBuilder stringBuilderResult = new StringBuilder();
            StringBuilder stringBuilderPosition = new StringBuilder();

            for (File file : listOfFiles) {
                long starttime = System.currentTimeMillis();
                if (file.isFile()) {
//                    System.out.println("Hey Ho");
                    final FasterMultithreadedTreeComputation fmtc = new FasterMultithreadedTreeComputation(sirius.getMs2Analyzer());
                    System.out.println(file.getName());
                    final Ms2Experiment experiment = sirius.parseExperiment(new File(file.getPath())).next();

                    final ProcessedInput input = sirius.getMs2Analyzer().preprocessing(experiment);
//                    if (input.getExperimentInformation().getIonMass() <  700){
//                        System.out.println("skipped");
//                        continue;
//                    }
                    if (input.getExperimentInformation().getIonMass() >=  800){
                        System.out.println("skipped");
                        continue;
                    }
                    fmtc.setInput(input);
//                    System.out.println("let's go!");
                    FasterMultithreadedTreeComputation.Output out = fmtc.startComputation();

                    long endtime = System.currentTimeMillis();
                    System.out.println(endtime-starttime);
                    FileWriter writer = new FileWriter("outputFinal.csv");

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