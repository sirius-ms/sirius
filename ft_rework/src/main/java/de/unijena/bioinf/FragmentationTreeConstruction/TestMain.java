package de.unijena.bioinf.FragmentationTreeConstruction;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FasterMultithreadedTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.sirius.Sirius;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class TestMain {

    public static void main(String[] args) {

        final Sirius sirius;


//        File folder = new File("/home/florian/Uni/projektmodul/small_masses/");
        File folder = new File("/home/go96bix/small_masses/");
        File[] listOfFiles = folder.listFiles();

        try {
            sirius = new Sirius("qtof");
            new GurobiSolver();
            System.out.println(sirius.getMs2Analyzer().getTreeBuilder().getClass().getSimpleName());
            StringBuilder stringBuilderCut = new StringBuilder();
            StringBuilder stringBuilderPrediction = new StringBuilder();
            StringBuilder stringBuilderMaxPrediction = new StringBuilder();
            StringBuilder stringBuilderResult = new StringBuilder();
            StringBuilder stringBuilderPosition = new StringBuilder();
            StringBuilder stringBuilderSubsearch1 = new StringBuilder();
            StringBuilder stringBuilderSubsearch2 = new StringBuilder();
            StringBuilder stringBuilderSubsearch3 = new StringBuilder();
            StringBuilder stringBuilderSubsearch4 = new StringBuilder();
            StringBuilder stringBuilderSubsearch5 = new StringBuilder();
            StringBuilder stringBuilderSubsearch6 = new StringBuilder();
            StringBuilder stringBuilderSubsearch7 = new StringBuilder();
            StringBuilder stringBuilderSubsearch8 = new StringBuilder();
            StringBuilder stringBuilderLength = new StringBuilder();
            StringBuilder stringBuilderHeuSpeed = new StringBuilder();
            StringBuilder stringBuilderTotalSpeed = new StringBuilder();
            StringBuilder stringBuilderName = new StringBuilder();

            for (File file : listOfFiles) {
                long starttime = System.currentTimeMillis();
                if (file.isFile()) {
                    final FasterMultithreadedTreeComputation fmtc = new FasterMultithreadedTreeComputation(sirius.getMs2Analyzer());
                    System.out.println(file.getName());
                    final Ms2Experiment experiment = sirius.parseExperiment(new File(file.getPath())).next();

                    final ProcessedInput input = sirius.getMs2Analyzer().preprocessing(experiment);
                    if (input.getExperimentInformation().getIonMass() >=  800){
                        System.out.println("skipped");
                        continue;
                    }
                    fmtc.setInput(input);
                    FasterMultithreadedTreeComputation.Output out = fmtc.startComputation();

                    long endtime = System.currentTimeMillis();
                    System.out.println(endtime-starttime);
                    FileWriter writer = new FileWriter("outputFinal.csv");

                    stringBuilderCut.append(out.cut+",");
                    stringBuilderPrediction.append(out.prediction+",");
                    stringBuilderMaxPrediction.append(out.maxPrediction+",");
                    stringBuilderResult.append(out.result+",");
                    stringBuilderPosition.append(out.index+",");
                    stringBuilderSubsearch1.append(out.subsearch1+",");
                    stringBuilderSubsearch2.append(out.subsearch2+",");
                    stringBuilderSubsearch3.append(out.subsearch3+",");
                    stringBuilderSubsearch4.append(out.subsearch3+",");
                    stringBuilderSubsearch5.append(out.subsearch3+",");
                    stringBuilderSubsearch6.append(out.subsearch3+",");
                    stringBuilderSubsearch7.append(out.subsearch3+",");
                    stringBuilderSubsearch8.append(out.subsearch3+",");
                    stringBuilderLength.append(out.length+",");
                    stringBuilderHeuSpeed.append((out.heuEndTime-starttime)+",");
                    stringBuilderTotalSpeed.append((endtime-starttime)+",");
                    stringBuilderName.append(file.getName()+",");

                    writer.write(stringBuilderCut.toString()+"\n");
                    writer.write(stringBuilderPrediction.toString()+"\n");
                    writer.write(stringBuilderMaxPrediction.toString()+"\n");
                    writer.write(stringBuilderResult.toString()+"\n");
                    writer.write(stringBuilderPosition.toString()+"\n");
                    writer.write(stringBuilderSubsearch1.toString()+"\n");
                    writer.write(stringBuilderSubsearch2.toString()+"\n");
                    writer.write(stringBuilderSubsearch3.toString()+"\n");
                    writer.write(stringBuilderSubsearch4.toString()+"\n");
                    writer.write(stringBuilderSubsearch5.toString()+"\n");
                    writer.write(stringBuilderSubsearch6.toString()+"\n");
                    writer.write(stringBuilderSubsearch7.toString()+"\n");
                    writer.write(stringBuilderSubsearch8.toString()+"\n");
                    writer.write(stringBuilderLength.toString()+"\n");
                    writer.write(stringBuilderHeuSpeed.toString()+"\n");
                    writer.write(stringBuilderTotalSpeed.toString()+"\n");
                    writer.write(stringBuilderName.toString());
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