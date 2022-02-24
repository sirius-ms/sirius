package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.babelms.MsIO;
import gurobi.GRBException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;
import java.util.List;

public class ExpectationMaximizationHydrogenRearrangmentEstimator {

    private File spectraDir;
    private File fTreeDir;
    private String[] fileNames;
    private double parameter;

    public ExpectationMaximizationHydrogenRearrangmentEstimator(File spectraDir, File fTreeDir, double startParameter){
        if(spectraDir.isDirectory()){
            if(fTreeDir.isDirectory()){
                this.spectraDir = spectraDir;
                this.fTreeDir = fTreeDir;
                this.parameter = startParameter;
                EMFragmenterScoring.rearrangementProb = startParameter;

                String[] spectrumFileNames = spectraDir.list();
                this.fileNames = new String[spectrumFileNames.length];
                for(int i = 0; i < spectrumFileNames.length; i++){
                    this.fileNames[i] = spectrumFileNames[i].replaceFirst("\\.ms","");
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

    public void run(int maxNumIterations, double epsilon, File outputFile) throws IOException, InvalidSmilesException, UnknownElementException, GRBException {
        if(outputFile.isFile() && outputFile.canWrite()){
            // Create the BufferedWriter that writes each estimated parameter into 'outputFile'
            // and write the start parameter into this file.
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputFile));
            fileWriter.write(String.valueOf(this.parameter));
            fileWriter.newLine();

            /* Now, iterate over the training data until the maximal number of iterations 'maxNumIterations'
             * are reached or the new estimated parameter differs by 'epsilon' or less.
             * In each iterations, there are two major steps:
             * -E-step: compute the optimal subtree with the current parameter 'parameter' for the scoring
             *          and calculate for each peak assignment the number of hydrogen rearrangements
             * -M-step: use this data to recompute 'parameter' by using its Maximum-Likelihood estimator
             */
            int iterations = 0;
            while(iterations < maxNumIterations){
                // E-STEP:
                long sum = 0;
                long numberOfAssignments = 0;

                for(String fileName : this.fileNames){
                    MolecularGraph molecule = this.readMolecule(fileName+".ms");
                    FTree fTree = this.readFTree(fileName+".json");
                    EMFragmenterScoring scoring = new EMFragmenterScoring(molecule);

                    PCSTFragmentationTreeAnnotator subtreeCalc = new PCSTFragmentationTreeAnnotator(fTree, molecule, scoring);
                    subtreeCalc.initialize(node -> node.fragment.getFormula().getMass() > this.getMinimalMassInFTree(fTree));
                    subtreeCalc.computeSubtree();

                    List<Integer> hydrogenRearrangements = subtreeCalc.getListWithAmountOfHydrogenRearrangements();
                    for(int k : hydrogenRearrangements) sum = sum + k;
                    numberOfAssignments = numberOfAssignments + hydrogenRearrangements.size();
                }

                // M-STEP:
                double newParameter = (double) (sum / (sum + numberOfAssignments));
                EMFragmenterScoring.rearrangementProb = newParameter;

                fileWriter.write(String.valueOf(newParameter));
                fileWriter.newLine();

                // Termination criterion:
                if(Math.abs(this.parameter - newParameter) <= epsilon) break;

                this.parameter = newParameter;
                iterations++;
            }

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


}
