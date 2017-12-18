package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.ExtendedCriticalPathHeuristic;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class ExtendedCriticalPathHeuristicTreeBuilder implements TreeBuilder {

    @Override
    public FluentInterface computeTree() {
        return new FluentInterface(this);
    }

    @Override
    public Result computeTree(ProcessedInput input, FGraph graph, FluentInterface options) {
        return new Result(new ExtendedCriticalPathHeuristic(graph).buildSolution(), false, AbortReason.COMPUTATION_CORRECT);
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
