package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;

import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * @author Kai Dührkop
 */
public interface TreeBuilder {

    public Object prepareTreeBuilding(ProcessedInput input, FragmentationGraph graph, double lowerbound);

    public FragmentationTree buildTree(ProcessedInput input, FragmentationGraph graph, double lowerbound, Object preparation);

    public FragmentationTree buildTree(ProcessedInput input, FragmentationGraph graph, double lowerbound);

}
