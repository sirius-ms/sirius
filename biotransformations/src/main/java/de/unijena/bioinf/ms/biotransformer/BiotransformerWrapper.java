package de.unijena.bioinf.ms.biotransformer;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//



import biotransformer.biosystems.BioSystem.BioSystemName;
import biotransformer.railsappspecific.*;
import biotransformer.transformation.Biotransformation;

import java.util.ArrayList;
import java.util.List;

import biotransformer.utils.BiotransformerSequenceStep;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

public class BiotransformerWrapper{

    public BiotransformerWrapper() {

    }
public static IAtomContainer SetMolecule(String smiles) throws Exception {
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        SmilesParser smiParser = new SmilesParser(builder);
        return smiParser.parseSmiles(smiles);

}



public static List<Biotransformation> abioticTransformer(IAtomContainer singleMolecule,int nrOfSteps) throws Exception {
    AbioticTransformer_rails abiotic_bt = new AbioticTransformer_rails(false, false);
    return abiotic_bt.applyAbioticTransformationsChain(singleMolecule, true, true, nrOfSteps, (double) 0.5F);
}


public static List<Biotransformation> cyp450BTransformer(IAtomContainer singleMolecule,int nrOfSteps, int cyp450Mode, boolean useDB,boolean useSub) throws Exception {
    Cyp450BTransformer_rails cyp450bt = new Cyp450BTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
   return cyp450bt.predictCyp450BiotransformationChainByMode(singleMolecule, true, true, nrOfSteps, (double) 0.5F, cyp450Mode);

}

public static List<Biotransformation> ecBasedBTransformer(IAtomContainer singleMolecule,int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
    ECBasedBTransformer_rails ecbt = new ECBasedBTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
    return ecbt.simulateECBasedMetabolismChain(singleMolecule, true, true, nrOfSteps, (double) 0.5F);

}

public static List<Biotransformation> hGutBTransformer(IAtomContainer singleMolecule,int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
    HGutBTransformer_rails hgut = new HGutBTransformer_rails(useDB, useSub);
    return hgut.simulateGutMicrobialMetabolism(singleMolecule, true, true, nrOfSteps, (double) 0.5F);

}

public static List<Biotransformation> phaseIIBTransformer(IAtomContainer singleMolecule,int nrOfSteps, int p2Mode, boolean useDB, boolean useSub) throws Exception {
        PhaseIIBTransformer_rails phase2b = new PhaseIIBTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
        return phase2b.applyPhase2TransformationsChainAndReturnBiotransformations(singleMolecule, true, true, true, nrOfSteps, (double) 0.5F, p2Mode);

}

public static List<Biotransformation> superBioTransformer(IAtomContainer singleMolecule,int p2Mode,int cyp450Mode, boolean useDB, boolean useSub) throws Exception {
    SimulateHumanMetabolism_rails hsbt = new SimulateHumanMetabolism_rails(cyp450Mode, p2Mode, useDB, "hmdb", false, 30, useSub);
    return hsbt.simulateHumanMetabolism(singleMolecule, 4);

} //TODO notwendig?? da SUPERBIO = allHUMAN mit iteration=4

public static List<Biotransformation> allHumanTransformer(IAtomContainer singleMolecule,int nrOfSteps,int p2Mode,int cyp450Mode, boolean useDB, boolean useSub) throws Exception {
    SimulateHumanMetabolism_rails hsbt = new SimulateHumanMetabolism_rails(cyp450Mode, p2Mode, useDB, "hmdb", false, 30, useSub);
    return hsbt.simulateHumanMetabolism(singleMolecule, nrOfSteps);


}


public static List<Biotransformation> envMicrobialTransformer (IAtomContainer singleMolecule, int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
    EnvMicroBTransformer_rails ebt = new EnvMicroBTransformer_rails(useDB, useSub);
    return ebt.applyEnvMicrobialTransformationsChain(singleMolecule, true, true, nrOfSteps, (double) 0.5F);

}

public static List<Biotransformation> multiBioTransformer(IAtomContainer singleMolecule, ArrayList<BiotransformerSequenceStep> transformer, int cyp450Mode, boolean useDB, boolean useSub) throws Exception {
    BiotransformerSequence_rails biotransformerSequence = new BiotransformerSequence_rails(transformer,useDB, useSub);
    return biotransformerSequence.runSequence(singleMolecule,0.5F,cyp450Mode);

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
