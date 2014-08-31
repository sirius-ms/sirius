package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;

public interface GraphReduction {

    /**
     * Either return a new graph or modify and return the given input graph.
     * Remove Edges and Fragments from the graph for which it can be guaranteed that they won't be
     * part of the optimal solution.
     * Lowerbound is the minimal score of the optimal solution. If the optimal solution of the graph is worse
     * than the lowerbound, there is no solution for the problem. This means that it is also allowed to remove
     * fragments and edges if it can be guaranteed that they won't be part of any solution that is better than
     * the lowerbound. It's also valid to delete the whole graph if no possible solution satisfies the lowerbound.
     *
     * @param graph
     * @param lowerbound
     * @return
     */
    public FGraph reduce(FGraph graph, double lowerbound);

    public void command( String cmd );
}
