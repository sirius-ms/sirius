package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.MsIO;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DataProcessor {

    private File spectraDir;
    private File fTreeDir;
    private File outputDir;
    private String[] fileNames;

    public DataProcessor(File spectraDir, File fTreeDir, File outputDir){
        if(spectraDir.isDirectory() && fTreeDir.isDirectory() && outputDir.isDirectory()){
            this.spectraDir = spectraDir;
            this.fTreeDir = fTreeDir;
            this.outputDir = outputDir;

            String[] spectrumFileNames = spectraDir.list();
            this.fileNames = new String[spectrumFileNames.length];
            for(int i = 0; i < spectrumFileNames.length; i++){
                this.fileNames[i] = spectrumFileNames[i].replaceFirst("\\.ms", "");
            }
        }else{
            throw new RuntimeException("The given abstract pathnames don't exist or aren't a directory.");
        }
    }

    private MolecularGraph readMolecule(String fileName) throws IOException, InvalidSmilesException, UnknownElementException {
        File file = new File(this.spectraDir, fileName);
        try(BufferedReader fileReader = new BufferedReader(new FileReader(file))) {

            String currentLine = fileReader.readLine();
            String molecularFormula = null, smiles = null;
            boolean mfWasRead = false;
            boolean smilesWasRead = false;

            while (currentLine != null) {
                if (currentLine.startsWith(">formula")) {
                    molecularFormula = currentLine.split(" ")[1];
                    mfWasRead = true;
                } else if (currentLine.startsWith(">smiles")) {
                    smiles = currentLine.split(" ")[1];
                    smilesWasRead = true;
                }
                if (mfWasRead && smilesWasRead) break;
                currentLine = fileReader.readLine();
            }
            // Assumption: In any case, 'file' contains two lines that start with ">formula" and ">smiles".
            // --> the molecular formula and the smiles string have been read
            SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            return new MolecularGraph(MolecularFormula.parse(molecularFormula), smilesParser.parseSmiles(smiles));
        }
    }

    private FTree readFTree(String fileName) throws IOException {
        File file = new File(this.fTreeDir, fileName);
        FTree fTree = MsIO.readTreeFromFile(file);
        return fTree;
    }

    public void run(CombinatorialFragmenter.Callback2 fragmentationConstraint) throws InterruptedException {
        // INITIALISATION:
        EMFragmenterScoring.rearrangementProb = 1.0; // don't punish hydrogen rearrangements at first
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // LOOP - ITERATE OVER ALL FILES:
        // For each spectrum file and its corresponding FTree file, a new task, for computing the best subtree
        // in the calculated in silico fragmentation graph and saving this result, will be submitted.
        System.out.println("Collect all tasks...");
        ArrayList<Callable<Object>> tasks = new ArrayList<>(this.fileNames.length);
        for(String fileName : this.fileNames){
            Callable<Object> task = Executors.callable(() -> {
                try {
                    System.out.println("Starting task: "+fileName);
                    MolecularGraph molecule = this.readMolecule(fileName + ".ms");
                    FTree fTree = this.readFTree(fileName + ".json");
                    EMFragmenterScoring scoring = new EMFragmenterScoring(molecule);

                    System.out.println("Task"+fileName+": Initialize and compute subtree.");
                    CriticalPathSubtreeCalculator subtreeCalc = new CriticalPathSubtreeCalculator(fTree, molecule, scoring, true);
                    subtreeCalc.initialize(fragmentationConstraint);
                    subtreeCalc.computeSubtree();

                    System.out.println("Task"+fileName+": Save results.");
                    CombinatorialSubtreeCalculatorJsonWriter.writeResultsToFile(subtreeCalc, new File(this.outputDir, fileName + ".json"));
                }catch (UnknownElementException | IOException | CDKException e) {
                    System.out.println("Task"+fileName+": AN ERROR OCCURED!");
                    e.printStackTrace();
                }
            });
            tasks.add(task);
        }
        // COMPUTING THE TASKS:
        // Wait, until all tasks terminated.
        System.out.println("Execute all tasks...");
        Collection<Future<Object>> futures = executor.invokeAll(tasks);

        System.out.println("All tasks have been processed and the executor service will be shutdown.");
        executor.shutdown();
    }

    public static void main(String[] args){
        try{
            File spectraDir = new File(args[0]);
            File fTreeDir = new File(args[1]);
            File outputDir = new File(args[2]);
            int fragmentationDepth = Integer.parseInt(args[3]);

            DataProcessor dataProcessor = new DataProcessor(spectraDir, fTreeDir, outputDir);
            dataProcessor.run(node -> node.depth < fragmentationDepth);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



}
