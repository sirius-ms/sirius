package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.treebuilder;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver.PrimStyleRDESolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * The PrimStyleRDETreeBuilder can create a new {@link PrimStyleRDESolver} by using the {@link #buildTree(ProcessedInput, FGraph, double)} method. The Solver returns a {@link FTree} from the input graph.
 * @author Marie Lataretu
 * 
 */
public class PrimStyleRDETreeBuilder extends HeuristicTreeBuilder {

	/**
	 * Constructs a {@link PrimStyleRDETreeBuilder}
	 */
	public PrimStyleRDETreeBuilder() {
		super();
	}

	public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
		long before = System.nanoTime();

		FTree tree = new PrimStyleRDESolver(graph).solve();

		long after = System.nanoTime();

		long runningTimeNs = (after - before);
		addRuntime(runningTimeNs);

		return tree;
	}

	public String getDescription() {
		return "Prim Style RDE";
	}

}
