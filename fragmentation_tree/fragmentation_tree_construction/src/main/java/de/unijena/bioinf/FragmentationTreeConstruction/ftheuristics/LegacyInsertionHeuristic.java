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

public class LegacyInsertionHeuristic extends AbstractHeuristic{

    public LegacyInsertionHeuristic(FGraph graph) {
        super(graph);
        usedColors = new BitSet(ncolors);
        color2Edge = new TIntObjectHashMap<>(ncolors,0.75f,-1);
    }

    private final BitSet usedColors;
    private final TIntObjectHashMap<Loss> color2Edge;
    @Override
    public FTree solve() {
        extend(graph.getRoot().getOutgoingEdge(0));
        compute();
        return buildSolution(true);
    }

    private void compute() {
        int[] buffer = new int[3];
        while (true) {
            double relocateScore = Double.NEGATIVE_INFINITY;
            Arrays.fill(buffer, -1);
            for (int i=usedColors.nextSetBit(0); i >= 0; i = usedColors.nextSetBit(i+1)) {
                relocateScore = calculateRelocation(i, buffer, relocateScore);
            }
            if (relocateScore>0 || !Double.isInfinite(relocateScore)) {
                final Loss sel = color2Edge.get(buffer[0]);
                final Loss newLoss = sel.getTarget().getOutgoingEdge(buffer[1]);
                extend(newLoss);
                if (buffer[2]>=0) {
                    final Loss replaceLoss = newLoss.getTarget().getOutgoingEdge(buffer[2]);
                    color2Edge.put(replaceLoss.getTarget().getColor(), replaceLoss);
                }
            } else break;
        }
        selectedEdges.addAll(color2Edge.valueCollection());
    }

    private double calculateRelocation(int lossIndex, int[] indizesBuffer, double overrideScore) {
        final Loss l = color2Edge.get(lossIndex);
        final Fragment u = l.getSource(), v = l.getTarget();
        for (int i=0; i < v.getOutDegree(); ++i) {
            final Fragment w = v.getChildren(i);
            if (!usedColors.get(w.getColor())) {
                final double attachScore = v.getOutgoingEdge(i).getWeight();
                if (attachScore>overrideScore) {
                    overrideScore = attachScore;
                    indizesBuffer[0] = lossIndex;
                    indizesBuffer[1] = i;
                    indizesBuffer[2] = -1;
                }
                // search for other fragments we can relocate
                for (int j=0; j < w.getOutDegree(); ++j) {
                    final Fragment x = w.getChildren(j);
                    final Loss cur = color2Edge.get(x.getColor());
                    if (cur!=null && cur.getTarget() == x) {
                        double relocateScore = w.getOutgoingEdge(j).getWeight() - cur.getWeight();
                        final double finalScore = attachScore+relocateScore;
                        if (finalScore>overrideScore) {
                            overrideScore = finalScore;
                            indizesBuffer[0] = lossIndex;
                            indizesBuffer[1] = i;
                            indizesBuffer[2] = j;
                        }

                    }
                }
            }
        }
        return overrideScore;
    }

    private void extend(Loss l) {
        usedColors.set(l.getTarget().getColor());
        color2Edge.put(l.getTarget().getColor(), l);
    }

}
