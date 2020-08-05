
package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;

public interface GraphReduction {

    /**
     * Either return a new graph or modify and return the given input graph.
     * Remove Edges and Fragments from the graph for which it can be guaranteed that they won't be
     * part of the optimal solution.
     * Lowerbound is the minimal score of the optimal solution. If the optimal solution of the graph is worse
     * than the LP_LOWERBOUND, there is no solution for the problem. This means that it is also allowed to remove
     * fragments and edges if it can be guaranteed that they won't be part of any solution that is better than
     * the LP_LOWERBOUND. It's also valid to delete the whole graph if no possible solution satisfies the LP_LOWERBOUND.
     *
     * @param graph
     * @param lowerbound
     * @return
     */
    FGraph reduce(FGraph graph, double lowerbound);
}
