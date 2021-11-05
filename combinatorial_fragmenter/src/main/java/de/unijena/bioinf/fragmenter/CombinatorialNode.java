package de.unijena.bioinf.fragmenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombinatorialNode {

    protected CombinatorialFragment fragment;
    protected List<CombinatorialEdge> incomingEdges, outgoingEdges;

    /**
     * The depth of this node; i.e. the minimal number of edges from this node to the root.
     */
    protected short depth;
    /**
     * The minimal number of bonds which have to break in order to get this fragment.
     */
    protected short bondbreaks;
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
        this.score=0f;
        this.totalScore=0f;
        this.state=0;
    }

    public List<CombinatorialEdge> getOptimalPathToRoot() {
        List<CombinatorialEdge> path = new ArrayList<>();
        CombinatorialNode n = this;
        while (n.depth>0) {
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

    @Override
    public String toString() {
        return fragment.getFormula() + " (" + bondbreaks + " bond breaks)";
    }

    public float getScore() {
        return score;
    }

    public float getTotalScore() {
        return totalScore;
    }
}
