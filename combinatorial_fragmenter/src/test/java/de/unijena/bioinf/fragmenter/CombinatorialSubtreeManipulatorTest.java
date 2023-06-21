package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import org.junit.Test;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CombinatorialSubtreeManipulatorTest {

    public CombinatorialSubtree createSubtree(float[] fragmentScores, float[] edgeScores) throws InvalidSmilesException, UnknownElementException {
        String smiles = "C";
        SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        MolecularGraph molecule = new MolecularGraph(parser.parseSmiles(smiles));

        CombinatorialSubtree subtree = new CombinatorialSubtree(molecule);
        CombinatorialFragment[] fragments = new CombinatorialFragment[12];
        BitSet bitset = new BitSet(4);
        bitset.set(1);
        for(int i = 0; i < fragments.length; i++){
            fragments[i] = new CombinatorialFragment(molecule, (BitSet) bitset.clone(), MolecularFormula.parse("C"+(1+i)), new BitSet(), false, 0f);
            this.incrementBitSet(bitset);
        }

        for(int i = 0; i < 3; i++) subtree.addFragment(subtree.getRoot(), fragments[i], null, null, fragmentScores[i], edgeScores[i]);
        for(int i = 3; i < 5; i++) subtree.addFragment(subtree.getNode(fragments[0].bitset), fragments[i], null,null,fragmentScores[i],edgeScores[i]);
        for(int i = 5; i < 8; i++) subtree.addFragment(subtree.getNode(fragments[2].bitset), fragments[i], null,null,fragmentScores[i],edgeScores[i]);
        for(int i = 8; i < 10; i++) subtree.addFragment(subtree.getNode(fragments[5].bitset), fragments[i], null,null,fragmentScores[i],edgeScores[i]);
        for(int i = 10;i < 12; i++) subtree.addFragment(subtree.getNode(fragments[7].bitset), fragments[i], null,null,fragmentScores[i],edgeScores[i]);

        return subtree;
    }

    private void incrementBitSet(BitSet bitset){
        int idx = 0;
        while(bitset.get(idx)){
            bitset.set(idx, false);
            idx++;
        }
        bitset.set(idx);
    }

    private int bitset2Integer(BitSet bitset, int nbits){
        int num = 0;
        for(int idx = 0; idx < nbits; idx++){
            num = num + (bitset.get(idx) ? (int) Math.pow(2,idx) : 0);
        }
        return num;
    }

    @Test
    public void testPositiveScores(){
        try{
            float[] fragmentScores = new float[12];
            float[] edgeScores = new float[12];
            Arrays.fill(fragmentScores, 1f);
            Arrays.fill(edgeScores, 1f);
            CombinatorialSubtree subtree = this.createSubtree(fragmentScores, edgeScores);
            // TEST 'removeDanglingSubtrees'
            // 1. Test if the returned score is equal to the subtree score:
            double bestSubtreeScore  = CombinatorialSubtreeManipulator.removeDanglingSubtrees(subtree);
            assertEquals(24, bestSubtreeScore, 0.0);
            assertEquals(24, subtree.getScore(), 0.0);

            // 2. Test if the topology is not changed:
            String newickStr = subtree.toString();
            String expNewickStr = "((C4[1.0,1.0,0],C5[1.0,1.0,0])C[1.0,1.0,0],C2[1.0,1.0,0],((C9[1.0,1.0,0],C10[1.0,1.0,0])C6[1.0,1.0,0],C7[1.0,1.0,0],(C11[1.0,1.0,0],C12[1.0,1.0,0])C8[1.0,1.0,0])C3[1.0,1.0,0])C[0,0.0,0];";
            assertEquals(expNewickStr, newickStr);


        } catch (InvalidSmilesException | UnknownElementException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNegativeScores(){
        try{
            float[] fragmentScores = new float[12];
            float[] edgeScores = new float[12];
            Arrays.fill(fragmentScores, -1f);
            Arrays.fill(edgeScores, -1f);
            CombinatorialSubtree subtree = this.createSubtree(fragmentScores, edgeScores);

            assertEquals(-24.0, subtree.getScore(), 0.0);

            // 1. Check the score of the subtree after calling 'removeDanglingSubtrees' (RDS):
            assertEquals(0.0, CombinatorialSubtreeManipulator.removeDanglingSubtrees(subtree), 0.0);
            assertEquals(0.0, subtree.getScore(), 0.0);

            // 2. Check topology - only the root must be contained in the subtree after calling RDS:
            assertEquals(1, subtree.numberOfNodes());

            ArrayList<CombinatorialNode> sortedNodeList = subtree.getSortedNodeList();
            assertArrayEquals(new int[]{1}, sortedNodeList.stream().mapToInt(n -> this.bitset2Integer(n.fragment.bitset, 4)).toArray());

        } catch (UnknownElementException | InvalidSmilesException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testScoreIsInitiallyZero(){
        try{
            float[] fragmentScores = new float[12], edgeScores = new float[12];
            Arrays.fill(fragmentScores, 1f);
            Arrays.fill(edgeScores, -1f);
            CombinatorialSubtree subtree = this.createSubtree(fragmentScores, edgeScores);

            assertEquals(0.0, subtree.getScore(), 0.0);

            // 1. Check if the score is equal 0 after calling RDS:
            assertEquals(0.0, CombinatorialSubtreeManipulator.removeDanglingSubtrees(subtree), 0.0);
            assertEquals(0.0, subtree.getScore(), 0.0);

            // 2. Check the topology of the tree:
            assertEquals(13, subtree.numberOfNodes());
            String newickStr = subtree.toString();
            String expNewickStr = "((C4[-1.0,1.0,0],C5[-1.0,1.0,0])C[-1.0,1.0,0],C2[-1.0,1.0,0],((C9[-1.0,1.0,0],C10[-1.0,1.0,0])C6[-1.0,1.0,0],C7[-1.0,1.0,0],(C11[-1.0,1.0,0],C12[-1.0,1.0,0])C8[-1.0,1.0,0])C3[-1.0,1.0,0])C[0,0.0,0];";
            assertEquals(expNewickStr, newickStr);

        } catch (UnknownElementException | InvalidSmilesException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPostiveAndNegativeSubtreeScores(){
        try{
            float[] fragmentScores = new float[]{1f,0f,1f,0f,1f,0f,0f,3f,1f,1f,0f,1f};
            float[] edgeScores = new float[]{1f,-1f,1f,-1f,-1f,-1f,1f,-1f,-1f,-1f,-1f,-1f};
            CombinatorialSubtree subtree = this.createSubtree(fragmentScores, edgeScores);
            assertEquals(3.0, subtree.getScore(), 0.0);

            // 1. Check the scores:
            assertEquals(7.0, CombinatorialSubtreeManipulator.removeDanglingSubtrees(subtree), 0.0);
            assertEquals(7.0, subtree.getScore(), 0.0);

            // 2. Check the tree topology:
            assertEquals(7, subtree.numberOfNodes());
            String newickStr = subtree.toString();
            String expNewickStr = "((C5[-1.0,1.0,0])C[1.0,1.0,0],(C7[1.0,0.0,0],(C12[-1.0,1.0,0])C8[-1.0,3.0,0])C3[1.0,1.0,0])C[0,0.0,0];";
            assertEquals(expNewickStr, newickStr);

        } catch (UnknownElementException | InvalidSmilesException e) {
            e.printStackTrace();
        }
    }
}
