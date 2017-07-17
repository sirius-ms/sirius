package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.List;

/**
 * The HeuristicTreeBuilder provides a super class for all heuristic TreeBuilders.
 * @author Marie Lataretu
 *
 */
public abstract class HeuristicTreeBuilder implements TreeBuilder{
	
	/**
	 * Sums up the runtime in nanoseconds that is needed for a certain TreeBuilder to run the {@link #buildTree(ProcessedInput, FGraph, double)} method.
	 */
	protected long runtimeSumNs;
	
	/**
	 * Constructs a {@link HeuristicTreeBuilder}
	 */
	public HeuristicTreeBuilder(){
		this.runtimeSumNs = 0;
	}
 	
	public Object prepareTreeBuilding(ProcessedInput input, FGraph graph,
			double lowerbound) {
		return null;
	}

	public FTree buildTree(ProcessedInput input, FGraph graph,
			double lowerbound, Object preparation) {
			return buildTree(input, graph, lowerbound);
	}

	/**
	 * Returns the {@link FTree} computed by the Solver and measures the runtime.
	 */
	public abstract FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound);

	public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph,
			double lowerbound, Object preparation) {
		return null;
	}

	public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph,
			double lowerbound) {
		return null;
	}
	
	/**
	 * Returns a description of the TreeBuilder.
	 */
	public abstract String getDescription();

	/**
	 * Adds time to the total runtime variable {@link #runtimeSumNs}.
	 * @param ns time in nanoseconds to be added
	 */
	protected void addRuntime(long ns) {
		this.runtimeSumNs = this.runtimeSumNs + ns;
	}
	
	/**
	 * @return total runtime variable {@link #runtimeSumNs}
	 */
	public long getRuntimeSumNs() {
		return runtimeSumNs;
	}
	
}
