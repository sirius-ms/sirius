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

    @Override
    public void command(String cmd) {

    }
}
