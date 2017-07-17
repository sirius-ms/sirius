package de.unijena.bioinf.GibbsSampling.model;

import gnu.trove.list.array.TIntArrayList;

/**
 * Created by ge28quv on 10/05/17.
 */
public abstract class AbstractEdgeFilter implements EdgeFilter {

    protected int[][] makeWeightsSymmetricAndCreateConnectionsArray(Graph graph) {
        TIntArrayList[] connectionsList = new TIntArrayList[graph.getSize()];

        int connections;
        for(connections = 0; connections < graph.getSize(); ++connections) {
            connectionsList[connections] = new TIntArrayList(100);
        }

        int i;
        for(connections = 0; connections < graph.getSize(); ++connections) {
            for(i = connections + 1; i < graph.getSize(); ++i) {
                double w1 = graph.getLogWeight(connections, i);
                double w2 = graph.getLogWeight(i, connections);
                double max;
                if(w1 < w2) {
                    graph.setLogWeight(connections, i, w2);
                    max = w2;
                } else if(w2 < w1) {
                    graph.setLogWeight(i, connections, w1);
                    max = w1;
                } else {
                    max = w1;
                }

                if(max != 0.0D) {
                    connectionsList[connections].add(i);
                    connectionsList[i].add(connections);
                }
            }
        }

        int[][] var11 = new int[graph.getSize()][];

        for(i = 0; i < var11.length; ++i) {
            var11[i] = connectionsList[i].toArray();
        }

        return var11;
    }
}
