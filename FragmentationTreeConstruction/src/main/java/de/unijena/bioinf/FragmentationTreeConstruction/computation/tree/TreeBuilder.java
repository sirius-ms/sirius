package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;

import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * @author Kai Dührkop
 */
public interface TreeBuilder {

    public FragmentationTree buildTree(ProcessedInput input, FragmentationGraph graph, double lowerbound);

}
