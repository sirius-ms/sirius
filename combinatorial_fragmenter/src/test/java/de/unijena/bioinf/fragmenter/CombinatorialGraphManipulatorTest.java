package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.ElectronIonization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import org.junit.Test;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.BitSet;

import static org.junit.Assert.*;

public class CombinatorialGraphManipulatorTest {

    private static final CombinatorialFragmenterScoring EMPTY_SCORING = new CombinatorialFragmenterScoring() {
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
            if (edge.target.fragment.innerNode) {
                return (edge.getCut1() != null ? scoreBond(edge.getCut1(), edge.getDirectionOfFirstCut()) : 0) + (edge.getCut2() != null ? scoreBond(edge.getCut2(), edge.getDirectionOfSecondCut()) : 0);
            }else{
                MolecularFormula f = edge.target.fragment.getFormula();
                return -Math.abs(edge.source.fragment.hydrogenRearrangements(f));
            }
        }
    };

    public CombinatorialGraph getCombinatorialGraph(String smiles) throws InvalidSmilesException{
        SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        MolecularGraph molecule = new MolecularGraph(parser.parseSmiles(smiles));

        CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(molecule);
        return fragmenter.createCombinatorialFragmentationGraph((n, nnodes, nnedges) -> true);
    }

    @Test
    public void testATNNoMatchingFragment(){
        try{
            FTree fTree = new FTree(MolecularFormula.parse("C2NO2H5"), new ElectronIonization());
            fTree.addFragment(fTree.getRoot(), new Fragment(1, MolecularFormula.parse("CNH4"), new ElectronIonization()));
            fTree.addFragment(fTree.getRoot(), new Fragment(2,MolecularFormula.parse("CO2H"), new ElectronIonization()));
            fTree.addFragment(fTree.getFragmentAt(1), new Fragment(3, MolecularFormula.parse("NH2"), new ElectronIonization()));
            fTree.addFragmentAnnotation(AnnotatedPeak.class, AnnotatedPeak::none);

            CombinatorialGraph graph = getCombinatorialGraph("C1CC1");
            CombinatorialGraphManipulator.addTerminalNodes(graph, EMPTY_SCORING, fTree);

            int natoms = graph.getRoot().fragment.parent.natoms;
            BitSet bitSet = new BitSet(natoms+1);
            bitSet.set(natoms);

            for(Fragment frag : fTree){
                assertNull(graph.getNode(bitSet));
                int idx = 0;
                while(bitSet.get(idx)){
                    bitSet.set(idx, false);
                    idx++;
                }
                bitSet.set(idx);
            }
        } catch (InvalidSmilesException | UnknownElementException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testATNMatchingFragments(){
        try {
            FTree fTree = new FTree(MolecularFormula.parse("C3H8"), new ElectronIonization());
            fTree.addFragment(fTree.getRoot(), new Fragment(1, MolecularFormula.parse("CH3"), new ElectronIonization()));
            fTree.addFragment(fTree.getRoot(), new Fragment(2, MolecularFormula.parse("C2H5"), new ElectronIonization()));
            fTree.addFragment(fTree.getRoot(), new Fragment(3, MolecularFormula.parse("CH2"), new ElectronIonization()));
            fTree.addFragmentAnnotation(AnnotatedPeak.class, AnnotatedPeak::none);

            CombinatorialGraph graph = getCombinatorialGraph("C1CC1");
            CombinatorialGraphManipulator.addTerminalNodes(graph, EMPTY_SCORING, fTree);

            // Compare node set:
            ArrayList<CombinatorialNode> sortedNodeList = graph.getSortedNodeList();
            int[] bitSet2NumList = sortedNodeList.stream().mapToInt(n -> {
                int num = 0;
                for(int i = 0; i <= graph.getRoot().fragment.parent.natoms; i++){
                    num = num + (n.fragment.bitset.get(i) ? (int) Math.pow(2,i) : 0);
                }
                return num;
            }).toArray();
            int[] bondbreaks = sortedNodeList.stream().mapToInt(n -> n.bondbreaks).toArray();
            int[] depths = sortedNodeList.stream().mapToInt(n -> n.depth).toArray();
            double[] totalScores = sortedNodeList.stream().mapToDouble(n -> n.totalScore).toArray();
            double[] scores = sortedNodeList.stream().mapToDouble(n -> n.score).toArray();

            assertArrayEquals(new int[]{1,2,3,4,5,6,7,8,9,10,11}, bitSet2NumList);
            assertArrayEquals(new int[]{2,2,2,2,2,2,0,0,2,2,2}, bondbreaks);
            assertArrayEquals(new int[]{1,1,1,1,1,1,0,1,2,2,2}, depths);
            assertArrayEquals(new double[]{-2.0,-2.0,-2.0,-2.0,-2.0,-2.0,0.0,-2.0,-3.0,-3.0,-4.0},totalScores, 0.0);
            assertArrayEquals(new double[]{-2.0,-2.0,-2.0,-2.0,-2.0,-2.0,0.0,-2.0,-1.0,-1.0,-2.0}, scores, 0.0);

            // Compare adjacency matrix:
            double minusInf = Double.NEGATIVE_INFINITY;
            double[][] adjMatrix = graph.getAdjacencyMatrix();
            double[][] expAdjMatrix = new double[][]{
                    {minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,-1.0,minusInf,-2.0},
                    {minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,-1.0,minusInf,-2.0},
                    {-1.0,-1.0,minusInf,minusInf,minusInf,minusInf,minusInf, minusInf,minusInf, -1.0, minusInf},
                    {minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,-1.0,minusInf,-2.0},
                    {-1.0,minusInf, minusInf, -1.0, minusInf, minusInf, minusInf, minusInf, minusInf, -1.0, minusInf},
                    {minusInf,-1.0,minusInf,-1.0,minusInf,minusInf,minusInf,minusInf,minusInf,-1.0, minusInf},
                    {-2.0,-2.0,-2.0,-2.0,-2.0,-2.0,minusInf,-2.0,minusInf,minusInf,minusInf},
                    {minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf},
                    {minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf},
                    {minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf},
                    {minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf,minusInf}
            };

            for(int i = 0; i < graph.numberOfNodes(); i++){
                assertArrayEquals(expAdjMatrix[i], adjMatrix[i], 0.0);
            }
        } catch (UnknownElementException | InvalidSmilesException e) {
            e.printStackTrace();
        }
    }

}
