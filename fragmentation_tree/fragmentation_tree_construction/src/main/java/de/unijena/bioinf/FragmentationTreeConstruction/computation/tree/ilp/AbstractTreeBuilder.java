package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class AbstractTreeBuilder<T extends AbstractSolver> implements TreeBuilder {

    protected final IlpFactory<T> factory;

    public AbstractTreeBuilder(IlpFactory<T> factory) {
        this.factory = factory;
    }

    @Override
    public FluentInterface computeTree() {
        return new FluentInterface(this);
    }

    @Override
    public Result computeTree(ProcessedInput input, FGraph graph, FluentInterface options) {
        return factory.create(input,graph,options).compute();
    }

    @Override
    public boolean isThreadSafe() {
        return factory.isThreadSafe();
    }

    @Override
    public String toString() {
        return "ILP Solver: " + factory.name();
    }
}
