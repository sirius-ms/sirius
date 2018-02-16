package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public interface IlpFactory<T extends AbstractSolver> {

    public T create(ProcessedInput input, FGraph graph, TreeBuilder.FluentInterface options);

    public boolean isThreadSafe();

    public String name();

}
