package de.unijena.bioinf.ms.biotransformer;

import de.unijena.bioinf.chemdb.InChISMILESUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BioTransformerWrapperTest {

    @Test
    void cyp450BTest() throws Exception {
        String smiles = "CC(C)C1=CC=C(C)C=C1O";
        IAtomContainer molecule = InChISMILESUtils.getAtomContainerFromSmiles(smiles);

        BioTransformerResult result = BioTransformerWrapper.cyp450BTransformer(molecule, 1, Cyp450Mode.RULE_BASED, false, false);
        assertEquals(6, result.biotranformations().size());
    }

    @Test
    @Disabled
    void allHumanTest() throws Exception {
        // default
        int p2Mode = 1;
        boolean useDB = true;
        boolean useSub = false;
        int steps = 2;

        String smiles = "CC(C)C1=CC=C(C)C=C1O";
        IAtomContainer molecule = InChISMILESUtils.getAtomContainerFromSmiles(smiles);

        BioTransformerResult result = BioTransformerWrapper.allHumanTransformer(molecule, steps, p2Mode, Cyp450Mode.COMBINED, useDB, useSub);
        System.out.println("RESULTS: " + result.biotranformations().size());
    }
}