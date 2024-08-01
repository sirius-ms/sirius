package de.unijena.bioinf.fragmenter;

import java.util.*;
import java.util.stream.Collectors;

public class CombinatorialNode {

    protected final CombinatorialFragment fragment;
    protected final ArrayList<CombinatorialEdge> incomingEdges, outgoingEdges;

    /**
     * The depth of this node; i.e. the minimal number of edges from this node to the root.
     */
    protected short depth;
    /**
     * The minimal number of bonds which have to break in order to get this fragment.
     */
    protected short bondbreaks;
    /**
     * This is the score or profit of the corresponding fragment.
     */
    protected float fragmentScore;
    /**
     * This score refers to {@link CombinatorialNode#totalScore}.
     * It is the sum of the fragment score plus the score of the edge which belongs to the most profitable path
     * from this node to the root.
     */
    protected float score;
    /**
     * This is the score of the path from root to this node which has the maximal score or "profit".
     * The score of a path is equal to the sum of scores of its contained fragments and edges.
     */
    protected float totalScore;

    protected byte state;

    public CombinatorialNode(CombinatorialFragment fragment) {
        this.fragment = fragment;
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.depth = Short.MAX_VALUE;
        this.bondbreaks = Short.MAX_VALUE;
        this.fragmentScore = 0f;
        this.score=0f;
        this.totalScore=0f;
        this.state=0;
    }

    public String toSmarts() {
        return fragment.toSMARTS(Collections.emptySet());
    }
    public String invertSmarts() {
        return fragment.toSMARTSLoss(Collections.emptySet());
    }

    public String pathToSmarts() {
        return fragment.toSMARTS(getOptimalPathToRoot().stream().flatMap(x-> Arrays.stream(x.getCuts())).collect(Collectors.toSet()));
    }
    public String pathToInvertedSmarts() {
        return fragment.toSMARTSLoss(getOptimalPathToRoot().stream().flatMap(x-> Arrays.stream(x.getCuts())).collect(Collectors.toSet()));
    }

    public List<CombinatorialEdge> getOptimalPathToRoot() {
        List<CombinatorialEdge> path = new ArrayList<>();
        CombinatorialNode n = this;
        while (n.getIncomingEdges().size()>0) {
            double maxScore = Double.NEGATIVE_INFINITY;
            CombinatorialEdge best = null;
            for (CombinatorialEdge e : n.incomingEdges) {
                if (e.source.totalScore > maxScore) {
                    best=e;
                    maxScore = best.source.totalScore;
                }
            }
            path.add(best);
            n = best.source;
        }
        Collections.reverse(path);
        return path;
    }

    public CombinatorialFragment getFragment() {
        return fragment;
    }

    public List<CombinatorialEdge> getIncomingEdges() {
        return incomingEdges;
    }

    public List<CombinatorialEdge> getOutgoingEdges() {
        return outgoingEdges;
    }

    public int getDepth() {
        return depth;
    }

    public short getBondbreaks() {
        return bondbreaks;
    }

    public void setState(byte newState){
        this.state = newState;
    }

    public byte getState() {
        return state;
    }

    @Override
    public String toString() {
        return fragment.getFormula() + " (" + bondbreaks + " bond breaks)";
    }

    public float getFragmentScore(){
        return this.fragmentScore;
    }

    public float getScore() {
        return score;
    }

    public float getTotalScore() {
        return totalScore;
    }

    public Optional<CombinatorialNode> getTerminalChildNode() {
        for (CombinatorialEdge e : outgoingEdges) {
            if (!e.target.fragment.isInnerNode()) return Optional.of(e.target);
        }
        return Optional.empty();
    }
}
