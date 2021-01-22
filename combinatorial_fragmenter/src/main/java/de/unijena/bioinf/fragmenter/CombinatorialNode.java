package de.unijena.bioinf.fragmenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombinatorialNode {

    protected CombinatorialFragment fragment;
    protected List<CombinatorialEdge> incomingEdges, outgoingEdges;
    protected short depth, bondbreaks;
    protected float score, totalScore;
    protected byte state;

    public CombinatorialNode(CombinatorialFragment fragment) {
        this.fragment = fragment;
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.depth = Short.MAX_VALUE;
        this.bondbreaks = Short.MAX_VALUE;
        score=0f; totalScore=0f;
        state=0;
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
