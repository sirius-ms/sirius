package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;


import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.MaximumColorfulSubtreeAlgorithm;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * @author Kai Dührkop
 */
public class DPTreeBuilder implements TreeBuilder {

    private final MaximumColorfulSubtreeAlgorithm algorithm;
    private final int maxNumberOfColors;

    public DPTreeBuilder(int maxNumberOfColors) {
        this.algorithm = new MaximumColorfulSubtreeAlgorithm();
        this.maxNumberOfColors = maxNumberOfColors;
    }

    public DPTreeBuilder() {
        this(16);
    }

    @Override
    public FragmentationTree buildTree(ProcessedInput input, FragmentationGraph graph, double lowerbound) {
        return algorithm.compute(graph, maxNumberOfColors);
    }
}

