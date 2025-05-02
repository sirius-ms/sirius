package de.unijena.bioinf.ms.biotransformer;


import biotransformer.biosystems.BioSystem.*;
import biotransformer.railsappspecific.*;
import biotransformer.transformation.Biotransformation;
import biotransformer.utils.BiotransformerSequence;
import biotransformer.utils.BiotransformerSequenceStep;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.List;

public class BiotransformerWrapper{

public static List<Biotransformation> abioticTransformer(IAtomContainer singleMolecule, int nrOfSteps) throws Exception {
    AbioticTransformer_rails abiotic_bt = new AbioticTransformer_rails(false, false);
    return abiotic_bt.applyAbioticTransformationsChain(singleMolecule, true, true, nrOfSteps, (double) 0.5F);
}


public static List<Biotransformation> cyp450BTransformer(IAtomContainer singleMolecule,int nrOfSteps, Cyp450Mode cyp450Mode, boolean useDB,boolean useSub) throws Exception {
    Cyp450BTransformer_rails cyp450bt = new Cyp450BTransformer_rails(BioSystemName.HUMAN, useDB, useSub);
   return cyp450bt.predictCyp450BiotransformationChainByMode(singleMolecule, true, true, nrOfSteps, (double) 0.5F, cyp450Mode.getCyp450ModeOrdinal());

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

public static List<Biotransformation> superBioTransformer(IAtomContainer singleMolecule,int p2Mode,Cyp450Mode cyp450Mode, boolean useDB, boolean useSub) throws Exception {
    SimulateHumanMetabolism_rails hsbt = new SimulateHumanMetabolism_rails(cyp450Mode.getCyp450ModeOrdinal(), p2Mode, useDB, "hmdb", false, 30, useSub);
    return hsbt.simulateHumanMetabolism(singleMolecule, 4);

} //TODO notwendig?? da SUPERBIO = allHUMAN mit iteration=4

public static List<Biotransformation> allHumanTransformer(IAtomContainer singleMolecule,int nrOfSteps,int p2Mode,Cyp450Mode cyp450Mode, boolean useDB, boolean useSub) throws Exception {
    SimulateHumanMetabolism_rails hsbt = new SimulateHumanMetabolism_rails(cyp450Mode.getCyp450ModeOrdinal(), p2Mode, useDB, "hmdb", false, 30, useSub);
    return hsbt.simulateHumanMetabolism(singleMolecule, nrOfSteps);


}


public static List<Biotransformation> envMicrobialTransformer (IAtomContainer singleMolecule, int nrOfSteps, boolean useDB, boolean useSub) throws Exception {
    EnvMicroBTransformer_rails ebt = new EnvMicroBTransformer_rails(useDB, useSub);
    return ebt.applyEnvMicrobialTransformationsChain(singleMolecule, true, true, nrOfSteps, (double) 0.5F);

}

public static List<Biotransformation> multiBioTransformer(IAtomContainer singleMolecule, ArrayList<BiotransformerSequenceStep> transformer, Cyp450Mode cyp450Mode, boolean useDB, boolean useSub) throws Exception {
    if (transformer == null || transformer.isEmpty()){
        throw new IllegalArgumentException("sequenceSteps has to be set!");
    }
        BiotransformerSequence_rails biotransformerSequence = new BiotransformerSequence_rails(transformer,useDB, useSub);
    return biotransformerSequence.runSequence(singleMolecule,0.5F, cyp450Mode.getCyp450ModeOrdinal());

}



    public static void main(String[] args) throws Exception {
        // default
        int p2Mode = 1;
        boolean useDB = true;
        boolean useSub = false;
        int steps= 2;

        String smiles = "CC(C)C1=CC=C(C)C=C1O";
        IAtomContainer molecule = InChISMILESUtils.getAtomContainerFromSmiles(smiles);

        List<Biotransformation> result = BiotransformerWrapper.allHumanTransformer(molecule, steps, p2Mode, Cyp450Mode.COMBINED, useDB, useSub);
        System.out.println("RESULTS: " + result.size());
    }
}
