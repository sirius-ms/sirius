package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.List;

/**
 * @author Kai Dührkop
 */
public interface TreeBuilder {

    public Object prepareTreeBuilding(ProcessedInput input, FGraph graph, double lowerbound);

    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound, Object preparation);

    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound);

    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound, Object preparation);

    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound);

    public String getDescription();


}
