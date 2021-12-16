package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.interfaces.IBond;

import java.util.*;

public class CombinatorialGraph {

    protected final List<CombinatorialNode> nodes; //alle Knoten außer der Wurzel
    protected final CombinatorialNode root;
    protected final HashMap<BitSet, CombinatorialNode> bitset2node;

    public CombinatorialGraph(MolecularGraph graph) {
        this.root = new CombinatorialNode(graph.asFragment());
        root.score =0;
        root.depth = 0;
        root.bondbreaks = 0;
        this.nodes = new ArrayList<>();
        this.bitset2node = new HashMap<>();
        bitset2node.put(root.fragment.bitset, root);
    }

    public CombinatorialNode addReturnNovel(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond, CombinatorialFragmenterScoring scoring) {
        boolean novel = (bitset2node.get(fragment.bitset) == null);
        CombinatorialNode node = this.addReturnAlways(parent, fragment,firstBond,secondBond,scoring,null);
        if(novel){
            return node;
        }else{
            return null;
        }
    }

    public CombinatorialNode addReturnAlways(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond, CombinatorialFragmenterScoring scoring, boolean[] updateFlag) {
        CombinatorialNode node = bitset2node.get(fragment.bitset);
        if (node == null) {
            node = new CombinatorialNode(fragment);
            bitset2node.put(fragment.bitset,node);
            nodes.add(node);
            node.fragmentScore = Float.NEGATIVE_INFINITY;
            node.score = Float.NEGATIVE_INFINITY;
            node.totalScore = Float.NEGATIVE_INFINITY;
        }

        node.depth = (short)Math.min(node.depth, parent.depth+1);
        node.bondbreaks = (short)Math.min(node.bondbreaks, parent.bondbreaks + (firstBond != null ? 1 : 0) + (secondBond !=null ? 1 : 0));

        boolean cut1Direction = firstBond != null && (fragment.bitset.get(firstBond.getAtom(0).getIndex()));
        boolean cut2Direction = secondBond != null && (fragment.bitset.get(secondBond.getAtom(0).getIndex()));

        CombinatorialEdge edge = new  CombinatorialEdge(parent, node, firstBond, secondBond,cut1Direction,cut2Direction);
        node.incomingEdges.add(edge);
        parent.outgoingEdges.add(edge);

        edge.score = (float) scoring.scoreEdge(edge);
        node.fragmentScore = (float) scoring.scoreFragment(node);
        float score = node.fragmentScore + edge.score;
        float bestScore = (parent.totalScore + score);

        if (bestScore > node.totalScore) {
            node.totalScore = bestScore;
            node.score = score;
            if(updateFlag != null && updateFlag.length > 0) updateFlag[0] = true;
        } else {
            if(updateFlag != null && updateFlag.length > 0) updateFlag[0] = false;
        }
        return node;
    }

    /**
     * for each node, allow only paths back to root which have minimal distance
     */
    public void pruneLongerPaths() {
        for (CombinatorialNode node : nodes) {
            final int min = node.incomingEdges.stream().mapToInt(x->x.source.bondbreaks).min().orElse(0);
            node.incomingEdges.removeIf(combinatorialEdge -> combinatorialEdge.source.bondbreaks > min);
        }
    }

    /*
    public CombinatorialEdge addBondCut(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond) {
        return addRingCut(parent,fragment,firstBond,null);
    }

    public CombinatorialEdge addRingCut(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond) {

        CombinatorialNode node = bitset2node.get(fragment.bitset);
        if (node == null) {
            node = new CombinatorialNode(fragment);
            bitset2node.put(fragment.bitset,node);
        }
        node.depth = (short)Math.min(node.depth, parent.depth+1);
        node.bondbreaks = (short)Math.min(node.bondbreaks, parent.bondbreaks+2);
        boolean cut1Direction =  ( fragment.bitset.get(firstBond.getAtom(0).getIndex()));
        boolean cut2Direction = secondBond != null && (fragment.bitset.get(secondBond.getAtom(0).getIndex()));
        CombinatorialEdge edge = new CombinatorialEdge(parent, node, firstBond, secondBond,cut1Direction,cut2Direction);
        node.incomingEdges.add(edge);
        parent.outgoingEdges.add(edge);
        return edge;
    }
    */

    public List<CombinatorialNode> getNodes() {
        return nodes;
    }

    public CombinatorialNode getRoot() {
        return root;
    }
}
