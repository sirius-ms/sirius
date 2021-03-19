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
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.AbstractHeuristic;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class GreedyBuilder implements TreeBuilder {

    protected final AbstractHeuristic heuristic;
    protected final Constructor<? extends AbstractHeuristic> constructor;

    public GreedyBuilder(AbstractHeuristic heuristic) {
        this.heuristic = heuristic;
        try {
            this.constructor = heuristic.getClass().getConstructor(FGraph.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FluentInterface computeTree() {
        return new FluentInterface(this);
    }

    @Override
    public Result computeTree(ProcessedInput input, FGraph graph, FluentInterface options) {
        try {
            AbstractHeuristic abstractHeuristic = constructor.newInstance(graph);
            FTree solve = abstractHeuristic.solve();
            return new Result(solve, false, AbortReason.COMPUTATION_CORRECT, abstractHeuristic.getGraphMappingBuilder().done(graph,solve));
        } catch (InstantiationException|IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    @Override
    public String toString() {
        return "Heuristic Solver: Greedy";
    }
}
