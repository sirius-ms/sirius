package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import org.junit.Test;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.junit.Assert.*;

public class CombinatorialFragmenterTest {

    @Test
    public void testCutBond(){
        try {
            String smiles = "[H]C([H])([H])C(=O)O[H]";
            SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            IAtomContainer aceticAcid = parser.parseSmiles(smiles);
            MolecularGraph mol = new MolecularGraph(aceticAcid);

            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(mol);
            CombinatorialFragment[] fragments = fragmenter.cutBond(mol.asFragment(),3);

            BitSet frag1 = new BitSet(mol.natoms);
            frag1.set(0,4);
            BitSet frag2 = new BitSet(mol.natoms);
            frag2.set(4,8);

            assertTrue(frag1.equals(fragments[0].bitset) && frag2.equals(fragments[1].bitset));
        } catch(CDKException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testCutRing(){
        try{
            String smiles = "c1(O)ccc(O)cc1";
            SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            IAtomContainer quinol = parser.parseSmiles(smiles);
            MolecularGraph mol = new MolecularGraph(quinol);

            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(mol);
            CombinatorialFragment[] fragments = fragmenter.cutRing(mol.asFragment(), 0,2,6);

            BitSet frag1 = new BitSet(mol.natoms);
            frag1.set(0,3);
            frag1.set(7);
            BitSet frag2 = (BitSet) frag1.clone();
            frag2.flip(0, mol.natoms);

            boolean containCorrectAtoms = frag1.equals(fragments[0].bitset) && frag2.equals(fragments[1].bitset);
            boolean noRingAnymore = fragments[0].disconnectedRings.cardinality() == 1 && fragments[1].disconnectedRings.cardinality() == 1;

            assertTrue(containCorrectAtoms && noRingAnymore);
        }catch(CDKException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testCutAllBonds(){
        try{
            String smiles = "CC1CC1";
            SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            IAtomContainer molecule = parser.parseSmiles(smiles);
            MolecularGraph mol = new MolecularGraph(molecule);

            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(mol);
            List<CombinatorialFragment> fragments = fragmenter.cutAllBonds(mol.asFragment(), null);

            ArrayList<BitSet> bitsets = new ArrayList<>();

            // Schneiden der Bindung, die nicht zu einem Ring gehört
            BitSet frag = new BitSet(mol.natoms);
            frag.set(0);
            bitsets.add((BitSet) frag.clone());
            frag.flip(0,mol.natoms);
            bitsets.add((BitSet) frag.clone());

            // Schneiden der ersten und dritten Bindung im Ring:
            frag.clear();
            frag.set(0,2);
            bitsets.add((BitSet) frag.clone());
            frag.flip(0, mol.natoms);
            bitsets.add((BitSet) frag.clone());

            // Schneiden der ersten und zweiten Bindung im Ring:
            frag.clear();
            frag.set(0,2);
            frag.set(3);
            bitsets.add((BitSet) frag.clone());
            frag.flip(0, mol.natoms);
            bitsets.add((BitSet) frag.clone());

            // Schneiden der zweiten und dritten Bindung im Ring:
            frag.clear();
            frag.set(0,3);
            bitsets.add((BitSet) frag.clone());
            frag.flip(0, mol.natoms);
            bitsets.add((BitSet) frag.clone());

            assertEquals(bitsets.size(), fragments.size());

            for(CombinatorialFragment fragment : fragments){
                bitsets.remove(fragment.bitset);
            }

            assertTrue(bitsets.isEmpty());
        } catch (InvalidSmilesException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEmptyMolecule(){
        IAtomContainer molecule = new AtomContainer();
        MolecularGraph mol = new MolecularGraph(molecule);
        CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(mol);
        CombinatorialGraph fragGraph = fragmenter.createCombinatorialFragmentationGraph(n -> true);
        assertTrue(fragGraph.getNodes().isEmpty()); //fragGraph.nodes is the set of all nodes without the root
    }

    @Test
    public void testCreateCombinatorialGraph(){
        try{
            String smiles = "C1CC1";
            SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            MolecularGraph mol = new MolecularGraph(parser.parseSmiles(smiles));

            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(mol);
            CombinatorialGraph graph = fragmenter.createCombinatorialFragmentationGraph(n -> true);

            assertEquals(6,graph.getNodes().size());

            CombinatorialNode root = graph.getRoot();
            assertEquals(0f, root.fragmentScore, 0);
            assertEquals(0, root.incomingEdges.size());
            assertEquals(6, root.outgoingEdges.size());


            BitSet first = new BitSet(3); first.set(0);
            BitSet second = new BitSet(3); second.set(1);
            BitSet third = new BitSet(3); third.set(2);
            BitSet fstScnd = new BitSet(3); fstScnd.set(0,2);
            BitSet fstThrd = new BitSet(3); fstThrd.set(0); fstThrd.set(2);
            BitSet scndThrd = new BitSet(3); scndThrd.set(1,3);

            for(CombinatorialEdge edge : root.outgoingEdges){
                CombinatorialNode node = edge.target;
                BitSet fragBitSet = node.fragment.bitset;

                assertEquals(-2, edge.score, 0);
                assertEquals(0, node.fragmentScore, 0);
                assertEquals(-2d, node.score, 0);
                assertEquals(-2d, node.totalScore, 0);

                if(fragBitSet.equals(first) || fragBitSet.equals(second) || fragBitSet.equals(third)){
                    assertEquals(0, node.outgoingEdges.size());
                    assertEquals(3, node.incomingEdges.size());
                }else if(fragBitSet.equals(fstScnd) || fragBitSet.equals(fstThrd) || fragBitSet.equals(scndThrd)){
                    assertEquals(1, node.incomingEdges.size());
                    assertEquals(2, node.outgoingEdges.size());

                    for(CombinatorialEdge edge2 : node.outgoingEdges) {
                        String fragStr = edge2.target.fragment.bitset.toString();
                        assertEquals(-1d, edge2.score, 0);

                        if (fragBitSet.equals(fstScnd)) {
                            assertTrue(fragStr.equals("{0}") || fragStr.equals("{1}"));
                        } else if (fragBitSet.equals(fstThrd)) {
                            assertTrue(fragStr.equals("{0}") || fragStr.equals("{2}"));
                        } else {
                            assertTrue(fragStr.equals("{1}") || fragStr.equals("{2}"));
                        }
                    }
                }else{
                    fail("The graph contains another fragment.");
                }
            }

        } catch (InvalidSmilesException e) {
            e.printStackTrace();
        }
    }

}
