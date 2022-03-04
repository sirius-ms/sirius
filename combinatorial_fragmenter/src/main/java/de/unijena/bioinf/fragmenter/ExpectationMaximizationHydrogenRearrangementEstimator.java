package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.babelms.MsIO;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class ExpectationMaximizationHydrogenRearrangementEstimator {

    private File spectraDir;
    private File fTreeDir;
    private String[] fileNames;
    private double parameter;

    public ExpectationMaximizationHydrogenRearrangementEstimator(File spectraDir, File fTreeDir, int numberOfInstances, double startParameter){
        if(spectraDir.isDirectory()){
            if(fTreeDir.isDirectory()){
                this.spectraDir = spectraDir;
                this.fTreeDir = fTreeDir;
                this.parameter = startParameter;
                EMFragmenterScoring.rearrangementProb = startParameter;

                List<String> spectrumFileNames = Arrays.asList(spectraDir.list());
                Collections.shuffle(spectrumFileNames);

                this.fileNames = new String[numberOfInstances];
                for(int i = 0; i < numberOfInstances; i++){
                    this.fileNames[i] = spectrumFileNames.get(i).replaceFirst("\\.ms", "");
                }
            }else{
                throw new RuntimeException("The given File object for the fragmentation tree directory does not exist or is not a directory.");
            }
        }else{
           throw new RuntimeException("The given File object for the spectra directory does not exist or is not a directory.");
        }
    }

    private MolecularGraph readMolecule(String fileName) throws IOException, InvalidSmilesException, UnknownElementException {
        File file = new File(this.spectraDir, fileName);
        BufferedReader fileReader = new BufferedReader(new FileReader(file));

        String currentLine = fileReader.readLine();
        String molecularFormula = null, smiles = null;
        boolean mfWasRead = false;
        boolean smilesWasRead = false;

        while(currentLine != null){
            if(currentLine.startsWith(">formula")){
                molecularFormula = currentLine.split(" ")[1];
                mfWasRead = true;
            }else if(currentLine.startsWith(">smiles")){
                smiles = currentLine.split(" ")[1];
                smilesWasRead = true;
            }
            if(mfWasRead && smilesWasRead) break;
            currentLine = fileReader.readLine();
        }
        fileReader.close();
        // Assumption: In any case, 'file' contains two lines that start with ">formula" and ">smiles".
        // --> the molecular formula and the smiles string have been read
        SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        return new MolecularGraph(MolecularFormula.parse(molecularFormula), smilesParser.parseSmiles(smiles));
    }

    private FTree readFTree(String fileName) throws IOException {
        File file = new File(this.fTreeDir, fileName);
        FTree fTree = MsIO.readTreeFromFile(file);
        return fTree;
    }

    public void run(int fragmentationDepth, int maxNumIterations, double epsilon, File outputFile) throws IOException, UnknownElementException, InvalidSmilesException {
        if(outputFile.isFile() && outputFile.canWrite()){
            // Create the BufferedWriter that writes each estimated parameter into 'outputFile'
            // and write the start parameter into this file.
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputFile));
            fileWriter.write("Sum_H-Rearrangements #Assignments Est.Prob.");
            fileWriter.newLine();
            fileWriter.write("NaN NaN "+this.parameter);
            fileWriter.newLine();

            /* Now, iterate over the training data until the maximal number of iterations 'maxNumIterations'
             * are reached or the new estimated parameter differs by 'epsilon' or less.
             * In each iteration, there are two major steps:
             * -E-step: compute the optimal subtree with the current parameter 'parameter' for the scoring
             *          and calculate for each peak assignment the number of hydrogen rearrangements
             * -M-step: use this data to recompute 'parameter' by using its Maximum-Likelihood estimator
             *
             * The E-STEP is done by using MULTIPLE threading!
             */
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            ExecutorCompletionService<List<Integer>> completionService = new ExecutorCompletionService<>(executor);

            int iterations = 0;
            while(iterations < maxNumIterations){
                /* E-STEP:
                 * For each instance molecule, a thread will be created which computes the optimal subtree
                 * with the current estimated probability and then returns a list of integer values
                 * which represent the number of hydrogen rearrangements per peak assignment.
                 */
                for(String fileName : this.fileNames){
                    MolecularGraph molecule = this.readMolecule(fileName+".ms");
                    FTree fTree = this.readFTree(fileName+".json");
                    EMFragmenterScoring scoring = new EMFragmenterScoring(molecule);

                    completionService.submit(() -> {
                       PCSTFragmentationTreeAnnotator subtreeCalc = new PCSTFragmentationTreeAnnotator(fTree, molecule, scoring);
                       subtreeCalc.initialize(node -> node.depth < fragmentationDepth);
                       subtreeCalc.computeSubtree();
                       return subtreeCalc.getListWithAmountOfHydrogenRearrangements();
                    });
                }
                /* For each instance, a task was submitted.
                 * Everytime a task finished computing, we take its results.
                 * We use this result for computing the sum of hydrogen rearrangements and the total number of assignments.
                 */
                long sum = 0;
                long numberOfAssignments = 0;
                for(int i = 0; i < this.fileNames.length; i++){
                    try {
                        List<Integer> result = completionService.take().get();
                        numberOfAssignments = numberOfAssignments + result.size();
                        for(int k : result) sum = sum + k;
                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("An error occurred while executing the task or the waiting thread was interrupted.");
                        e.printStackTrace();
                    }
                }

                // M-STEP:
                // Estimate the new probability parameter by using the MLE of the geometric distribution.
                double newParameter = ((double) sum / (sum + numberOfAssignments));
                EMFragmenterScoring.rearrangementProb = newParameter;

                fileWriter.write(sum+" "+numberOfAssignments+" "+newParameter);
                fileWriter.newLine();
                fileWriter.flush();

                // Termination criterion:
                if(Math.abs(this.parameter - newParameter) <= epsilon) break;

                this.parameter = newParameter;
                iterations++;
            }
            fileWriter.close();
            executor.shutdown();
        }else{
            throw new IOException("The given File 'outputFile' does not exist, is not a file or cannot be written.");
        }
    }

    private double getMinimalMassInFTree(FTree fTree){
        double minMass = Double.POSITIVE_INFINITY;
        for(Fragment frag : fTree){
            double mass = frag.getFormula().getMass();
            if(mass < minMass){
                minMass = mass;
            }
        }
        return minMass;
    }

    public static void main(String[] args){
        try{
            File spectraDir = new File(args[0]);
            File fTreeDir = new File(args[1]);
            File outputFile = new File(args[2]);
            int numberOfInstances = Integer.parseInt(args[3]);
            double startParameter = Double.parseDouble(args[4]);
            int fragmentationDepth = Integer.parseInt(args[5]);
            int maxNumOfIterations = Integer.parseInt(args[6]);
            double epsilon = Double.parseDouble(args[7]);

            ExpectationMaximizationHydrogenRearrangementEstimator em = new ExpectationMaximizationHydrogenRearrangementEstimator(spectraDir, fTreeDir, numberOfInstances, startParameter);
            em.run(fragmentationDepth, maxNumOfIterations, epsilon, outputFile);

        } catch (UnknownElementException e) {
            System.out.println("A molecular formula was not possible to parse.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("An IOException occurred.");
            e.printStackTrace();
        } catch (InvalidSmilesException e) {
            System.out.println("A smiles string was not possible to parse.");
            e.printStackTrace();
        }
    }


}
