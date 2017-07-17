package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.HashSet;
import java.util.Map;

public class PrimStyleSolver extends HeuristicSolver {

	public PrimStyleSolver(FGraph graph) {
		super(graph);
	}

	@Override
	public FTree solve() {
		if (graph.numberOfEdges() == 1) {
			return tree;
		}
		return buildSolution();
	}

	/**
	 * Builds a new tree with the Prim Style Heuristic.
	 * 
	 * @return the new tree
	 */
	protected FTree buildSolution() {
		while (treeUsedColors.size() != graphUsedColors.size()) {
			Loss bestLoss = this.getBestLoss();

			if (bestLoss.getSource() != null) {
				this.addTreeFragment(bestLoss);
			} else {
				break;
			}
		}
		return this.tree;
	}

	/**
	 * <p>
	 * Finds loss with the biggest weight in the graph that can be reached from
	 * the current tree fragments and preserves the tree and colorful property
	 * </p>
	 * <p>
	 * If no such loss exist, it returns a loss where the source and target is
	 * null
	 * </p>
	 * 
	 * @return loss with the biggest weight that can be reached from the current
	 *         tree fragments and preserves the tree and colorful property
	 */
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
						&& currentLoss.getWeight() > bestLoss.getWeight()) {
					bestLoss = currentLoss;
				}
			}
		}
		return bestLoss;
	}

	@Override
	protected void addFlags(int fragmentID) {
		this.treeVertexUsed[fragmentID] = true;

		this.treeUsedColors.add(this.colorForEachVertex[fragmentID]);

		this.treeReachableFragments.remove(fragmentID);

		for (Fragment child : this.graph.getFragmentAt(fragmentID)
				.getChildren()) {
			int childID = child.getVertexId();
			if (treeVertexUsed[childID] == false) {
				HashSet<Integer> parentList;
				if (!treeReachableFragments.containsKey(childID)) {
					parentList = new HashSet<Integer>();
				} else {
					parentList = treeReachableFragments.get(childID);
				}
				parentList.add(fragmentID);
				this.treeReachableFragments.put(childID, parentList);
			}
		}
	}

}
