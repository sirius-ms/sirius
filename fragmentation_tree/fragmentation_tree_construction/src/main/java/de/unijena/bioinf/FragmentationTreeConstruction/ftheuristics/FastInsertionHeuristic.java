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

package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;
import java.util.BitSet;

public class FastInsertionHeuristic extends AbstractHeuristic {

    protected final double[] maxIn, maxOut;
    protected final Loss[] maxInVertices;
    private final BitSet usedColors;
    private final TIntObjectHashMap<Loss> color2Edge;

    private final int[] vertices2consider;
    private int nv;

    public FastInsertionHeuristic(FGraph graph) {
        super(graph);
        usedColors = new BitSet(ncolors);
        color2Edge = new TIntObjectHashMap<>(ncolors, 0.75f, -1);
        this.maxIn = new double[graph.numberOfVertices()];
        this.maxOut = new double[graph.numberOfVertices()];
        this.maxInVertices = new Loss[graph.numberOfVertices()];
        this.vertices2consider = new int[graph.numberOfVertices()];
    }

    @Override
    public FTree solve() {
        if (graph.getRoot().getOutDegree() > 1)
            throw new RuntimeException("Algorithm is optimized for graphs with pseudo root outdegree = 1");
        extend(graph.getRoot().getOutgoingEdge(0));
        compute();
        selectedEdges.addAll(color2Edge.valueCollection());
        return buildSolution(true);
    }

    private void extend(Loss l) {
        usedColors.set(l.getTarget().getColor());
        color2Edge.put(l.getTarget().getColor(), l);
    }

    private void compute() {
        initialize();
        while (true) {
            double maxScore = Double.NEGATIVE_INFINITY;
            Loss maxLoss = null;
            // find vertex with max score
            int k=0;
            for (int j = 0; j < nv; ++j) {
                final int i = vertices2consider[j];
                if (usedColors.get(graph.getFragmentAt(i).getColor())) {
                    maxIn[i] = Double.NaN;
                } else {
                    final double score = maxIn[i] + maxOut[i];
                    if (score > maxScore) {
                        maxScore = score;
                        maxLoss = maxInVertices[i];
                    }
                    vertices2consider[k++] = i;
                }
            }
            this.nv = k;
            if (maxLoss == null)
                break;
            insert(maxLoss);
        }
    }

    private void insert(Loss maxLoss) {
        final Fragment newVertex = maxLoss.getTarget();
        usedColors.set(newVertex.getColor());
        color2Edge.put(newVertex.getColor(), maxLoss);
        // relocate and update
        for (int i = 0, n = newVertex.getOutDegree(); i < n; ++i) {
            final Loss l = newVertex.getOutgoingEdge(i);
            final Fragment w = l.getTarget();
            final Loss xw = color2Edge.get(w.getColor());
            if (xw != null) {
                if (xw.getTarget() == w && xw.getWeight() < l.getWeight()) {
                    color2Edge.put(w.getColor(), l);
                    for (int j = 0, m = w.getInDegree(); j < m; ++j) {
                        final Loss zw = w.getIncomingEdge(j);
                        final Fragment z = zw.getSource();
                        final int zid = z.getVertexId();
                        if (!Double.isNaN(maxIn[zid])) {
                            maxOut[zid] = Math.max(
                                    0,
                                    maxOut[zid] + xw.getWeight() - l.getWeight()
                            );
                        }
                    }
                }
            } else {
                final int wid = w.getVertexId();
                if (maxIn[wid] < l.getWeight()) {
                    maxIn[wid] = l.getWeight();
                    maxInVertices[wid] = l;
                }
            }
        }
        for (int i=0, n = newVertex.getInDegree(); i < n; ++i) {
            final Loss yv = newVertex.getIncomingEdge(i);
            if (yv.getWeight() > maxLoss.getWeight()) {
                int y = yv.getSource().getVertexId();
                maxOut[y] += yv.getWeight()-maxLoss.getWeight();
            }
        }

    }

    private void initialize() {
        Arrays.fill(maxIn, Double.NEGATIVE_INFINITY);
        final Loss rootLoss = graph.getRoot().getOutgoingEdge(0);
        final Fragment root = rootLoss.getTarget();
        for (int i = 0, n = root.getOutDegree(); i < n; ++i) {
            final Loss uv = root.getOutgoingEdge(i);
            final int vertexId = uv.getTarget().getVertexId();
            maxIn[vertexId] = uv.getWeight();
            maxInVertices[vertexId] = uv;
        }
        maxIn[root.getVertexId()] = Double.NaN;
        maxOut[root.getVertexId()] = Double.NaN;
        maxIn[graph.getRoot().getVertexId()] = maxOut[graph.getRoot().getVertexId()] = Double.NaN;
        int k=0;
        for (int i=0; i < graph.numberOfVertices(); ++i) {
            vertices2consider[k++] = i;
        }
        this.nv = k;

    }

}
