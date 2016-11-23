package de.unijena.ftreeheuristics.solver;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.HashSet;
import java.util.Map;

public class PrimStyleStarSolver extends PrimStyleSolver {

	public PrimStyleStarSolver(FGraph graph) {
		super(graph);
	}

	@Override
	public FTree solve() {
		if (this.graph.numberOfEdges() == 1) {
			return tree;
		}
		return buildSolution();
	}

	/**
	 * <p>
	 * Finds loss with the biggest positive weight in the graph that can be
	 * reached from the current tree fragments and preserves the tree and
	 * colorful property.
	 * </p>
	 * <p>
	 * If no such loss exist, it returns a loss where the source and target is
	 * null.
	 * </p>
	 * 
	 * @return loss with the biggest positive weight that can be reached from
	 *         the current tree fragments and preserves the tree and colorful
	 *         property
	 */
	@Override
	protected Loss getBestLoss() {
		Loss bestLoss = new Loss(null, null, null, -Double.MAX_VALUE);
		for (Map.Entry<Integer, HashSet<Integer>> reachableFragment : treeReachableFragments
				.entrySet()) {
			int fragID = reachableFragment.getKey();
			HashSet<Integer> parents = reachableFragment.getValue();
			for (int parentID : parents) {

				Loss currentLoss = this.graph.getLoss(
						this.graph.getFragmentAt(parentID),
						this.graph.getFragmentAt(fragID));
				if (!treeUsedColors.contains(this.colorForEachVertex[fragID])
						&& currentLoss.getWeight() > 0
						&& currentLoss.getWeight() > bestLoss.getWeight()) {
					bestLoss = currentLoss;
				}
			}
		}
		return bestLoss;
	}
}
