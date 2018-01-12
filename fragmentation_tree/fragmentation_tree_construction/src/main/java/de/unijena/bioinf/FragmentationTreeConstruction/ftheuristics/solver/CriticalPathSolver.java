package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class CriticalPathSolver extends HeuristicSolver {

	protected double score = 0d;

	public CriticalPathSolver(FGraph graph) {
		super(graph);
		score = graph.getRoot().getOutgoingEdge(0).getWeight();

		this.treeSubtreeScore = new double[this.graph.numberOfVertices()];
	}

	@Override
	public FTree solve() {
		if (this.graph.numberOfEdges() == 1) {
			return tree;
		}
		return this.buildSolution();
	}

	/**
	 * Builds a new tree with the Critical Path Heuristic.
	 * 
	 * @return the new tree
	 */
	private FTree buildSolution() {
		Loss bestLoss = this.getBestLoss();

		while (bestLoss.getSource() != null) {
			//System.out.println(".....");
			this.addTreeFragment(bestLoss);

			// add the path below the added loss
			Fragment pathChild = bestLoss.getTarget();
			addCriticalPath(pathChild);
			bestLoss = this.getBestLoss();
		}
		tree.setTreeWeight(score);
		return this.tree;
	}

	@Override
	protected void addTreeFragment(Loss loss) {
		super.addTreeFragment(loss);
		score += loss.getWeight();
		//System.out.println("ADD " + loss + " WITH WEIGHT " + loss.getWeight());
	}

	/**
	 * Finds the loss with the biggest score, that preserves the tree and
	 * colorful property if added to the tree.
	 * 
	 * @return loss with the biggest score
	 */
	private Loss getBestLoss() {
		// at first calculate the scores
		this.calculatePaths();

		Loss bestLoss = new Loss(null, null, null, -Double.MAX_VALUE);
		double bestScore = -Double.MAX_VALUE;

		for (Map.Entry<Integer, HashSet<Integer>> reachableFragment : treeReachableFragments
				.entrySet()) {
			int fragID = reachableFragment.getKey();
			HashSet<Integer> parents = reachableFragment.getValue();
			if (!this.treeUsedColors.contains(colorForEachVertex[fragID])) {
				for (int parentID : parents) {
					Loss currentLoss = this.graph.getLoss(
							this.graph.getFragmentAt(parentID),
							this.graph.getFragmentAt(fragID));

					double currentScore = treeSubtreeScore[fragID]
							+ currentLoss.getWeight();

					// the best score must be bigger than zero, else no loss can
					// be added to the tree
					if (currentScore > 0 && currentScore > bestScore) {
						bestLoss = currentLoss;
						bestScore = currentScore;
					}
				}
			}
		}
		return bestLoss;
	}

	/**
	 * Traverses the graph and calculates the scores depending on the current
	 * tree.
	 */
	private void calculatePaths() {
		Iterator<Fragment> iter = this.graph.postOrderIterator();

		while (iter.hasNext()) {
			Fragment currentFragment = iter.next();
			int currFragID = currentFragment.getVertexId();
			if (this.treeVertexUsed[currFragID] == true
					|| !this.treeUsedColors
							.contains(this.colorForEachVertex[currFragID])) {
				// the currentFragment is already in the tree, or it is not in
				// the tree and its color is not in the tree

				double bestScore = 0;
				for (Fragment child : currentFragment.getChildren()) {
					int childID = child.getVertexId();
					if (!this.treeUsedColors
							.contains(colorForEachVertex[childID])) {

						double currentScore = this.treeSubtreeScore[child
								.getVertexId()]
								+ this.graph.getLoss(currentFragment, child)
										.getWeight();

						if (currentScore > bestScore) {
							bestScore = currentScore;
						}
					}
				}
				this.treeSubtreeScore[currFragID] = bestScore;
			}
		}
	}

	/**
	 * Backtracks the best critical path in the graph starting in a certain
	 * fragment.
	 * 
	 * @param startFragment
	 *            where the path starts
	 */
	private void addCriticalPath(Fragment startFragment) {
		while (this.treeSubtreeScore[startFragment.getVertexId()] > 0) {
			for (Fragment child : startFragment.getChildren()) {
				int childID = child.getVertexId();
				if (!this.treeUsedColors
						.contains(this.colorForEachVertex[childID])) {
					Loss loss = this.graph.getLoss(startFragment, child);

					if (this.treeSubtreeScore[startFragment.getVertexId()] <= this.treeSubtreeScore[childID]
							+ loss.getWeight()) {
						addTreeFragment(loss);
						startFragment = loss.getTarget();
						break;
					}
				}
			}
			if (startFragment.isLeaf()) {
				break;
			}
		}
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
