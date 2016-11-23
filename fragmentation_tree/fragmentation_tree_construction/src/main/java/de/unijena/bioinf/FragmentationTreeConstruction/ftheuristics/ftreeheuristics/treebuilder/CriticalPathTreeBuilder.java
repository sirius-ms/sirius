package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.ftreeheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.ftreeheuristics.solver.CriticalPathSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * The CriticalPathTreeBuilder can create a new {@link CriticalPathSolver} by using the {@link #buildTree(ProcessedInput, FGraph, double)} method. The Solver returns a {@link FTree} from the input graph.
 * @author Marie Lataretu
 * 
 */
public class CriticalPathTreeBuilder extends HeuristicTreeBuilder {

	/**
	 * Constructs a {@link CriticalPathTreeBuilder}.
	 */
	public CriticalPathTreeBuilder() {
		super();
	}

	public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
		long before = System.nanoTime();
		FTree tree = new CriticalPathSolver(graph).solve();
		long after = System.nanoTime();
		long runningTimeNs = (after - before);
		
		addRuntime(runningTimeNs);

		return tree;
	}

	public String getDescription() {
		return "Critical Path";
	}

}
