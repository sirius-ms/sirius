import matching.algorithm.ElectronPairWiseEDIC;
import org.junit.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ElectronPairWiseEDICTest {

    private final static SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

    @Test
    public void testDoubleBondDeletion1(){
        try{
            String smiMol1 = "CCCCCC(C=CC1C(CC(=O)C1CCCCCCC(=O)NCCO)O)O";
            String smiMol2 = "CCCCCC(C=CC1C(CC(C1CC=CCCCC(=O)NCCO)O)O)O";
            IAtomContainer mol1 = smiParser.parseSmiles(smiMol1);
            IAtomContainer mol2 = smiParser.parseSmiles(smiMol2);

            ElectronPairWiseEDIC matcher = new ElectronPairWiseEDIC(mol1,mol2);
            assertEquals(1.0, matcher.getScore(),0.0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testDoubleBondDeletion2(){
        try{
            String smiMol1 = "CC1C2CCC3(C(C2(CCC1O)C)C(=O)CC4C3(CC(C4=C(CCC=C(C)C)C(=O)O)OC(=O)C)C)C";
            String smiMol2 = "CC1C2CCC3(C(C2(CCC1=O)C)C(CC4C3(CC(C4=C(CCC=C(C)C)C(=O)O)OC(=O)C)C)O)C";
            IAtomContainer mol1 = smiParser.parseSmiles(smiMol1);
            IAtomContainer mol2 = smiParser.parseSmiles(smiMol2);

            ElectronPairWiseEDIC matcher = new ElectronPairWiseEDIC(mol1,mol2);
            assertEquals(1.0,matcher.getScore(),0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testSingeBondDeletion1(){
        try{
            String smiMol1 = "CN(C)CCC1=CNC2=C1C(=CC=C2)OC";
            String smiMol2 = "CN(C)CCC1=CNC2=C1C=C(C=C2)OC";
            IAtomContainer mol1 = smiParser.parseSmiles(smiMol1);
            IAtomContainer mol2 = smiParser.parseSmiles(smiMol2);
            ElectronPairWiseEDIC matcher = new ElectronPairWiseEDIC(mol1,mol2);

            assertEquals(1.0,matcher.getScore(),0.0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testSingleBondDeletion2(){
        try{
            String smiMol1 = "C(C(=O)O)(N)CC(C)C"; // leucine
            String smiMol2 = "C(C(=O)O)(N)C(C)CC"; // isoleucine
            IAtomContainer mol1 = smiParser.parseSmiles(smiMol1);
            IAtomContainer mol2 = smiParser.parseSmiles(smiMol2);
            ElectronPairWiseEDIC matcher = new ElectronPairWiseEDIC(mol1,mol2);

            assertEquals(1.0,matcher.getScore(),0.0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testNegativeExampleSameMolecularFormula1(){
        try{
            String smiMol1 = "C1=CC=C(C=C1)C=CCCCOO";
            String smiMol2 = "CCC(=O)OCCC1=CC=CC=C1";
            IAtomContainer mol1 = smiParser.parseSmiles(smiMol1);
            IAtomContainer mol2 = smiParser.parseSmiles(smiMol2);

            ElectronPairWiseEDIC matcher = new ElectronPairWiseEDIC(mol1,mol2);
            assertEquals(0.0,matcher.getScore(),0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testNegativeExampleSameMolecularFormula2(){
        try{
            String smiMol1 = "CC1(CC2=C(O1)C(=CC=C2)O)C";
            String smiMol2 = "CCOC(=O)CC1=CC=CC=C1";
            IAtomContainer mol1 = smiParser.parseSmiles(smiMol1);
            IAtomContainer mol2 = smiParser.parseSmiles(smiMol2);

            ElectronPairWiseEDIC matcher = new ElectronPairWiseEDIC(mol1,mol2);
            assertEquals(0.0, matcher.getScore(),0.0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testNegativeExampleDifferentMolecularFormula(){
        try{
            String smiMol1 = "C(C(=O)O)(N)CC1=CC=C(O)C=C1"; // tyrosine
            String smiMol2 = "C(C(=O)O)(N)CC1=CC=CC=C1"; // phenylalanine
            IAtomContainer mol1 = smiParser.parseSmiles(smiMol1);
            IAtomContainer mol2 = smiParser.parseSmiles(smiMol2);

            ElectronPairWiseEDIC matcher = new ElectronPairWiseEDIC(mol1,mol2);
            assertEquals(0.0,matcher.getScore(),0.0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testSameMolecules(){
        try{
            String smiMol = "C(C(=O)O)(N)CC1=CC=C(O)C=C1";
            IAtomContainer mol = smiParser.parseSmiles(smiMol);
            ElectronPairWiseEDIC matcher = new ElectronPairWiseEDIC(mol,mol);
            assertEquals(1.0,matcher.getScore(),0.0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testMoleculesWithHydrogensAndAromaticityAfterComparison(){
        try{
            String inputSmiles = "C(C(=O)O)(N)CC1=CC=C(O)C=C1";
            String otherSmiles = "C(C(=O)O)(N)Cc1ccccc1O";
            String expectedMolSmiles = "C(C(=O)O[H])(N([H])[H])([H])C([H])([H])c1c([H])c([H])c(O[H])c([H])c1[H]";

            IAtomContainer mol = smiParser.parseSmiles(inputSmiles);
            IAtomContainer expectedMol = smiParser.parseSmiles(expectedMolSmiles);
            IAtomContainer otherMol = smiParser.parseSmiles(otherSmiles);

            ElectronPairWiseEDIC matcher = new ElectronPairWiseEDIC(mol, otherMol);
            matcher.compare();

            IAtomContainer actualMol = matcher.getFirstMolecule();

            UniversalIsomorphismTester isomorphismTester = new UniversalIsomorphismTester();
            assertTrue(isomorphismTester.isIsomorph(expectedMol, actualMol));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
