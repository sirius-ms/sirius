
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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
