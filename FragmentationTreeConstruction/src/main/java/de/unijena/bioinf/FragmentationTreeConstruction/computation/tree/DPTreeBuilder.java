package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;


import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.MaximumColorfulSubtreeAlgorithm;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.List;

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
    public Object prepareTreeBuilding(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;
    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
        return algorithm.compute(graph, maxNumberOfColors);
    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
        return buildTree(input, graph, lowerbound, prepareTreeBuilding(input, graph, lowerbound));
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
        return algorithm.computeMultipleTrees(graph, maxNumberOfColors);
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound) {
        return buildMultipleTrees(input, graph, lowerbound, prepareTreeBuilding(input, graph, lowerbound));
    }

    @Override
    public String getDescription() {
        return "DP";
    }
}

