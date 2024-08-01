/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
