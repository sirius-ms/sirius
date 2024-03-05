import matching.algorithm.MinimalWeightedPlacements;
import matching.datastructures.*;
import matching.io.SideChainListReader;
import org.junit.Test;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import matching.utils.HungarianAlgorithm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

public class MinimalWeightedPlacementsTest {

    @Test(expected = IllegalArgumentException.class)
    public void testEmptySideChainList(){
        try {
            String smiles = "c1ccccc1";
            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            AtomContainerE molecule = new AtomContainerE(smiParser.parseSmiles(smiles));

            MinimalWeightedPlacements alg = new MinimalWeightedPlacements(molecule, molecule, new SideChainList());
        }catch (CDKException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testNoSideChainOccursInBothMolecules(){
        try {
            String smilesMol = "C1=NC2=C(N1)C(=O)N=CN2";

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            AtomContainerE molecule1 = this.getAtomContainerE(smiParser, smilesMol);
            AtomContainerE molecule2 = this.getAtomContainerE(smiParser, smilesMol);

            AtomContainerE sc = new AtomContainerE(smiParser.parseSmiles("[C](=[O])[O][H]"));
            SideChainList scList = new SideChainList();
            scList.add(new SideChain(sc, (AtomE) sc.getAtom(0)));

            MinimalWeightedPlacements alg = new MinimalWeightedPlacements(molecule1, molecule2, scList);

            assertEquals(0.0, alg.compare(), 0);

        } catch (CDKException e){
            e.printStackTrace();
        }

    }

    @Test
    public void testDifferentMolecularFormula() {
        try {
            String smilesMol1 = "c1ccccc1";
            String smilesMol2 = "c1ccc(O)cc1";

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            AtomContainerE molecule1 = this.getAtomContainerE(smiParser, smilesMol1);
            AtomContainerE molecule2 = this.getAtomContainerE(smiParser, smilesMol2);

            InputStream is = getClass().getClassLoader().getResourceAsStream("sideChains.txt");
            SideChainListReader scReader = new SideChainListReader(new InputStreamReader(is));
            scReader.readFile();

            MinimalWeightedPlacements alg = new MinimalWeightedPlacements(molecule1, molecule2, scReader.getSideChainList());
            double actualDistance = alg.compare();

            assertEquals(Double.MAX_VALUE, actualDistance, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDifferentSideChains(){
        try {
            String smilesMol1 = "c1(C(=O)O)cc(C(=O)O)c(S)cc1(N)";
            String smilesMol2 = "c1(N)c(C=O)c(S)c(O)c(C=O)c1(O)";

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            AtomContainerE molecule1 = this.getAtomContainerE(smiParser, smilesMol1);
            AtomContainerE molecule2 = this.getAtomContainerE(smiParser, smilesMol2);

            InputStream is = getClass().getClassLoader().getResourceAsStream("sideChains.txt");
            SideChainListReader scReader = new SideChainListReader(new InputStreamReader(is));
            scReader.readFile();

            MinimalWeightedPlacements alg = new MinimalWeightedPlacements(molecule1, molecule2, scReader.getSideChainList());
            double actualDistance = alg.compare();

            assertEquals(Double.MAX_VALUE, actualDistance, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDifferentSkeletonStructure(){
        try {
            String smilesFructofuranose = "C1(CO)(O)C(O)C(O)C(CO)O1";
            String smilesGlucopyranose = "C1(O)C(O)C(O)C(O)C(CO)O1";

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            AtomContainerE fructofuranose = this.getAtomContainerE(smiParser, smilesFructofuranose);
            AtomContainerE glucopyranose = this.getAtomContainerE(smiParser, smilesGlucopyranose);

            InputStream is = getClass().getClassLoader().getResourceAsStream("sideChains.txt");
            SideChainListReader scReader = new SideChainListReader(new InputStreamReader(is));
            scReader.readFile();

            MinimalWeightedPlacements alg = new MinimalWeightedPlacements(fructofuranose, glucopyranose, scReader.getSideChainList());

            assertEquals(Double.MAX_VALUE, alg.compare(), 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMoleculesWithOnlyDifferentSideChainArrangement1(){
        try{
            String smilesLeucine = "CC(C)CC(C(=O)O)N";
            String smilesIsoleucine = "CCC(C)C(C(=O)O)N";

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            AtomContainerE leucine = this.getAtomContainerE(smiParser, smilesLeucine);
            AtomContainerE isoleucine = this.getAtomContainerE(smiParser, smilesIsoleucine);

            InputStream is = getClass().getClassLoader().getResourceAsStream("sideChains.txt");
            SideChainListReader scReader = new SideChainListReader(new InputStreamReader(is));
            scReader.readFile();

            MinimalWeightedPlacements alg = new MinimalWeightedPlacements(leucine, isoleucine, scReader.getSideChainList());

            assertEquals(20.0, alg.compare(), 0.0);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testMoleculesWithOnlyDifferentSideChainArrangement2(){
        try{
            String smilesMol1 = "C1=C2C(=C(C=C1C(=O)O)O[P](=O)([O-])[O-])C(=CC(=C2CO)C(=O)O)N";
            String smilesMol2 = "C1(=C2C(=C(C=C1C(=O)O)CC(=O)O)C=CC(=C2O)O[P](=O)([O-])[O-])N";
            String smilesMol3 = "C1(=C2C(=C(C=C1C(=O)O)CC(=O)O)C=CC(=C2O)N)O[P](=O)([O-])[O-]";

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            AtomContainerE molecule1 = this.getAtomContainerE(smiParser, smilesMol1);
            AtomContainerE molecule2 = this.getAtomContainerE(smiParser, smilesMol2);
            AtomContainerE molecule3 = this.getAtomContainerE(smiParser, smilesMol3);

            InputStream is = getClass().getClassLoader().getResourceAsStream("sideChains.txt");
            SideChainListReader scReader = new SideChainListReader(new InputStreamReader(is));
            scReader.readFile();

            MinimalWeightedPlacements alg1 = new MinimalWeightedPlacements(molecule1, molecule2, scReader.getSideChainList());
            assertEquals(24.0, alg1.compare(), 0.0);

            MinimalWeightedPlacements alg2 = new MinimalWeightedPlacements(molecule1, molecule3, scReader.getSideChainList());
            assertEquals(30.0, alg2.compare(), 0.0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testSameMolecules(){
        try{
            String smilesMol = "C(N)(C(=O)O)CC1=CC(=C(C(=C1O[P](=O)([O-])[O-])S)O)C(=O)O";

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            AtomContainerE molecule1 = this.getAtomContainerE(smiParser, smilesMol);
            AtomContainerE molecule2 = this.getAtomContainerE(smiParser, smilesMol);

            InputStream is = getClass().getClassLoader().getResourceAsStream("sideChains.txt");
            SideChainListReader scReader = new SideChainListReader(new InputStreamReader(is));
            scReader.readFile();

            MinimalWeightedPlacements alg = new MinimalWeightedPlacements(molecule1, molecule2, scReader.getSideChainList());

            assertEquals(0.0, alg.compare(), 0.0);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testSameObjects(){
        try {
            String smilesMol = "c1ccc(O)cc1";

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            AtomContainerE molecule = this.getAtomContainerE(smiParser, smilesMol);

            InputStream is = getClass().getClassLoader().getResourceAsStream("sideChains.txt");
            SideChainListReader scReader = new SideChainListReader(new InputStreamReader(is));
            scReader.readFile();

            MinimalWeightedPlacements alg = new MinimalWeightedPlacements(molecule, molecule, scReader.getSideChainList());

            assertEquals(0.0, alg.compare(), 0.0);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private AtomContainerE getAtomContainerE(SmilesParser smiParser, String smiles) throws CDKException {
        AtomContainerE molecule = new AtomContainerE(smiParser.parseSmiles(smiles));
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
        Aromaticity.cdkLegacy().apply(molecule);
        return molecule;
    }
}
