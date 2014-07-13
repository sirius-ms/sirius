package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree;

import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

class TraceItem {

    final double accumulatedWeight;
    final Fragment vertex;
    final Fragment treeNode;
    final int bitset;

    public TraceItem(Fragment vertex, Fragment treeNode, int bitset, double accumulatedWeight) {
        this.accumulatedWeight = accumulatedWeight;
        this.treeNode = treeNode;
        this.vertex = vertex;
        this.bitset = bitset;
    }

}
