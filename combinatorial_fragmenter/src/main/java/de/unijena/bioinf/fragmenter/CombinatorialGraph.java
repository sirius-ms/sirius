package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.interfaces.IBond;

import java.util.*;

public class CombinatorialGraph {

    protected final List<CombinatorialNode> nodes;
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

    public CombinatorialNode addReturnNovel(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond) {
        return addReturnNovel(parent,fragment,firstBond,secondBond,EMPTY_SCORING);
    }
    private static CombinatorialFragmenterScoring EMPTY_SCORING = new CombinatorialFragmenterScoring() {
        @Override
        public double scoreBond(IBond bond, boolean direction) {
            return -1d;
        }

        @Override
        public double scoreFragment(CombinatorialFragment fragment) {
            return 0;
        }
    };

    public CombinatorialNode addReturnNovel(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond, CombinatorialFragmenterScoring scoring) {
        boolean novel = false;
        CombinatorialNode node = bitset2node.get(fragment.bitset);
        if (node == null) {
            node = new CombinatorialNode(fragment);
            bitset2node.put(fragment.bitset,node);
            nodes.add(node);
            novel = true;
            node.score = Float.NEGATIVE_INFINITY;
            node.totalScore = Float.NEGATIVE_INFINITY;

        }
        node.depth = (short)Math.min(node.depth, parent.depth+1);
        node.bondbreaks = (short)Math.min(node.bondbreaks, parent.bondbreaks + (secondBond==null ? 1 : 2));

        boolean cut1Direction =  ( fragment.bitset.get(firstBond.getAtom(0).getIndex()));
        boolean cut2Direction = secondBond != null && (fragment.bitset.get(secondBond.getAtom(0).getIndex()));

        float score = (float)(scoring.scoreBond(firstBond,cut1Direction)+ (secondBond!=null ? scoring.scoreBond(secondBond,cut2Direction) : 0f) + scoring.scoreFragment(node.fragment));
        float bestScore = (parent.totalScore + score);
        if (bestScore > node.totalScore) {
            node.totalScore = bestScore;
            node.score = score;
        }
        CombinatorialEdge edge = new CombinatorialEdge(parent, node, firstBond, secondBond, cut1Direction,cut2Direction);
        node.incomingEdges.add(edge);
        parent.outgoingEdges.add(edge);
        if (novel) return node;
        else return null;
    }
    public CombinatorialNode addReturnAlways(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond, CombinatorialFragmenterScoring scoring, boolean[] updateFlag) {
        boolean novel = false;
        CombinatorialNode node = bitset2node.get(fragment.bitset);
        if (node == null) {
            node = new CombinatorialNode(fragment);
            bitset2node.put(fragment.bitset,node);
            nodes.add(node);
            novel = true;
            node.score = Float.NEGATIVE_INFINITY;
            node.totalScore = Float.NEGATIVE_INFINITY;

        }
        node.depth = (short)Math.min(node.depth, parent.depth+1);
        node.bondbreaks = (short)Math.min(node.bondbreaks, parent.bondbreaks + (secondBond==null ? 1 : 2));
        boolean cut1Direction =  ( fragment.bitset.get(firstBond.getAtom(0).getIndex()));
        boolean cut2Direction = secondBond != null && (fragment.bitset.get(secondBond.getAtom(0).getIndex()));
        float score = (float)(scoring.scoreBond(firstBond,cut1Direction)+(secondBond!=null ? scoring.scoreBond(secondBond,cut2Direction) : 0f) + scoring.scoreFragment(node.fragment));
        float bestScore = (parent.totalScore + score);
        if (bestScore > node.totalScore) {
            node.totalScore = bestScore;
            node.score = score;
            updateFlag[0] = true;
        } else updateFlag[0] = false;

        CombinatorialEdge edge = new CombinatorialEdge(parent, node, firstBond, secondBond,cut1Direction,cut2Direction);
        node.incomingEdges.add(edge);
        parent.outgoingEdges.add(edge);
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

    public List<CombinatorialNode> getNodes() {
        return nodes;
    }

    public CombinatorialNode getRoot() {
        return root;
    }
}
