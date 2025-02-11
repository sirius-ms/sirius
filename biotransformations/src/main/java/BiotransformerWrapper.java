//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//



import biotransformer.biosystems.BioSystem.BioSystemName;
import biotransformer.btransformers.Biotransformer.bType;
import biotransformer.railsappspecific.AbioticTransformer_rails;
import biotransformer.railsappspecific.BiotransformerSequence_rails;
import biotransformer.railsappspecific.Cyp450BTransformer_rails;
import biotransformer.railsappspecific.ECBasedBTransformer_rails;
import biotransformer.railsappspecific.EnvMicroBTransformer_rails;
import biotransformer.railsappspecific.HGutBTransformer_rails;
import biotransformer.railsappspecific.PhaseIIBTransformer_rails;
import biotransformer.railsappspecific.SimulateHumanMetabolism_rails;
import biotransformer.transformation.Biotransformation;
import biotransformer.utils.BiotransformerSequence;
import biotransformer.utils.ChemStructureExplorer;
import biotransformer.utils.FileUtilities;

import executable.*;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.cli.Options;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

public class BiotransformerWrapper{

    public BiotransformerWrapper() {

    }
public static IAtomContainer SetMolecule(String smiles) throws Exception {
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        SmilesParser smiParser = new SmilesParser(builder);
        return smiParser.parseSmiles(smiles);

}

    // Helper method to map Biotransformation to BiotransformationObject
    private static ArrayList<BiotransformationObject> convertToBiotransformationObject(
            ArrayList<Biotransformation> transformations,
            String inputSmiles,
            int iterations,
            int cyp450Mode,
            int p2Mode,
            boolean useDB,
            boolean useSub
    ) {
        ArrayList<BiotransformationObject> results = new ArrayList<>();

        for (Biotransformation transformation : transformations) {
            BiotransformationObject object = new BiotransformationObject(
                    transformation.getSubstrates(),
                    transformation.getReactionType(),
                    transformation.getEnzymeNames(),
                    transformation.getProducts(),
                    transformation.getBioSystemName(),
                    transformation.getScore(),
                    inputSmiles,
                    iterations,
                    cyp450Mode,
                    p2Mode,
                    useDB,
                    useSub
            );
            results.add(object);
        }

        return results;
    }

    private static ArrayList<BiotransformationObject> convertToBiotransformationObject(
            ArrayList<Biotransformation> transformations,
            String inputSmiles,
            int iterations,
            boolean useDB,
            boolean useSub
    ) {
        ArrayList<BiotransformationObject> results = new ArrayList<>();

        for (Biotransformation transformation : transformations) {
            BiotransformationObject object = new BiotransformationObject(
                    transformation.getSubstrates(),
                    transformation.getReactionType(),
                    transformation.getEnzymeNames(),
                    transformation.getProducts(),
                    transformation.getBioSystemName(),
                    transformation.getScore(),
                    inputSmiles,
                    iterations,
                    useDB,
                    useSub
            );
            results.add(object);
        }

        return results;
    }

    private static ArrayList<BiotransformationObject> convertToBiotransformationObject(
            ArrayList<Biotransformation> transformations,
            String inputSmiles,
            int iterations,
            int p2Mode,
            boolean useDB,
            boolean useSub
    ) {
        ArrayList<BiotransformationObject> results = new ArrayList<>();

        for (Biotransformation transformation : transformations) {
            BiotransformationObject object = new BiotransformationObject(
                    transformation.getSubstrates(),
                    transformation.getReactionType(),
                    transformation.getEnzymeNames(),
                    transformation.getProducts(),
                    transformation.getBioSystemName(),
                    transformation.getScore(),
                    inputSmiles,
                    iterations,
                    p2Mode,
                    useDB,
                    useSub
            );
            results.add(object);
        }

        return results;
    }

public static ArrayList<BiotransformationObject> AbioticTransformer(String singleMoleculeSmiles,int nrOfSteps) throws Exception {
    IAtomContainer singlemolecule = SetMolecule(singleMoleculeSmiles);
    AbioticTransformer_rails abiotic_bt = new AbioticTransformer_rails(false, false);
    ArrayList<Biotransformation> biotransformations = abiotic_bt.applyAbioticTransformationsChain(singlemolecule, true, true, nrOfSteps, (double) 0.5F);
        return convertToBiotransformationObject(biotransformations,singleMoleculeSmiles,nrOfSteps,false,false);
}
public static ArrayList<Biotransformation> AbioticTransformer(){
    IAtomContainerSet multiplemolecules;
        return null;
}

public static ArrayList<BiotransformationObject> Cyp450BTransformer(String singleMoleculeSmiles,int nrOfSteps, int cyp450Mode, boolean useDB,boolean useSub) throws Exception {
    IAtomContainer singlemolecule = SetMolecule(singleMoleculeSmiles);
    Cyp450BTransformer_rails cyp450bt = new Cyp450BTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
    ArrayList<Biotransformation> biotransformations = cyp450bt.predictCyp450BiotransformationChainByMode(singlemolecule, true, true, nrOfSteps, (double) 0.5F, cyp450Mode);
        return convertToBiotransformationObject(biotransformations,singleMoleculeSmiles,nrOfSteps,cyp450Mode,useDB,useSub);
}
public static ArrayList<Biotransformation> Cyp450BTransformer(){
    IAtomContainerSet multiplemolecules;
        return null;
}
public static ArrayList<BiotransformationObject> ECBasedBTransformer(String singleMoleculeSmiles,int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
    IAtomContainer singlemolecule = SetMolecule(singleMoleculeSmiles);
    ECBasedBTransformer_rails ecbt = new ECBasedBTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
    ArrayList<Biotransformation> biotransformations = ecbt.simulateECBasedMetabolismChain(singlemolecule, true, true, nrOfSteps, (double) 0.5F);
        return convertToBiotransformationObject(biotransformations,singleMoleculeSmiles,nrOfSteps,false,false);
}
public static ArrayList<Biotransformation> ECBasedBTransformer(){
    IAtomContainerSet multiplemolecules;
        return null;
}
public static ArrayList<BiotransformationObject> HGutBTransformer(String singleMoleculeSmiles,int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
    IAtomContainer singlemolecule = SetMolecule(singleMoleculeSmiles);
    HGutBTransformer_rails hgut = new HGutBTransformer_rails(useDB, useSub);
    ArrayList<Biotransformation> biotransformations = hgut.simulateGutMicrobialMetabolism(singlemolecule, true, true, nrOfSteps, (double) 0.5F);
        return convertToBiotransformationObject(biotransformations,singleMoleculeSmiles,nrOfSteps,false,false);
}
public static ArrayList<Biotransformation> HGutBTransformer(){
    IAtomContainerSet multiplemolecules;
        return null;
}
public static ArrayList<BiotransformationObject> PhaseIIBTransformer(String singleMoleculeSmiles,int nrOfSteps, int p2Mode, boolean useDB, boolean useSub) throws Exception {
    IAtomContainer singlemolecule = SetMolecule(singleMoleculeSmiles);
        PhaseIIBTransformer_rails phase2b = new PhaseIIBTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
        ArrayList<Biotransformation> biotransformations = phase2b.applyPhase2TransformationsChainAndReturnBiotransformations(singlemolecule, true, true, true, nrOfSteps, (double) 0.5F, p2Mode);
        return convertToBiotransformationObject(biotransformations,singleMoleculeSmiles,nrOfSteps,p2Mode,false,false);
}
public static ArrayList<Biotransformation> PhaseIIBTransformer(){
    IAtomContainerSet multiplemolecules;
        return null;
}
public static ArrayList<Biotransformation> SuperBioTransformer(String singleMoleculeSmiles,int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
        return null;
} //TODO notwendig?? da SUPERBIO = allHUMAN mit iteration=4

public static ArrayList<BiotransformationObject> AllHumanTransformer(String singleMoleculeSmiles,int nrOfSteps,int p2Mode,int cyp450Mode, boolean useDB, boolean useSub) throws Exception {
    IAtomContainer singlemolecule = SetMolecule(singleMoleculeSmiles);
    SimulateHumanMetabolism_rails hsbt = new SimulateHumanMetabolism_rails(cyp450Mode, p2Mode, useDB, "hmdb", false, 30, useSub);
    ArrayList<Biotransformation> biotransformations = hsbt.simulateHumanMetabolism(singlemolecule, nrOfSteps);
    return convertToBiotransformationObject(biotransformations,singleMoleculeSmiles,nrOfSteps,p2Mode,cyp450Mode,useDB,useSub);

}
public static ArrayList<Biotransformation> AllHumanTransformer(){
    IAtomContainerSet multiplemolecules;
    return null;
}

public static ArrayList<BiotransformationObject> EnvMicrobialTransformer (String singleMoleculeSmiles, int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
    IAtomContainer singlemolecule = SetMolecule(singleMoleculeSmiles);
    EnvMicroBTransformer_rails ebt = new EnvMicroBTransformer_rails(useDB, useSub);
    ArrayList<Biotransformation> biotransformations = ebt.applyEnvMicrobialTransformationsChain(singlemolecule, true, true, nrOfSteps, (double) 0.5F);
    return convertToBiotransformationObject(biotransformations,singleMoleculeSmiles,nrOfSteps,useDB,useSub);
}
public static ArrayList<Biotransformation> EnvMicrobialTransformer(){
    IAtomContainerSet multiplemolecules;
    return null;
}

//TODO: biotransformersequence?? da nur aneinenandereihung der human BTs




   /* public static void main(String[] args) throws Exception {



        BiotransformerSequence_rails biotransformerSeqeuence = null;
        BiotransformerSequence btq_for_mf = null;
        // default
        int cyp450Mode = 1;
        int p2Mode = 1;
        boolean useDB = true;
        boolean useSub = false;
        Double massThreshold = (double)1500.0F;

        String smile = "CC(C)C1=CC=C(C)C=C1O";
        int steps= 2;
        int cypmode = 3;
        ArrayList<Biotransformation> biotransformations;
        //biotransformations = BiotransformerWrapper.AllHumanTransformer(smile, steps, p2Mode, cypmode, useDB, useSub);

        BiotransformerWrapper.AllHumanTransformer(smile, steps, p2Mode, cypmode, useDB, useSub);



    }*/
}
