import matching.datastructures.AtomContainerE;
import matching.datastructures.AtomE;
import matching.datastructures.Pair;
import matching.datastructures.SideChain;
import org.junit.Test;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.silent.Atom;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import matching.utils.MoleculeManipulator;
import java.util.ArrayList;
import static org.junit.Assert.assertEquals;

public class MoleculeManipulatorTest{

    @Test
    public void testUncyclicSideChain(){
        SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

        try{
            String smiMol = "C(C(=O)O)(N)Cc1ccc(C(=O)O)cc1";
            String smiSC1 = "[C](=[O])[O][H]";
            String smiSC2 = "[N]([H])[H]";

            AtomContainerE molecule = new AtomContainerE(smiParser.parseSmiles(smiMol));
            AtomContainerE sc1 = new AtomContainerE(smiParser.parseSmiles(smiSC1));
            SideChain sideChain1 = new SideChain(sc1, (AtomE) sc1.getAtom(0));
            AtomContainerE sc2 = new AtomContainerE(smiParser.parseSmiles(smiSC2));
            SideChain sideChain2 = new SideChain(sc2, (AtomE) sc2.getAtom(0));

            molecule = MoleculeManipulator.removeSideChain(molecule, sideChain1).getObject1();
            molecule = MoleculeManipulator.removeSideChain(molecule, sideChain2).getObject1();

            String expectedMolSmi = "C(*)(*)Cc1ccc(*)cc1";
            AtomContainerE expectedMolecule = new AtomContainerE(smiParser.parseSmiles(expectedMolSmi));

            assertEquals(true, new UniversalIsomorphismTester().isIsomorph(expectedMolecule, molecule));

        }catch (CDKException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testUncyclicSideChainNumberOfOccurrences(){
        SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

        try{
            String smiMol = "C(C(=O)O)(N)Cc1ccc(C(=O)O)cc1";
            String smiSC1 = "[C](=[O])[O][H]";
            String smiSC2 = "[N]([H])[H]";

            AtomContainerE molecule = new AtomContainerE(smiParser.parseSmiles(smiMol));
            AtomContainerE sc1 = new AtomContainerE(smiParser.parseSmiles(smiSC1));
            SideChain sideChain1 = new SideChain(sc1, (AtomE) sc1.getAtom(0));
            AtomContainerE sc2 = new AtomContainerE(smiParser.parseSmiles(smiSC2));
            SideChain sideChain2 = new SideChain(sc2, (AtomE) sc2.getAtom(0));

            ArrayList<AtomE> removedBridgeNodes = new ArrayList<AtomE>();

            Pair<AtomContainerE, ArrayList<AtomE>> results = MoleculeManipulator.removeSideChain(molecule, sideChain1);
            removedBridgeNodes.addAll(results.getObject2());
            removedBridgeNodes.addAll(MoleculeManipulator.removeSideChain(results.getObject1(), sideChain2).getObject2());

            assertEquals(3, removedBridgeNodes.size());

        }catch (CDKException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testCyclicSideChain(){
        SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

        try{
            String molSmiles = "C(C(=O)O)(N)Cc1ccc(O)cc1";
            String scSmiles = "[c]1[c]([H])[c]([H])[c]([O][H])[c]([H])[c]1([H])";

            AtomContainerE molecule = new AtomContainerE(smiParser.parseSmiles(molSmiles));
            AtomContainerE sc = new AtomContainerE(smiParser.parseSmiles(scSmiles));
            SideChain sideChain = new SideChain(sc, (AtomE) sc.getAtom(0));

            molecule = MoleculeManipulator.removeSideChain(molecule, sideChain).getObject1();

            String expectecSmiles = "C(C(=O)O)(N)C*";
            AtomContainerE expectedMolecule = new AtomContainerE(smiParser.parseSmiles(expectecSmiles));

            assertEquals(true, new UniversalIsomorphismTester().isIsomorph(expectedMolecule, molecule));

        }catch (CDKException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testFormalCharge() {
        SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

        try {
            String molSmiles = "C(C(=O)O)(N)C(c1ccc([O-])cc1)c1ccc(O)cc1";
            String sc1Smiles = "[c]1[c]([H])[c]([H])[c]([O-])[c]([H])[c]1([H])";
            String sc2Smiles = "[O]";

            AtomContainerE molecule = new AtomContainerE(smiParser.parseSmiles(molSmiles));
            AtomContainerE sc1 = new AtomContainerE(smiParser.parseSmiles(sc2Smiles));
            SideChain sideChain1 = new SideChain(sc1, (AtomE) sc1.getAtom(0));
            AtomContainerE sc2 = new AtomContainerE(smiParser.parseSmiles(sc1Smiles));
            SideChain sideChain2 = new SideChain(sc2, (AtomE) sc2.getAtom(0));


            molecule = MoleculeManipulator.removeSideChain(molecule, sideChain1).getObject1();
            molecule = MoleculeManipulator.removeSideChain(molecule, sideChain2).getObject1();

            String expectecSmiles = "C(C(=*)O)(N)C(*)c1ccc(O)cc1";
            AtomContainerE expectedMolecule = new AtomContainerE(smiParser.parseSmiles(expectecSmiles));

            assertEquals(true, new UniversalIsomorphismTester().isIsomorph(expectedMolecule, molecule));

        } catch (CDKException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAromaticity(){
        SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        try {
            String molSmiles = "CC1=CC=CC([Cl])=C1";
            AtomContainerE molecule = new AtomContainerE(smiParser.parseSmiles(molSmiles));

            for(int i = 1; i <= 7; i++){
                if(i != 5){
                    molecule.getBond(i).setIsAromatic(true);
                }
            }

            String scSmiles = "[C]1=CC([Cl])=CC=C1";
            AtomContainerE sc = new AtomContainerE(smiParser.parseSmiles(scSmiles));

            for(int i = 0; i < 7; i++){
                if(i != 2){
                    sc.getBond(i).setIsAromatic(true);
                }
            }

            SideChain sideChain = new SideChain(sc, (AtomE) sc.getAtom(0));

            molecule = MoleculeManipulator.removeSideChain(molecule, sideChain).getObject1();

            String expectedSmiles = "C*";
            AtomContainerE expectedMolecule = new AtomContainerE(smiParser.parseSmiles(expectedSmiles));

            assertEquals(true, new UniversalIsomorphismTester().isIsomorph(expectedMolecule, molecule));

        }catch (CDKException e){
            e.printStackTrace();
        }

    }

    @Test
    public void testMarkedNodesWithSameDepthButWithDifferentBond(){
        SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        try{
            String smiMol = "Cc1ccccc1";
            String smiSC = "[C]([H])([H])[C]1[C]([H])=[C]([H])[C]([H])=[C]([H])[C]([H])1";

            AtomContainerE molecule = new AtomContainerE(smiParser.parseSmiles(smiMol));
            AtomContainerE sideChain = new AtomContainerE(smiParser.parseSmiles(smiSC));
            SideChain sc = new SideChain(sideChain, (AtomE) sideChain.getAtom(0));

            Pair<AtomContainerE, ArrayList<AtomE>> results = MoleculeManipulator.removeSideChain(molecule, sc);
            AtomContainerE newMolecule = results.getObject1();

            UniversalIsomorphismTester isomTester = new UniversalIsomorphismTester();
            assertEquals(true, isomTester.isIsomorph(new AtomContainerE(smiParser.parseSmiles(smiMol)), newMolecule));


        }catch (CDKException e){
            e.printStackTrace();
        }
    }

}

