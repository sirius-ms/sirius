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

import java.util.ArrayList;
import java.util.BitSet;

public class DeepSearchHeuristic extends AbstractHeuristic {

    private final BitSet usedColors;
    private final ArrayList<Fragment> stack;

    public DeepSearchHeuristic(FGraph graph) {
        super(graph);
        this.usedColors = new BitSet(ncolors);
        this.stack = new ArrayList<>(ncolors);
        stack.add(graph.getRoot());
    }

    @Override
    public FTree solve() {
        compute();
        return buildSolution(true);
    }

    private void compute() {
        while (!stack.isEmpty()) {
            final int i = stack.size()-1;
            final Fragment u = stack.get(i);
            double maxScore = Double.NEGATIVE_INFINITY;
            int maxIndex = -1;
            for (int index = 0; index < u.getOutDegree(); ++index) {
                final Fragment v = u.getChildren(index);
                if (!usedColors.get(v.getColor())) {
                    final double score = u.getOutgoingEdge(index).getWeight();
                    if (score > maxScore) {
                        maxIndex = index;
                        maxScore = score;
                    }
                }
            }
            if (maxIndex>=0) {
                selectedEdges.add(u.getOutgoingEdge(maxIndex));
                final Fragment v = u.getChildren(maxIndex);
                usedColors.set(v.getColor());
                stack.add(v);
            } else {
                stack.remove(i);
            }
        }
    }
}
