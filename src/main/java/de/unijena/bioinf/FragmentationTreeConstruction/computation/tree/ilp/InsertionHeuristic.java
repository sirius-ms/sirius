package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.FragmentationTreeConstruction.model.*;

import java.util.ArrayList;
import java.util.List;

public class InsertionHeuristic {

    protected final FragmentationTree tree;
    protected final FragmentationGraph graph;
    protected final ProcessedInput input;
    protected final double lowerbound;
    protected final List<GraphFragment>[] fragments;
    protected final List<TreeFragment> nodes;
    protected int usedColors;

    public InsertionHeuristic(ProcessedInput input, FragmentationGraph graph, double lowerbound) {
        this(input, graph, new FragmentationTree(0d, graph), lowerbound);
    }

    public InsertionHeuristic(ProcessedInput input, FragmentationGraph graph, FragmentationTree backbone, double lowerbound) {
        this.tree = backbone;
        this.graph = graph;
        this.input = input;
        this.lowerbound = lowerbound;
        this.usedColors = backbone.numberOfColors();
        assert backboneContainsOnlyUsedColors();
        this.fragments = new ArrayList[graph.numberOfColors()-usedColors];
        for (int i=0; i < fragments.length; ++i) {
            fragments[i] = new ArrayList<GraphFragment>();
        }
        for (GraphFragment f : graph.getFragments()) {
            if (f.getColor() >= usedColors)
                fragments[f.getColor()-usedColors].add(f);
        }
        this.nodes = new ArrayList<TreeFragment>();
        nodes.addAll(backbone.getFragments());
    }

    public void run() {
        for (int c = usedColors; c < graph.numberOfColors(); ++c) {
            insertColor(c);
        }
    }

    protected void insertColor(int c) {
        GraphFragment bestCandidate = null;
        for (final GraphFragment v : fragments[c-usedColors]) {
            TreeFragment toAttach = null;
            for (TreeFragment u : nodes) {

            }
        }
    }

    private boolean backboneContainsOnlyUsedColors() {
        for (TreeFragment f : tree.getFragments()) {
            if (f.getColor() >= usedColors) return false;
        }
        return true;
    }

}
