package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree;

import de.unijena.bioinf.FragmentationTreeConstruction.model.GraphFragment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.TreeFragment;

class TraceItem {

    final double accumulatedWeight;
    final GraphFragment vertex;
    final TreeFragment treeNode;
    final int bitset;

    public TraceItem(GraphFragment vertex, TreeFragment treeNode, int bitset, double accumulatedWeight) {
        this.accumulatedWeight = accumulatedWeight;
        this.treeNode = treeNode;
        this.vertex = vertex;
        this.bitset = bitset;
    }

}
