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

package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IsotopicMarker;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.CriticalPathInsertionHeuristic;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.CriticalPathInsertionWithIsotopePeaksHeuristic;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.concurrent.Callable;

public class ExtendedCriticalPathHeuristicTreeBuilder implements TreeBuilder {

    protected Callable<Boolean> interruptionCheck;

    public ExtendedCriticalPathHeuristicTreeBuilder(Callable<Boolean> interruptionCheck) {
        this.interruptionCheck = interruptionCheck;
    }

    @Override
    public FluentInterface computeTree() {
        return new FluentInterface(this).withInterruptionCheck(interruptionCheck);
    }

    @Override
    public Result computeTree(ProcessedInput input, FGraph graph, FluentInterface options) {
        if (graph.getFragmentAnnotationOrNull(IsotopicMarker.class)!=null) {
            CriticalPathInsertionWithIsotopePeaksHeuristic h = new CriticalPathInsertionWithIsotopePeaksHeuristic(graph);
            if (options.getInterruptionCheck()!=null) {
                h.setInteruptionCheck(options.getInterruptionCheck());
            } else h.setInteruptionCheck(interruptionCheck);
            FTree t = h.solve();
            return new Result(t, false, AbortReason.COMPUTATION_CORRECT, h.getGraphMappingBuilder().done(graph,t));
        } else {
            CriticalPathInsertionHeuristic h = new CriticalPathInsertionHeuristic(graph);
            if (options.getInterruptionCheck()!=null) {
                h.setInteruptionCheck(options.getInterruptionCheck());
            } else h.setInteruptionCheck(interruptionCheck);
            FTree t = h.solve();
            return new Result(t, false, AbortReason.COMPUTATION_CORRECT, h.getGraphMappingBuilder().done(graph,t));
        }
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    @Override
    public String toString() {
        return "Heuristic Solver: Critical Path";
    }
}
