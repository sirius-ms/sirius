package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.ElectronIonization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import org.junit.Test;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;


import java.util.List;

import static org.junit.Assert.assertEquals;

public class CriticalPathInsertionSubtreeCalculatorTest {

    private static CombinatorialFragmenterScoring DEFAULT_SCORING = new CombinatorialFragmenterScoring() {
        @Override
        public double scoreBond(IBond bond, boolean direction) {
            return -1d;
        }

        @Override
        public double scoreFragment(CombinatorialNode fragment) {
            return 0;
        }

        @Override
        public double scoreEdge(CombinatorialEdge edge) {
            if(edge.target.fragment.isInnerNode()){
                return this.scoreBond(edge.getCut1(), edge.getDirectionOfFirstCut()) + (edge.getCut2() != null ? scoreBond(edge.getCut2(), edge.getDirectionOfSecondCut()) : 0);
            }else{
                return 10 - Math.abs(edge.source.fragment.hydrogenRearrangements(edge.target.fragment.getFormula()));
            }
        }
    };

    @Test
    public void testCyclobutadiene(){
        try{
            String smiles = "C1=CC=C1";
            SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            MolecularGraph molecule = new MolecularGraph(parser.parseSmiles(smiles));

            FTree fTree = new FTree(MolecularFormula.parse("C4H4"), new ElectronIonization());
            fTree.addFragment(fTree.getRoot(), new Fragment(1, MolecularFormula.parse("C2H2"), new ElectronIonization()));
            fTree.addFragment(fTree.getRoot(), new Fragment(2, MolecularFormula.parse("CH2"), new ElectronIonization()));

            CriticalPathInsertionSubtreeCalculator subtreeCalc = new CriticalPathInsertionSubtreeCalculator(fTree, molecule, DEFAULT_SCORING);
            subtreeCalc.initialize(n -> true);
            CombinatorialSubtree subtree = subtreeCalc.computeSubtree();

            assertEquals(25.0, subtreeCalc.getScore(), 0.0);
            assertEquals(25.0, subtree.getScore(), 0.0);
        }catch(UnknownElementException | InvalidSmilesException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testGetListWithAmountOfHydrogenRearrangements(){
        try{
            String smiles = "C1=CC=C1";
            SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            MolecularGraph molecule = new MolecularGraph(parser.parseSmiles(smiles));

            FTree fTree = new FTree(MolecularFormula.parse("C4H4"), new ElectronIonization());
            fTree.addFragment(fTree.getRoot(), new Fragment(1, MolecularFormula.parse("C2H2"), new ElectronIonization()));
            fTree.addFragment(fTree.getRoot(), new Fragment(2, MolecularFormula.parse("CH2"), new ElectronIonization()));

            CriticalPathInsertionSubtreeCalculator subtreeCalc = new CriticalPathInsertionSubtreeCalculator(fTree, molecule, DEFAULT_SCORING);
            subtreeCalc.initialize(n -> true);
            CombinatorialSubtree subtree = subtreeCalc.computeSubtree();

            List<Integer> actualHydrogenRearrangementList = subtreeCalc.getListWithAmountOfHydrogenRearrangements();
            int[] expectedHydrogenRearrangements = new int[]{0,0,1};

            assertEquals(3, actualHydrogenRearrangementList.size());

            for(int i = 0; i < 3; i++){
                assertEquals(expectedHydrogenRearrangements[i], actualHydrogenRearrangementList.get(i).intValue());
            }

        }catch(UnknownElementException | InvalidSmilesException e){
            e.printStackTrace();
        }
    }
}
