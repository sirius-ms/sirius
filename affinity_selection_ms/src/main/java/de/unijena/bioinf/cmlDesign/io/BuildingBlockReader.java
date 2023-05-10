package de.unijena.bioinf.cmlDesign.io;

import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;

public class BuildingBlockReader {

    private File bbFile;
    private String[][] bbSmiles;
    private double[][] bbMasses;
    private double[] massLosses;

    public BuildingBlockReader(File bbFile, double[] massLosses){
        this.bbFile = bbFile;
        this.massLosses = massLosses;
        this.bbSmiles = new String[this.massLosses.length][];
        this.bbMasses = new double[this.massLosses.length][];
    }

    public void readFile() throws IOException, CDKException {
        try(BufferedReader fileReader = Files.newBufferedReader(this.bbFile.toPath())){
            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            fileReader.readLine(); // first line contains only the column names
            String currentLine = fileReader.readLine();

            int currentBBIdx = -1;
            ArrayList<String> smilesList = null;
            ArrayList<Double> massesList = null;
            while(currentLine != null && currentLine.length() > 0){
                String[] row = currentLine.split("\t"); // row = [bbIdx, id, SMILES]
                int newBBIdx = Integer.parseInt(row[0]);
                String smiles = row[2];

                if(newBBIdx != currentBBIdx){
                    if(newBBIdx > 0){ // in this case: there are already building blocks which were read!
                        this.sortAndStoreMassesAndSmiles(smilesList, massesList, currentBBIdx);
                    }
                    smilesList = new ArrayList<>();
                    massesList = new ArrayList<>();
                    currentBBIdx = newBBIdx;
                }
                smilesList.add(smiles);
                massesList.add(this.smilesToMass(smiParser, smiles, currentBBIdx));
                currentLine = fileReader.readLine();
            }
            this.sortAndStoreMassesAndSmiles(smilesList, massesList, this.massLosses.length-1);
        }
    }

    private void sortAndStoreMassesAndSmiles(ArrayList<String> smilesList, ArrayList<Double> massesList, int bbIdx){
        // Sort smilesList and massesList according to the increasing order of massesList:
        ArrayList<Integer> indices = new ArrayList<>(massesList.size());
        for(int idx = 0; idx < massesList.size(); idx++) indices.add(idx);
        indices.sort(new IndicesComparator(massesList));

        this.bbSmiles[bbIdx] = new String[indices.size()];
        this.bbMasses[bbIdx] = new double[indices.size()];
        for(int idx = 0; idx < indices.size(); idx++){
            int sortedIdx = indices.get(idx);
            this.bbSmiles[bbIdx][idx] = smilesList.get(sortedIdx);
            this.bbMasses[bbIdx][idx] = massesList.get(sortedIdx);
        }
    }

    private double smilesToMass(SmilesParser smiParser, String smiles, int bbIdx) throws CDKException{
        // 1.) Parse SMILES string and configure aromaticity and other properties:
        IAtomContainer bbMol = smiParser.parseSmiles(smiles);
        AtomContainerManipulator.percieveAtomTypesAndConfigureUnsetProperties(bbMol);
        Aromaticity.cdkLegacy().apply(bbMol);

        double bbMolMass = AtomContainerManipulator.getMass(bbMol, AtomContainerManipulator.MonoIsotopic);
        double loss = this.massLosses[bbIdx];
        return bbMolMass - loss;
    }

    public double[][] getBbMasses(){
        return this.bbMasses;
    }

    public String[][] getBbSmiles(){
        return this.bbSmiles;
    }

    private class IndicesComparator implements Comparator<Integer> {

        private ArrayList<Double> massesList;

        public IndicesComparator(ArrayList<Double> massesList){
            this.massesList = massesList;
        }

        @Override
        public int compare(Integer idx1, Integer idx2) {
            return (int) Math.signum(this.massesList.get(idx1) - this.massesList.get(idx2));
        }
    }
}
