package de.unijena.bioinf.GibbsSampling.model;

import gnu.trove.list.array.TIntArrayList;

/**
 * Created by ge28quv on 10/05/17.
 */
public abstract class AbstractEdgeFilter implements EdgeFilter {

    protected int[][] makeWeightsSymmetricAndCreateConnectionsArray(Graph graph) {
        TIntArrayList[] connectionsList = new TIntArrayList[graph.getSize()];

        for(int i = 0; i < graph.getSize(); ++i) {
            connectionsList[i] = new TIntArrayList(100);
        }

        //todo easily speed up by going over all graph.getLogWeightConnections
        for(int i = 0; i < graph.getSize(); ++i) {
            for(int j = i + 1; j < graph.getSize(); ++j) {
                double w1 = graph.getLogWeight(i, j);
                double w2 = graph.getLogWeight(j, i);
                double max;
                if(w1 < w2) {
                    graph.setLogWeight(i, j, w2);
                    max = w2;
                } else if(w2 < w1) {
                    graph.setLogWeight(j, i, w1);
                    max = w1;
                } else {
                    max = w1;
                }

//                if(max != 0.0D) { //changed
                if(max > 0.0D) {
                    connectionsList[i].add(j);
                    connectionsList[j].add(i);
                } else if (max < 0d) {
                throw new RuntimeException("Edge has a negative weight");
            }
            }
        }

        int[][] connections = new int[graph.getSize()][];

        for(int j = 0; j < connections.length; ++j) {
            connections[j] = connectionsList[j].toArray();
        }

        return connections;
    }
}
