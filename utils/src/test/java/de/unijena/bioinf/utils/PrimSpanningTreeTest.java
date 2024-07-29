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

import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.fail;

public class PrimSpanningTreeTest {

    @Test
    public void compareToJGraphTRandomWeightsMultipleRuns() {
        int rounds = 10;
        int failed = 0;
        for (int i = 0; i < rounds; i++) {
            boolean succeeded = compareToJGraphTRandomWeights(100,false);
            if (!succeeded) failed++;
        }
        if (failed>1) fail("spanning tree differed more than ones from Kruskal implementation");;

        failed = 0;
        for (int i = 0; i < rounds; i++) {
            boolean succeeded = compareToJGraphTRandomWeights(100,true);
            if (!succeeded) failed++;
        }
        if (failed>1) fail("spanning tree differed more than ones from Kruskal implementation when negating weights");;


    }

    private boolean compareToJGraphTRandomWeights(int numberNodes, boolean negateWeights) {
        Random random = new Random();
        double[][] dist = new double[numberNodes][numberNodes];
        for (int i = 0; i < dist.length; i++) {
            for (int j = i+1; j < dist.length; j++) {
                dist[j][i] = dist[i][j] = random.nextDouble();
            }
        }


        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<Integer, DefaultWeightedEdge>(DefaultWeightedEdge.class);

        for (int i = 0; i < dist.length; i++) {
            graph.addVertex(i);
        }
        for (int i = 0; i < dist.length; i++) {
            for (int j = i+1; j < dist.length; j++) {
                DefaultWeightedEdge e = graph.addEdge(i,j);
                final double weight = negateWeights ? -dist[i][j] : dist[i][j];
                graph.setEdgeWeight(e, weight);
            }
        }


        KruskalMinimumSpanningTree<Integer, DefaultWeightedEdge> kruskal = new KruskalMinimumSpanningTree<>(graph);

        SpanningTreeAlgorithm.SpanningTree<DefaultWeightedEdge> spanningTree = kruskal.getSpanningTree();





        Set<DefaultWeightedEdge> treeEdges = spanningTree.getEdges();

        Set<Edge> kruskalEdges = new HashSet<>();
        for (DefaultWeightedEdge edge : treeEdges) {
            final int s = graph.getEdgeSource(edge);
            final int t = graph.getEdgeTarget(edge);
//            System.out.println(s+"-"+t);
            kruskalEdges.add(new Edge(s,t));
        }


        PrimsSpanningTree<Integer> primsSpanningTree= new PrimsSpanningTree<>(dist, negateWeights);

        List<int[]> edges = primsSpanningTree.computeSpanningTree();

        Set<Edge> primsEdges = new HashSet<>();
        for (int[] edge : edges) {
//            System.out.println(edge[0]+"--"+edge[1]);
            primsEdges.add(new Edge(edge[0], edge[1]));
        }

        return kruskalEdges.equals(primsEdges);
    }


    private class Edge {
        final int a;
        final int b;

        private Edge(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(a)+Integer.hashCode(b);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Edge)) return false;
            Edge e = (Edge)obj;
            if (e.a==a && e.b==b) return true;
            if (e.a==b && e.b==a) return true;
            return false;
        }

        @Override
        public String toString() {
            return String.valueOf(Math.min(a,b))+"--"+String.valueOf(Math.max(a,b));
        }
    }


}
