package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

public class IterateThroughMolecule {

    public static IAtomContainer readMolecule(File spectrumFile) throws IOException, CDKException {
        try(BufferedReader fileReader = new BufferedReader(new FileReader(spectrumFile))) {
            String currentLine = fileReader.readLine();

            while (currentLine != null) {
                if (currentLine.startsWith(">smiles")) {
                    break;
                } else {
                    currentLine = fileReader.readLine();
                }
            }

            if (currentLine != null) {
                String smiles = currentLine.split(" ")[1];
                SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
                IAtomContainer molecule = parser.parseSmiles(smiles);
                AtomContainerManipulator.percieveAtomTypesAndConfigureUnsetProperties(molecule);
                Aromaticity.cdkLegacy().apply(molecule);
                return molecule;
            } else {
                return null;
            }
        }
    }
    public static String explainBondBy(IBond b, boolean fromLeftToRight) {
        String labelA = b.getAtom(0).getAtomTypeName();
        String labelB = b.getAtom(1).getAtomTypeName();

        if (!fromLeftToRight) {
            String c = labelA;
            labelA = labelB;
            labelB = c;
        }
        String s = labelA;
        if (b.isAromatic()) {
            s += ":";
        } else {
            switch (b.getOrder()) {
                case SINGLE:
                    s += "-";
                    break;
                case DOUBLE:
                    s += "=";
                    break;
                case TRIPLE:
                    s += "#";
                    break;
                default:
                    s += "?";
            }
        }
        s += labelB;
        return s;
    }

    public static void unreference(IAtomContainer molecule, File spectrumFile){
        molecule = null;
        spectrumFile = null;
    }

    public static void writeBondNamesIntoFile(File outputFile, ArrayList<String> bondNames) throws IOException{
        try(BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputFile))){
            for(String bondName : bondNames){
                fileWriter.write(bondName);
                fileWriter.newLine();
            }
        }
    }


    public static void main(String[] args){
        File spectraDir = new File(args[0]);
        File outputFile = new File(args[1]);
        String[] spectraFiles = spectraDir.list();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ArrayList<Callable<ArrayList<String>>> tasks = new ArrayList<>(spectraFiles.length);

        for(String fileName : spectraFiles){
            Callable<ArrayList<String>> task = () -> {
                File spectrumFile = new File(spectraDir, fileName);
                IAtomContainer molecule = readMolecule(spectrumFile);
                ArrayList<String> bondNames = new ArrayList<>();

                for(IBond bond : molecule.bonds()){
                    String bondName1 = explainBondBy(bond, true);
                    String bondName2 = explainBondBy(bond, false);

                    if(!bondNames.contains(bondName1)) bondNames.add(bondName1);
                    if(!bondNames.contains(bondName2)) bondNames.add(bondName2);
                }
                unreference(molecule, spectrumFile);
                return bondNames;
            };
            tasks.add(task);
        }

        try {
            Collection<Future<ArrayList<String>>> futures = executor.invokeAll(tasks);
            System.out.println("All tasks have been completed!");

            ArrayList<String> allBondNames = new ArrayList<>();
            for(Future<ArrayList<String>> future : futures){
                ArrayList<String> bondNames = future.get();

                for(String bondName : bondNames){
                    if(!allBondNames.contains(bondName)){
                        allBondNames.add(bondName);
                    }
                }
            }

            writeBondNamesIntoFile(outputFile, allBondNames);
            executor.shutdown();
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        }
    }
}
