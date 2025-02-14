package de.unijena.bioinf.ms.biotransformer;

import biotransformer.biosystems.BioSystem;
import biotransformer.transformation.Biotransformation;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.util.ArrayList;

public class BiotransformationObject extends Biotransformation {
    private String inputMoleculeSmiles; // Input Molecule as SMILES
    private int iterations; // Number of iterations
    private int cyp450Mode; // CYP450 Mode
    private int p2Mode; // Phase II Mode
    private boolean useDB; // Use the database flag
    private boolean useSub; // Use the substructure flag

    // Constructor
    public BiotransformationObject(IAtomContainerSet substrates, String reactionType, ArrayList<String> enzymeNames,
                                   IAtomContainerSet products, BioSystem.BioSystemName bsysName, Double score,
                                   String inputMoleculeSmiles, int iterations, int cyp450Mode, int p2Mode,
                                   boolean useDB, boolean useSub) {
        super(substrates, reactionType, enzymeNames, products, score, bsysName);
        this.inputMoleculeSmiles = inputMoleculeSmiles;
        this.iterations = iterations;
        this.cyp450Mode = cyp450Mode;
        this.p2Mode = p2Mode;
        this.useDB = useDB;
        this.useSub = useSub;
    }

    public BiotransformationObject(IAtomContainerSet substrates, String reactionType, ArrayList<String> enzymeNames,
                                   IAtomContainerSet products, BioSystem.BioSystemName bsysName, Double score,
                                   String inputMoleculeSmiles, int iterations,
                                   boolean useDB, boolean useSub) {
        super(substrates, reactionType, enzymeNames, products, score, bsysName);
        this.inputMoleculeSmiles = inputMoleculeSmiles;
        this.iterations = iterations;
        this.useDB = useDB;
        this.useSub = useSub;
    }

    public BiotransformationObject(IAtomContainerSet substrates, String reactionType, ArrayList<String> enzymeNames,
                                   IAtomContainerSet products, BioSystem.BioSystemName bsysName, Double score,
                                   String inputMoleculeSmiles, int iterations, int cyp450Mode,
                                   boolean useDB, boolean useSub) {
        super(substrates, reactionType, enzymeNames, products, score, bsysName);
        this.inputMoleculeSmiles = inputMoleculeSmiles;
        this.iterations = iterations;
        this.cyp450Mode = cyp450Mode;
        this.useDB = useDB;
        this.useSub = useSub;
    }


    // Getter and Setter for inputMoleculeSmiles
    public String getInputMoleculeSmiles() {
        return inputMoleculeSmiles;
    }

    public void setInputMoleculeSmiles(String inputMoleculeSmiles) {
        this.inputMoleculeSmiles = inputMoleculeSmiles;
    }

    // Getter and Setter for iterations
    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    // Getter and Setter for cyp450Mode
    public int getCyp450Mode() {
        return cyp450Mode;
    }

    public void setCyp450Mode(int cyp450Mode) {
        this.cyp450Mode = cyp450Mode;
    }

    // Getter and Setter for p2Mode
    public int getP2Mode() {
        return p2Mode;
    }

    public void setP2Mode(int p2Mode) {
        this.p2Mode = p2Mode;
    }

    // Getter and Setter for useDB
    public boolean isUseDB() {
        return useDB;
    }

    public void setUseDB(boolean useDB) {
        this.useDB = useDB;
    }

    // Getter and Setter for useSub
    public boolean isUseSub() {
        return useSub;
    }

    public void setUseSub(boolean useSub) {
        this.useSub = useSub;
    }

    // Overriding display method to include fields of BiotransformationObject
    @Override
    public void display() {
        super.display();
        System.out.println("Input Molecule SMILES: " + inputMoleculeSmiles);
        System.out.println("Iterations: " + iterations);
        System.out.println("CYP450 Mode: " + cyp450Mode);
        System.out.println("Phase II Mode: " + p2Mode);
        System.out.println("Use DB: " + useDB);
        System.out.println("Use Substructure: " + useSub);
    }
}
