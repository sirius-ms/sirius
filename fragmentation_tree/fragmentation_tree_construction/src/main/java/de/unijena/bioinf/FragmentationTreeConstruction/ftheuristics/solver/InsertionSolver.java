package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class InsertionSolver extends HeuristicSolver {

	/**
	 * Saves the (graph) ID for a used tree fragment. If a fragment is not used
	 * in the tree, the ID is -1. In this case -1 is not necessarily the true
	 * graph ID of the fragment.
	 */
	private int[] treeParentID;

	/**
	 * A list of fragment (graph) IDs which contribute to the maximum score.
	 */
	private ArrayList<Integer> maxScoreFragments;

	public InsertionSolver(FGraph graph) {
		super(graph);

		treeReachableFragments = new HashMap<Integer, HashSet<Integer>>();

		for (Fragment child : this.graph.getRoot().getChildren(0).getChildren()) {
			int childID = child.getVertexId();
			HashSet<Integer> parentList = new HashSet<Integer>();
			parentList.add(this.graph.getRoot().getChildren(0).getVertexId());
			treeReachableFragments.put(childID, parentList);
		}

		this.treeParentID = new int[this.graph.numberOfVertices()];
		Arrays.fill(this.treeParentID, -1);

		this.maxScoreFragments = new ArrayList<Integer>();
	}

	@Override
	public FTree solve() {
		if (this.graph.numberOfEdges() == 1) {
			return tree;
		} else {
			return buildSolution();
		}
	}

	/**
	 * Builds a new tree with the Insertion Heuristic.
	 * 
	 * @return the new tree
	 */
	private FTree buildSolution() {
		Loss bestLoss = getBestLoss();
		Fragment source = bestLoss.getSource(); // u
		Fragment target = bestLoss.getTarget(); // v
		while (treeUsedColors.size() != graphUsedColors.size()
				&& source != null) {
			int sourceID = source.getVertexId();
			int targetID = target.getVertexId();

			this.addTreeFragment(bestLoss);
			this.treeParentID[targetID] = sourceID;

			// swap losses to the new fragment
			for (int fragID : maxScoreFragments) {
				Fragment v = this.tree
						.getFragmentAt(this.treeVertexID[targetID]);
				Fragment x = this.tree.getFragmentAt(this.treeVertexID[fragID]);
				Loss alternateLoss = this.graph.getLoss(target,
						this.graph.getFragmentAt(fragID));
				Loss newLoss = this.tree.swapLoss(x, v);
				newLoss.setWeight(alternateLoss.getWeight());
				newLoss.setFormula(alternateLoss.getFormula());
				this.treeParentID[fragID] = targetID;
			}

			bestLoss = getBestLoss();
			source = bestLoss.getSource(); // u
			target = bestLoss.getTarget(); // v
		}
		return tree;
	}

	/**
	 * Calculates the score for reachable and unused colored fragments. The best
	 * loss has the highest score.
	 * 
	 * @return the best loss to add
	 */
	@SuppressWarnings("unchecked")
	private Loss getBestLoss() {
		Loss bestLoss = new Loss(null, null, null, -Double.MAX_VALUE);
		double bestScore = bestLoss.getWeight();
		for (Entry<Integer, HashSet<Integer>> reachableFragment : treeReachableFragments
				.entrySet()) {
			int fragID = reachableFragment.getKey();
			HashSet<Integer> parents = reachableFragment.getValue();
			ArrayList<Integer> tempMaxScoreFragments = new ArrayList<Integer>();

			// reachable fragment with unused color
			if (!this.treeUsedColors.contains(this.colorForEachVertex[fragID])) {
				for (int parent : parents) {
					Loss currentLoss = this.graph.getLoss(
							this.graph.getFragmentAt(parent),
							this.graph.getFragmentAt(fragID));
					Fragment source = currentLoss.getSource(); // u
					Fragment target = currentLoss.getTarget(); // v
					double currentScore = currentLoss.getWeight();

					// all children of v
					for (Fragment child : target.getChildren()) { // x

						int childID = child.getVertexId();

						// children of v and of u
						if (this.treeVertexUsed[childID] == true
								&& this.treeParentID[childID] == parent) {
							double weightNew = this.graph
									.getLoss(target, child).getWeight(); // vx
							double weightOld = this.graph
									.getLoss(source, child).getWeight(); // ux
							if (weightNew > weightOld) {
								currentScore = currentScore
										+ (weightNew - weightOld);
								tempMaxScoreFragments.add(childID);
							}
						}
					}
					if (currentScore > bestScore) {
						bestLoss = currentLoss;
						bestScore = currentScore;
						this.maxScoreFragments = (ArrayList<Integer>) tempMaxScoreFragments
								.clone();
					}
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
