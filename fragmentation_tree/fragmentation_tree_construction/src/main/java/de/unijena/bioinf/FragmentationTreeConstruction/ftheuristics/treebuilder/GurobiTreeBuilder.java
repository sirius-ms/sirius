package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * The GurobiTreeBuilder can create a new {@link GurobiSolver} by using the {@link #buildTree(ProcessedInput, FGraph, double)} method. The Solver returns a {@link FTree} from the input graph.
 * @author Marie Lataretu
 * 
 */
public class GurobiTreeBuilder extends GurobiSolver {

	/**
	 * Sums up the runtime in nanoseconds that is needed for a certain TreeBuilder to run the {@link #buildTree(ProcessedInput, FGraph, double)} method.
	 */
	protected long runtimeSumNs;

	/**
	 * Constructs a {@link GurobiTreeBuilder}.
	 */
	public GurobiTreeBuilder() {
		super();
		this.runtimeSumNs = 0;
	}

	/**
	 * returns the {@link FTree} computed by the {@link GurobiSolver} and
	 * measures the runtime
	 */
	public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
		long before = System.nanoTime();
		FTree tree = buildTree(input, graph, lowerbound,
				prepareTreeBuilding(input, graph, lowerbound));
		long after = System.nanoTime();

		long runningTimeNs = (after - before);
		this.runtimeSumNs = this.runtimeSumNs + runningTimeNs;

		return tree;
	}

	/**
	 * @return total runtime variable {@link #runtimeSumNs}
	 */
	public long getRuntimeSumNs() {
		return runtimeSumNs;
	}
}
