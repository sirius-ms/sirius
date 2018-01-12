package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.AbstractHeuristic;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

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
            return new Result(constructor.newInstance(graph).solve(), false, AbortReason.COMPUTATION_CORRECT);
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
