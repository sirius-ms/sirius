package de.unijena.bioinf.FragmentationTreeConstruction.model.graph;

import java.util.AbstractList;

class EdgeList extends AbstractList<FTEdge> {

    private final FTEdge[] edges;
    private final int size;

    EdgeList(FTEdge[] edges) {
        this.edges = edges;
        int n=edges.length;
        while (n>0 && edges[n-1]==null) --n;
        this.size = n;

    }

    @Override
    public FTEdge get(int index) {
        if (index < size) return edges[size];
        else throw new IndexOutOfBoundsException("Index " + index + " out of bound " + size);
    }

    @Override
    public int size() {
        return size;
    }
}
