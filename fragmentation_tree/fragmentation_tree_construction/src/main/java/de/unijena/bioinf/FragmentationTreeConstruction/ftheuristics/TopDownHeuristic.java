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

import java.util.BitSet;

public class TopDownHeuristic extends AbstractHeuristic {

    private final BitSet usedColors;

    public TopDownHeuristic(FGraph graph) {
        super(graph);
        this.usedColors = new BitSet(ncolors);
    }

    @Override
    public FTree solve() {
        compute();
        return buildSolution(true);
    }

    private void compute() {
        Fragment root = graph.getRoot().getChildren(0);
        usedColors.set(root.getColor());
        selectedEdges.add(root.getIncomingEdge());
        Fragment u = root;
        Loss l;
        while ((l=findBestLoss(root))!=null) {
            Fragment v = l.getTarget();
            Loss l2;
            while ((l2=findBestLoss(v))!=null) {
                v = l2.getTarget();
            }
        }
    }

    private Loss findBestLoss(Fragment u) {
        Loss bestLoss = null;
        for (int i=0, n=u.getOutDegree(); i <n; ++i) {
            Loss l = u.getOutgoingEdge(i);
            if (!usedColors.get(l.getTarget().getColor()) && (bestLoss==null || bestLoss.getWeight() < l.getWeight())) {
                bestLoss = l;
            }
        }
        if (bestLoss == null) return null;
        else {
            selectedEdges.add(bestLoss);
            usedColors.set(bestLoss.getTarget().getColor());
            return bestLoss;
        }
    }
}
