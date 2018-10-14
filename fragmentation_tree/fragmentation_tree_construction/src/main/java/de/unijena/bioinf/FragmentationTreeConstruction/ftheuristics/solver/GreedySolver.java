package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.*;

public class GreedySolver extends HeuristicSolver {

	/**
	 * all {@link Loss} sorted by their weight from biggest to smallest
	 */
	private ArrayList<Loss> sortedLosses;

	/**
	 * Saves if a used tree fragment has already a parent in the tree.
	 */
	private boolean[] hasTreeParent;

	/**
	 * Helps to connect tree fragments, when the child is added before the
	 * parent.
	 */
	private HashMap<Integer, ArrayList<Integer>> treeFragmentMissingParent;

	public GreedySolver(FGraph graph) {
		super(graph);

		/*
		 * sort the {@link Loss} of the graph from biggest to smallest weight
		 */
		this.sortedLosses = (ArrayList<Loss>) graph.losses();
		Collections.sort(sortedLosses, new Comparator<Loss>() {

			public int compare(Loss l1, Loss l2) {
				double difference = l1.getWeight() - l2.getWeight();
				if (difference > 0) {
					return 1;
				} else if (difference < 0) {
					return -1;
				} else
					return 0;
			}
		});
		Collections.reverse(sortedLosses);

		this.hasTreeParent = new boolean[this.graph.numberOfVertices()];
		Arrays.fill(hasTreeParent, false);
		
		this.treeFragmentMissingParent = new HashMap<Integer, ArrayList<Integer>>();
	}

	@Override
	public FTree solve() {
		if (graph.numberOfEdges() == 1) {
			return tree;
		}
		return buildSolution();
	}

	/**
	 * Builds the new tree with the Greedy Heuristic.
	 * 
	 * @return the new tree
	 */
	protected FTree buildSolution() {
		for (Loss loss : sortedLosses) {

			/*
			 * get loss properties
			 */
			Fragment source = loss.getSource();
			Fragment target = loss.getTarget();
			int sourceColor = source.getColor();
			int targetColor = target.getColor();
			int sourceID = source.getVertexId();
			int targetID = target.getVertexId();

			/*
			 * ignore the pseudo-root of the graph
			 */
			if (sourceID != graph.getRoot().getVertexId()) {

				if (!treeUsedColors.contains(sourceColor)
						&& !treeUsedColors.contains(targetColor)) {
					this.hasTreeParent[targetID] = true;
					this.addTreeFragment(loss, this.treeFragmentMissingParent);
				} else if (treeUsedColors.contains(sourceColor)
						&& treeUsedColors.contains(targetColor)) {
					if (this.treeVertexUsed[sourceID] == true
							&& this.treeVertexUsed[targetID] == true
							&& !this.hasTreeParent[targetID]) {
						this.hasTreeParent[targetID] = true;
						this.addTreeFragment(loss, this.treeFragmentMissingParent);
					}
				} else if (treeUsedColors.contains(sourceColor)
						&& !treeUsedColors.contains(targetColor)) {
					if (this.treeVertexUsed[sourceID] == true) {
						this.hasTreeParent[targetID] = true;
						this.addTreeFragment(loss, this.treeFragmentMissingParent);
					}
				} else if (!treeUsedColors.contains(sourceColor)
						&& treeUsedColors.contains(targetColor)) {
					if (this.treeVertexUsed[targetID] == true
							&& !this.hasTreeParent[targetID]) {
						this.hasTreeParent[targetID] = true;
						this.addTreeFragment(loss, this.treeFragmentMissingParent);
					}
				}
			}

		}
		if (this.connectFragments()) {
			return this.tree;
		} else {
			return null;
		}

	}

	@Override
	protected void addFlags(int fragmentID) {
		this.treeVertexUsed[fragmentID] = true;
		this.treeUsedColors.add(this.colorForEachVertex[fragmentID]);
	}

	/**
	 * <p>
	 * Connects the slack {@link Fragment} in the {@link FTree}
	 * </p>
	 * <p>
	 * For each fragment (parent) of the tree we have to find the child and
	 * connect these two fragment with a new loss.
	 * </p>
	 */
	private boolean connectFragments() {
		for (Map.Entry<Integer, ArrayList<Integer>> missingParent : this.treeFragmentMissingParent
				.entrySet()) {
			int parentID = missingParent.getKey();
			if (!(this.treeVertexUsed[parentID] && this.treeVertexID[parentID] == -1)) {
				for (int childID : missingParent.getValue()) {
					Fragment parent = this.tree
							.getFragmentAt(this.treeVertexID[parentID]);
					Fragment child = this.tree
							.getFragmentAt(this.treeVertexID[childID]);

					double weight = child.getIncomingEdge().getWeight();
					MolecularFormula formula = child.getFormula();

					Loss newLoss = this.tree.swapLoss(child, parent);
					newLoss.setWeight(weight);
					newLoss.setFormula(formula);
				}
			} else {
				return false;
			}
		}
		return true;
	}
	
	private void addTreeFragment(Loss loss, HashMap<Integer, ArrayList<Integer>> treeFragmentMissingParent) {
		Fragment source = loss.getSource();
		Fragment target = loss.getTarget();
		int sourceID = source.getVertexId();
		int targetID = target.getVertexId();

		if (this.treeVertexID[sourceID] == -1
				&& this.treeVertexID[targetID] == -1) {
			Fragment scr = new Fragment(source.getVertexId(),
					source.getFormula(), source.getIonization());
			scr.setColor(source.getColor());
			Fragment newFragment = this.tree.addFragment(scr,
					target);

			newFragment.getIncomingEdge().setWeight(loss.getWeight());
			newFragment.setColor(target.getColor());

			this.treeVertexID[targetID] = newFragment.getVertexId();

			ArrayList<Integer> childList;
			if (!treeFragmentMissingParent.containsKey(sourceID)) {
				childList = new ArrayList<Integer>();
			} else {
				childList = treeFragmentMissingParent.get(sourceID);
			}
			childList.add(targetID);
			treeFragmentMissingParent.put(sourceID, childList);
		} else if (this.treeVertexID[sourceID] != -1
				&& this.treeVertexID[targetID] == -1) {
			Fragment scr = this.tree.getFragmentAt(this.treeVertexID[sourceID]);
			Fragment newFragment = this.tree.addFragment(scr,
					target);

			newFragment.getIncomingEdge().setWeight(loss.getWeight());
			newFragment.setColor(target.getColor());

			this.treeVertexID[targetID] = newFragment.getVertexId();

		}
		addFlags(source.getVertexId());
		addFlags(target.getVertexId());
	}
}
