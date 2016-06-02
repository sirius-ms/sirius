/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.GraphReduction;

/**
 * Created by Spectar on 05.11.2014.
 */
public class TMinimalController implements GraphReduction {

    @Override
    public FGraph reduce(FGraph graph, double lowerbound) {

        graph.sortTopological();


        TReduce reduceInstance = new TReduce(graph);

        reduceInstance.DoCheckVerticesAreTopSorted("FOO");

        // this applies to the following reduction code:
        // enable-seb-vub-strength tim-vertex-ubs reduce-vub * ( clear-vertex-ubs seb-vertex-ubs tim-vertex-ubs reduce-vub reduce-unreach )
        reduceInstance.gShouldStrengthenSebVertexUbs = true;
        reduceInstance.doTimVertexUpperBounds();
        reduceInstance.reduceEdgesByVertexUpperBound();

        boolean hasDeletedLastTime; // more human readable

        do {
            hasDeletedLastTime = false;

            reduceInstance.clearVertexUpperBounds(Double.POSITIVE_INFINITY);
            reduceInstance.doSebastianVertexUpperBounds();
            reduceInstance.doTimVertexUpperBounds();
            hasDeletedLastTime |= reduceInstance.reduceEdgesByVertexUpperBound();
            hasDeletedLastTime |= reduceInstance.reduceUnreachableEdges();
        } while (hasDeletedLastTime);

        return reduceInstance.getGraph();
    }
}
