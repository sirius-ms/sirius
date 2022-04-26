package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.babelms.dot.Graph;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;

/**
 *  Utility class to edit a CombinatorialGraph object.
 */
public class CombinatorialGraphManipulator {

    private static BitSet toBitSet(int number){
        BitSet bitset = new BitSet();
        int currentNumber = number;
        int idx = 0;

        while(currentNumber > 0){
            int newNumber = currentNumber / 2;
            int rest = currentNumber - 2*newNumber;
            if(rest == 1) bitset.set(idx);
            currentNumber = newNumber;
            idx++;
        }
        return bitset;
    }

    /**
     * This method adds for each vertex in the given {@link FTree} a terminal node in the {@link CombinatorialGraph}
     * and connects each {@link CombinatorialNode} with the vertex if their molecular formula differs only
     * in the number of hydrogen atoms.<br>
     *
     * If there is no such {@link CombinatorialNode}, the corresponding terminal node won't be added to the graph.<br>
     *
     * @param graph the in silico fragmentation graph
     * @param scoring the scoring object which assigns each edge and node a score
     * @param fTree the fragmentation tree which explains the measured MS2 spectrum
     */
    public static void addTerminalNodes(CombinatorialGraph graph, CombinatorialFragmenterScoring scoring, FTree fTree){
        graph.nodes.forEach(x->x.state=0);
        MolecularGraph molecule = graph.getRoot().fragment.parent;

        // 1. Create the hashmap which assigns each MF.withoutHydrogen()
        // a set of CombinatorialNodes whose molecular formula differ only in the number of hydrogen atoms
        HashMap<MolecularFormula, ArrayList<CombinatorialNode>> mf2NodeSet = new HashMap<>();
        ArrayList<CombinatorialNode> lst = new ArrayList<>();
        lst.add(graph.getRoot());
        mf2NodeSet.put(graph.getRoot().fragment.getFormula().withoutHydrogen(), lst);

        for(CombinatorialNode node : graph.getNodes()){
            MolecularFormula mf = node.fragment.getFormula().withoutHydrogen();
            mf2NodeSet.computeIfAbsent(mf, x -> new ArrayList<CombinatorialNode>()).add(node);
        }

        // 2. Iterate through the fragmentation tree 'fTree' and
        // for each vertex in 'fTree', add a terminal node into the CombinatorialGraph if
        // there are CombinatorialNodes in 'graph' which have the same molecular formula without hydrogen atoms.
        // Then connect this terminal node with these nodes.
        int count = 0;
        FragmentAnnotation<AnnotatedPeak> peakAno = fTree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        for(Fragment ftFrag : fTree){
            MolecularFormula mf = ftFrag.getFormula().withoutHydrogen();
            lst = mf2NodeSet.get(mf);
            if(lst != null){
                // in this case, there are nodes in 'graph' (and 'lst') with the same molecular formula base
                BitSet terminalNodeBitSet = toBitSet(count);
                terminalNodeBitSet.set(molecule.natoms); // terminal nodes are not real fragment --> this bit characterises them
                CombinatorialFragment terminalFragment = new CombinatorialFragment(molecule,terminalNodeBitSet,ftFrag.getFormula(),new BitSet(), false, (float)peakAno.get(ftFrag).getRelativeIntensity());

                for(CombinatorialNode node : lst){
                    graph.addReturnAlways(node, terminalFragment, null, null, scoring, null).state=1;
                    colorAllToRoot(node);
                }

                count++;
            }
        }
        // remove dangling subtrees

        final int size = graph.numberOfNodes();
        removeFromList(graph.nodes, x->x.state==0);
        for (CombinatorialNode node : graph.nodes) {
            removeFromList(node.outgoingEdges, x->x.target.state==0);
        }
        LoggerFactory.getLogger(CombinatorialGraphManipulator.class).warn("Remove "+(size-graph.numberOfNodes()) + " of " + graph.numberOfNodes() + " nodes");


    }
    public static <T> void removeFromList(ArrayList<T> xs, Predicate<T> f) {
        xs.removeIf(f); // super stupid implementation -_- shitty java
    }

    private static void colorAllToRoot(CombinatorialNode node) {
        node.state=1;
        ArrayList<CombinatorialNode> stack = new ArrayList<>();
        stack.add(node);
        while (!stack.isEmpty()) {
            CombinatorialNode fragment = stack.remove(stack.size()-1);
            for (CombinatorialEdge e : fragment.incomingEdges) {
                if (e.source.state == 0) {
                    e.source.state=1;
                    stack.add(e.source);
                }
            }
        }
    }
}
