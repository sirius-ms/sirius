package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver.GreedyRDSSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * The GreedyRDSTreeBuilder can create a new {@link GreedyRDSSolver} by using the {@link #buildTree(ProcessedInput, FGraph, double)} method. The Solver returns a {@link FTree} from the input graph.
 * @author Marie Lataretu
 * 
 */
public class GreedyRDSTreeBuilder extends HeuristicTreeBuilder {

	/**
	 * Constructs a {@link GreedyRDSTreeBuilder}
	 */
	public GreedyRDSTreeBuilder() {
		super();
	}

	public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
		long before = System.nanoTime();

		FTree tree = new GreedyRDSSolver(graph).solve();

		long after = System.nanoTime();

		long runningTimeNs = (after - before);
		addRuntime(runningTimeNs);

		return tree;
	}

	public String getDescription() {
		return "Greedy RDS";
	}

}
