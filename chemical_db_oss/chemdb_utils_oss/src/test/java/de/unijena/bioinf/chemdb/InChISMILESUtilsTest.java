package de.unijena.bioinf.chemdb;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import static de.unijena.bioinf.chemdb.InChISMILESUtils.*;
import static org.junit.Assert.assertEquals;

public class InChISMILESUtilsTest  {

    @Test
    public void mainConnectedComponentTest() throws CDKException {
        testSmiles("C[NH+](C)(C).[Cl-]", "C[NH+](C)C", false);
        testSmiles("C[NH+](C)(C).[Cl-]", "N(C)(C)C", true);

        testSmiles("C[N+](C)(C)CCO.[OH-]", "OCC[N+](C)(C)C", false);
        testSmiles("C[N+](C)(C)CCO.[OH-]", "OCC[N+](C)(C)C", true);


        testSmiles("CCCCCCCCCCCCCC[N+](C)(C)CC1=CC=CC=C1.O.O", "C=1C=CC(=CC1)C[N+](C)(C)CCCCCCCCCCCCCC", false);
        testSmiles("CCCCCCCCCCCCCC[N+](C)(C)CC1=CC=CC=C1.O.O", "C=1C=CC(=CC1)C[N+](C)(C)CCCCCCCCCCCCCC", true);

        //structures without a main component

        testSmiles("C[N+]1=C2C=C(N)C=CC2=CC2=C1C=C(N)C=C2.NC1=CC2=NC3=C(C=CC(N)=C3)C=C2C=C1", null, false);
        testSmiles("C[N+]1=C2C=C(N)C=CC2=CC2=C1C=C(N)C=C2.NC1=CC2=NC3=C(C=CC(N)=C3)C=C2C=C1", null, true);

        testSmiles("CC(O)C(=O)O.CCOC1=CC2=C(N)C3=C(C=C(N)C=C3)N=C2C=C1.O", null, false);
        testSmiles("CC(O)C(=O)O.CCOC1=CC2=C(N)C3=C(C=C(N)C=C3)N=C2C=C1.O", null, true);

//        //more examples with valid removal
//        "COC1CC(OC2CC(C3OC(C)(O)C(C)CC3C)OC2C2(C)CCC(C3(C)CCC4(CC(O)C(C)C(C(C)C5OC(O)(CC(=O)[O-])C(C)C(OC)C5OC)O4)O3)O2)OC(C)C1OC.[NH4+]";
//        "O=NN([O-])C1=CC=CC=C1.[NH4+]";
//        "CC(C)(COP(=O)([O-])OP(=O)([O-])OCC1OC(N2C=NC3=C2N=CN=C3N)C(O)C1OP(=O)([O-])O)C(O)C(=O)NCCC(=O)NCCS.O.O.[Li+].[Li+].[Li+]";

    }

    @NotNull
    private static void testSmiles(String inputSmiles, String expectedSmiles, boolean adjustCharges) throws CDKException {
        //todo isomorphism check would make this more robust
        IAtomContainer atomContainer = getAtomContainerFromSmiles(inputSmiles);
        atomContainer = getMainConnectedComponentOrNull(atomContainer, adjustCharges);
        String output = expectedSmiles==null ? null : getSmiles(atomContainer);

        assertEquals(expectedSmiles, output);
    }


}
