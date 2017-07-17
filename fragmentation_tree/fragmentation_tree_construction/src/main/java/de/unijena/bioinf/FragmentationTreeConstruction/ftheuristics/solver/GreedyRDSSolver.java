package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

public class GreedyRDSSolver extends GreedySolver {

	public GreedyRDSSolver(FGraph graph) {
		super(graph);
	}

	/**
	 * Builds a new tree with the Greedy Heuristic and removes dangling edges
	 * (RDE) if the tree has more than one fragment.
	 */
	@Override
	public FTree solve() {
		if (graph.numberOfEdges() == 1) {
			return tree;
		}

		this.tree = this.buildSolution();

		if (this.tree != null && this.tree.getFragments().size() > 1) {
			this.rds();
		}
		return this.tree;
	}
}
