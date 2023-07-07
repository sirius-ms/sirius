package de.unijena.bioinf.utils;/*
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

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.List;

/**
 * This implementation does not use any special data structures and is useful  for spanning trees on complete graphs
 */
public class PrimsSpanningTree<T> {

    final double[][] distances;
    final boolean negateWeights;
    private int root;

    public PrimsSpanningTree(double[][] distances, boolean maximize) {
        this.distances = distances;
        this.negateWeights = maximize;
    }

    public List<int[]> computeSpanningTree() {
        root = selectRoot();
        double[] costs = new double[distances.length];
        int[] incomingEdges = new int[distances.length];

        for (int i = 0; i < costs.length; i++) {
            costs[i] = Double.POSITIVE_INFINITY;
            incomingEdges[i] = -1;
        }

        IntSet queue = new IntOpenHashSet();
        for (int i = 0; i < distances.length; i++) {
            if (i != root) queue.add(i);
        }

        List<int[]> edges = new ArrayList<>();
        int lastVertex = root;
        while (queue.size()>0) {
            lastVertex = update(edges, lastVertex, costs, incomingEdges, queue);
        }
        return edges;
    }

    public int getRoot() {
        return root;
    }

    //
    private int update(List<int[]> edges, int lastVertex, double[] costs, int[] incomingEdges, IntSet queue) {
        final int[] newMin = new int[]{-1};
        final double[] minDist = new double[]{Double.POSITIVE_INFINITY};
        queue.forEach(i -> {
            if (i==lastVertex) return;

            double neighbourDist = getDistance(lastVertex,i);
            if (costs[i]>neighbourDist){
                costs[i] = neighbourDist;
                incomingEdges[i] = lastVertex;
            }
            if (costs[i] < minDist[0]) {
                newMin[0] = i;
                minDist[0] = costs[i];
            }
        });

        int parent = incomingEdges[newMin[0]];
        if (parent<0) throw new RuntimeException("could not find connecting node in spanning tree");
        edges.add(new int[]{parent, newMin[0]});
        queue.remove(newMin[0]);
        return newMin[0];
    }

    private int selectRoot() {
        //root, minimum summed dist to all others
        //assumes complete distance matrix, not just upper triangle
        double minDist = Double.MAX_VALUE;
        int root = -1;

        for (int i = 0; i < distances.length; i++) {
            final double[] row = distances[i];
            double s = sum(row)-row[i]; //minus diagonal
            if (negateWeights) s = -s;
            if (s<minDist){
                root = i;
                minDist = s;
            }
        }
        return root;
    }

    private double getDistance(int i, int j) {
        return (negateWeights ? -distances[i][j] : distances[i][j]);
    }

    private double sum(double[] array){
        double sum = 0d;
        for (double d : array) {
            sum += d;
        }
        return sum;
    }


}
