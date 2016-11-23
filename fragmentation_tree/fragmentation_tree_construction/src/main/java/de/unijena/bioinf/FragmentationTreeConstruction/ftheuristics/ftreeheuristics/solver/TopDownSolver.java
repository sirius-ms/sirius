package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.ftreeheuristics.solver;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

public class TopDownSolver extends HeuristicSolver {

	public TopDownSolver(FGraph graph) {
		super(graph);
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
	 * Builds the new tree with the Top Down Heuristic.
	 * 
	 * @return the new tree
	 */
	private FTree buildSolution() {
		Fragment currentFragment;
		Loss bestLoss;

		for (int i = 0; i < this.graph.getRoot().getChildren().get(0)
				.getChildren().size(); i++) {
			currentFragment = this.graph.getRoot().getChildren().get(0);
			bestLoss = getBestLoss(currentFragment);

			while (bestLoss.getSource() != null) {
				this.addTreeFragment(bestLoss);
				currentFragment = bestLoss.getTarget();
				bestLoss = this.getBestLoss(currentFragment);
			}
		}
		return tree;
	}

	/**
	 * <p>
	 * Finds the loss with the biggest weight in the graph, that starts in a
	 * certain fragment and preserves the colorful property.
	 * </p>
	 * </p> If no such loss exist, it returns a loss where source and target is
	 * null.</p>
	 * 
	 * @param parent
	 *            source of the loss
	 * @return loss with the biggest weight, that starts in a certain fragment
	 *         and preserves the colorful property
	 */
	private Loss getBestLoss(Fragment parent) {
		Loss bestLoss = new Loss(null, null, null, -Double.MAX_VALUE);
		for (Loss loss : parent.getOutgoingEdges()) {

			if (!treeUsedColors.contains(loss.getTarget().getColor())
					&& bestLoss.getWeight() < loss.getWeight()) {
				bestLoss = loss;
			}
		}
		return bestLoss;
	}

	@Override
	protected void addFlags(int fragmentID) {
		this.treeUsedColors.add(this.colorForEachVertex[fragmentID]);
	}
}
