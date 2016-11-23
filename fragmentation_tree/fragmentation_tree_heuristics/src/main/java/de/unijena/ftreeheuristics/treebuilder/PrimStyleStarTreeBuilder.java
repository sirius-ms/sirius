package de.unijena.ftreeheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.ftreeheuristics.solver.PrimStyleStarSolver;

/**
 * The PrimStyleStarTreeBuilder can create a new {@link PrimStyleStarSolver} by using the {@link #buildTree(ProcessedInput, FGraph, double)} method. The Solver returns a {@link FTree} from the input graph.
 * @author Marie Lataretu
 * 
 */
public class PrimStyleStarTreeBuilder extends HeuristicTreeBuilder {

	/**
	 * Constructs a {@link PrimStyleStarTreeBuilder}
	 */
	public PrimStyleStarTreeBuilder() {
		super();
	}

	public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
		long before = System.nanoTime();

		FTree tree = new PrimStyleStarSolver(graph).solve();

		long after = System.nanoTime();

		long runningTimeNs = (after - before);
		addRuntime(runningTimeNs);

		return tree;
	}

	public String getDescription() {
		return "Prim Style Star";
	}

}
