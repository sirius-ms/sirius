package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.IsotopicMarker;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.CriticalPathInsertionHeuristic;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.CriticalPathInsertionWithIsotopePeaksHeuristic;
import de.unijena.bioinf.sirius.ProcessedInput;

public class ExtendedCriticalPathHeuristicTreeBuilder implements TreeBuilder {

    @Override
    public FluentInterface computeTree() {
        return new FluentInterface(this);
    }

    @Override
    public Result computeTree(ProcessedInput input, FGraph graph, FluentInterface options) {
        if (graph.getFragmentAnnotationOrNull(IsotopicMarker.class)!=null) {
            return new Result(new CriticalPathInsertionWithIsotopePeaksHeuristic(graph).solve(), false, AbortReason.COMPUTATION_CORRECT);
        } else {
            return new Result(new CriticalPathInsertionHeuristic(graph).solve(), false, AbortReason.COMPUTATION_CORRECT);
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
