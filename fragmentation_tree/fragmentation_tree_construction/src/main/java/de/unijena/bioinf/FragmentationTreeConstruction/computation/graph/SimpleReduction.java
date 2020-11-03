
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
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TDoubleProcedure;
import gnu.trove.procedure.TIntDoubleProcedure;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by kaidu on 29.04.2015.
 */
public class SimpleReduction implements GraphReduction {

    @Override
    public FGraph reduce(FGraph graph, double lowerbound) {
        final int N = graph.numberOfEdges();
        final double[] upperbounds = new double[graph.numberOfVertices()];
        final TIntDoubleHashMap2[] scoresPerColor = new TIntDoubleHashMap2[graph.numberOfVertices()];
        for (Fragment u : graph) {
            scoresPerColor[u.getVertexId()] = new TIntDoubleHashMap2(Math.min(graph.maxColor(), u.getOutDegree()));
        }
        int deletedEdges;
        do {
            updateUpperbounds(graph, upperbounds, scoresPerColor);
            deletedEdges = deleteEdges(graph, upperbounds);
        } while (deletedEdges>0);
        for (Fragment f : graph) {
            f.compact();
        }

        return graph;
    }

    private int deleteEdges(FGraph graph, double[] upperbounds) {
        final int N = graph.numberOfVertices();
        int counter=0;
        final ArrayList<Loss> toDelete = new ArrayList<Loss>();
        final ArrayList<Fragment> deleteFragments = new ArrayList<Fragment>();
        for (int k=N-1; k > 0; --k) {
            final Fragment u = graph.getFragmentAt(k);
            final int n = u.getOutDegree();
            for (int i=0; i < n; ++i) {
                final Loss l = u.getOutgoingEdge(i);
                if (l.getWeight() + upperbounds[l.getTarget().getVertexId()] < 0) {
                    toDelete.add(l);
                }
            }
            counter += toDelete.size();
            for (Loss l : toDelete) graph.deleteLoss(l);
            toDelete.clear();
            if (u.getInDegree()==0) deleteFragments.add(u);
        }
        for (Fragment f : deleteFragments) {
            counter += f.getOutDegree();
        }
        graph.deleteFragmentsKeepTopologicalOrder(deleteFragments);
        return counter;
    }

    private void updateUpperbounds(final FGraph graph, final double[] upperbounds, TIntDoubleHashMap2[] scoresPerColor) {
        Arrays.fill(upperbounds, Double.MAX_VALUE);

        final int N = graph.numberOfVertices();
        for (int k=N-1; k >= 0; --k) {
            final Fragment u = graph.getFragmentAt(k);
            final TIntDoubleHashMap2 colors = scoresPerColor[k];
            colors.clear();
            final int n = u.getOutDegree();

            for (int l=0; l < n; ++l) {
                final Loss uv = u.getOutgoingEdge(l);
                final Fragment v = uv.getTarget();
                final double score = Math.max(0, upperbounds[v.getVertexId()] + uv.getWeight());
                if (score > 0) colors.putIfGreater(v.getColor(), score);
            }
            updateUpperbound(k, upperbounds, colors);

            colors.clear();
            for (int l=0; l < n; ++l) {
                final Loss uv = u.getOutgoingEdge(l);
                assert uv.getTarget().getVertexId() > k;
                // merge color maps
                final TIntDoubleHashMap2 childColors = scoresPerColor[uv.getTarget().getVertexId()];
                colors.putIfGreater(uv.getTarget().getColor(), uv.getWeight());
                childColors.forEachEntry(new TIntDoubleProcedure() {
                    @Override
                    public boolean execute(int a, double b) {
                        colors.putIfGreater(a, b);
                        return true;
                    }
                });
            }
            updateUpperbound(k, upperbounds, colors);

        }

    }

    private void updateUpperbound(final int k, final double[] upperbounds, TIntDoubleHashMap2 colors) {
        final double oldUpperbound = upperbounds[k];
        upperbounds[k] = 0;
        colors.forEachValue(new TDoubleProcedure() {
            @Override
            public boolean execute(double value) {
                assert value > 0;
                upperbounds[k] += value;
                return true;
            }
        });
        upperbounds[k] = Math.min(Math.max(0, upperbounds[k]), oldUpperbound);
    }

    private static class TIntDoubleHashMap2 extends TIntDoubleHashMap {
        public TIntDoubleHashMap2(int capa) {
            super(capa, TIntDoubleHashMap.DEFAULT_LOAD_FACTOR, -1, 0d);
        }

        boolean putIfGreater(int key, double score) {
            if (score <= no_entry_value)
                return false;
            int index = insertKey( key );
            if (index < 0) {
                final int k = -index - 1;
                if (_values[k] >= score) {
                    return false;
                }
                _values[k] = score;
                return true;
            } else {
                doPut(key, score, index);
                return true;
            }
        }
        private double doPut( int key, double value, int index ) {
            double previous = no_entry_value;
            boolean isNewMapping = true;
            if ( index < 0 ) {
                index = -index -1;
                previous = _values[index];
                isNewMapping = false;
            }
            _values[index] = value;

            if (isNewMapping) {
                postInsertHook( consumeFreeSlot );
            }

            return previous;
        }
    }

}
