package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

/**
 *  Utility class to edit a CombinatorialGraph object.
 */
public class CombinatorialGraphManipulator {

    private static BitSet increment(BitSet bitSet){
        BitSet newBitSet = (BitSet) bitSet.clone();
        int idx = 0;

        while(newBitSet.get(idx)){
            newBitSet.set(idx, false);
            idx++;
        }
        newBitSet.set(idx, true);
        return newBitSet;
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
        BitSet terminalNodeBitSet = new BitSet();
        terminalNodeBitSet.set(molecule.natoms); // terminal nodes are not real fragments --> this bit characterises them

        for(Fragment ftFrag : fTree){
            MolecularFormula mf = ftFrag.getFormula().withoutHydrogen();
            lst = mf2NodeSet.get(mf);
            if(lst != null){
                // in this case, there are nodes in 'graph' (and 'lst') with the same molecular formula base
                CombinatorialFragment terminalFragment = new CombinatorialFragment(molecule,terminalNodeBitSet,ftFrag.getFormula(),new BitSet());

                for(CombinatorialNode node : lst){
                    graph.addReturnAlways(node, terminalFragment, null, null, scoring, null);
                }

                terminalNodeBitSet = increment(terminalNodeBitSet);
            }
        }
    }

    public static void removeAllNodesNotConnectedToTerminalNodes(CombinatorialGraph graph){
        // INITIALISATION:
        // First create a hashmap which assigns each node in graph an unique index:
        final TObjectIntHashMap<CombinatorialNode> node2Index = new TObjectIntHashMap<>(graph.numberOfNodes());
        node2Index.put(graph.getRoot(), 0);

        int idx = 1;
        for(CombinatorialNode node : graph.getNodes()){
            node2Index.put(node, idx);
            idx++;
        }

        // ASSIGNING BOOLEAN STATES:
        // Assign each node a state equals 'true' if there is a path from this node to a terminal node,
        // otherwise, assign 'false' to this node:
        boolean[] states = new boolean[graph.numberOfNodes()];
        for(CombinatorialNode terminalNode : graph.getTerminalNodes()){
            colorAllToRoot(terminalNode, states, node2Index);
        }

        // REMOVE ALL NODES WHOSE STATE IS 'FALSE':
        // Iterate over all nodes in 'graph.getNodes()' and request their state/colour.
        // If the state of a node is still 'false', we know it's not connected to a terminal node.
        // In this case, remove this node.
        ArrayList<CombinatorialNode> nodesWithoutRoot = new ArrayList<>(graph.getNodes());
        for(CombinatorialNode node : nodesWithoutRoot){
            int nodeIdx = node2Index.get(node);
            if(!states[nodeIdx]){
                graph.deleteNodeDangerously(node);
            }
        }
    }

    private static void colorAllToRoot(CombinatorialNode node, boolean[] states, TObjectIntHashMap<CombinatorialNode> node2Index){
        // set the state of 'node' to true:
        int nodeIdx = node2Index.get(node);
        states[nodeIdx] = true;

        // 'stack' contains only nodes which are connected with 'node':
        ArrayList<CombinatorialNode> stack = new ArrayList<>();
        stack.add(node);

        // Do the following for each node in 'stack':
        // - poll 'currentNode' from the stack -> 'currentNode' is connected with 'node'
        // - each parent node of 'currentNode' is connected to 'node'
        // - if the parent node is not coloured 'true', colour it 'true' and add it to the stack
        // - if it's already coloured to 'true', there are two cases:
        //      case 1: this node was already processed
        //      case 2: this node was not processed yet, but it's contained in the stack and will be processed
        while(!stack.isEmpty()){
            CombinatorialNode currentNode = stack.remove(stack.size() - 1);

            for(CombinatorialEdge edge : currentNode.incomingEdges){
                CombinatorialNode parentNode = edge.source;
                int parentNodeIdx = node2Index.get(parentNode);
                if(!states[parentNodeIdx]){
                    states[parentNodeIdx] = true;
                    stack.add(parentNode);
                }
            }
        }
    }
}
