package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics.solver;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.*;

public abstract class HeuristicSolver {

	protected final FGraph graph;

	/**
	 * The new {@link FTree} to return.
	 */
	protected FTree tree;

	/**
	 * <p>
	 * Array with all vertices of the graph.
	 * </p>
	 * <p>
	 * vertices[x] = true: vertex with the graph ID x is used in the tree.
	 * </p>
	 */
	protected boolean[] treeVertexUsed;

	/**
	 * Saves the tree ID of a vertex. The graph ID of the same vertex is
	 * usually different from the tree ID.
	 */
	protected int[] treeVertexID;

	/**
	 * <p>
	 * Array with the color for each vertex of the graph.
	 * </p>
	 * <p>
	 * colorForEachVertex[x] = c: vertex with ID x has the color c.
	 * </p>
	 */
	protected final int[] colorForEachVertex;

	/**
	 * <p>
	 * Saves a sore for each fragment in the tree.
	 * </p>
	 * <p>
	 * The score is calculated in different ways, depending on the heuristic.
	 * </p>
	 * <p>
	 * Note that, {@link #treeSubtreeScore} is not initialized in this
	 * constructor, but in the specific solver, where it is needed.
	 * </p>
	 */
	protected double[] treeSubtreeScore;

	/**
	 * Set of the colors in the tree.
	 */
	protected HashSet<Integer> treeUsedColors;

	/**
	 * Set of the colors in the graph.
	 */
	protected final HashSet<Integer> graphUsedColors;

	/**
	 * <p>
	 * Saves all fragments, that are not in the tree, but can be reached from
	 * the current tree ignoring the color constrain.
	 * </p>
	 * <p>
	 * Note that, {@link #treeReachableFragments} is not initialized in this
	 * constructor, but in the specific solver, where it is needed.
	 * </p>
	 */
	protected HashMap<Integer, HashSet<Integer>> treeReachableFragments;

	/**
	 * Constructs a {@link HeuristicSolver} and assigns the attributes.
	 * 
	 * @param graph
	 */
	public HeuristicSolver(FGraph graph) {
		if (graph == null) {
			throw new NullPointerException("Cannot solve graph: graph is NULL!");
		}

		this.graph = graph;
		int graphNumberOfVertices = this.graph.numberOfVertices();

		/*
		 * initialization
		 */
		this.treeVertexUsed = new boolean[graphNumberOfVertices];
		Arrays.fill(treeVertexUsed, false);
		this.colorForEachVertex = new int[graphNumberOfVertices];
		this.graphUsedColors = new HashSet<Integer>();
		this.treeUsedColors = new HashSet<Integer>();
		this.treeReachableFragments = new HashMap<Integer, HashSet<Integer>>();
		this.treeVertexID = new int[graphNumberOfVertices];
		Arrays.fill(treeVertexID, -1);


		/*
		 * assignment
		 */
		for (Fragment f : graph) {
			colorForEachVertex[f.getVertexId()] = f.getColor();
			graphUsedColors.add(f.getColor());
		}

		/*
		 * create a new tree with the child of the graph's pseudo-root
		 */
		this.tree = new FTree(this.graph.getRoot().getChildren(0).getFormula(), this.graph.getRoot().getChildren(0).getIonization());
		int treeNewRootColor = graph.getRoot().getChildren(0).getColor();
		int treeRootID = this.graph.getRoot().getChildren(0).getVertexId();

		this.tree.getRoot().setColor(treeNewRootColor);

		this.treeVertexUsed[treeRootID] = true;
		this.treeUsedColors.add(treeNewRootColor);

		for (Fragment child : this.graph.getRoot().getChildren(0).getChildren()) {
			int childID = child.getVertexId();
			HashSet<Integer> parentList = new HashSet<Integer>();
			parentList.add(treeRootID);
			treeReachableFragments.put(childID, parentList);
		}
		this.treeVertexID[treeRootID] = 0;
	}

	public FTree solveWithScore() {
		FTree tree = solve();
		double score = graph.getRoot().getOutgoingEdge(0).getWeight();
		for (Fragment f : tree) {
			if (!f.isRoot())
				score += f.getIncomingEdge().getWeight();
		}
		tree.setTreeWeight(score);
		return tree;
	}

	/**
	 * builds the new {@link FTree}
	 * 
	 * @return the new computed {@link FTree}
	 */
	public abstract FTree solve();
	
	/**
	 * adds the flags that are useful for the heuristic
	 * 
	 * @param fragmentID
	 */
	protected abstract void addFlags(int fragmentID);

	/**
	 * adds a new slack {@link Fragment} with its parent and the corresponding
	 * {@link Loss}
	 * 
	 * @param loss
	 *            {@link Loss} from source to target
	 */
	protected void addTreeFragment(Loss loss) {
		Fragment source = loss.getSource();
		Fragment target = loss.getTarget();
		int sourceID = source.getVertexId();
		int targetID = target.getVertexId();

		Fragment scr = this.tree.getFragmentAt(this.treeVertexID[sourceID]);
		Fragment newFragment = this.tree.addFragment(scr,
				target);

		newFragment.getIncomingEdge().setWeight(loss.getWeight());
		newFragment.setColor(target.getColor());

		this.treeVertexID[targetID] = newFragment.getVertexId();
		
		addFlags(source.getVertexId());
		addFlags(target.getVertexId());
	}

	/**
	 * removes pendant losses with negative weight
	 */
	protected void rde() {
		int[] outdeg = new int[this.tree.numberOfVertices()];
		for (Fragment frag : this.tree.getFragments()) {
			outdeg[frag.getVertexId()] = frag.getOutDegree();
		}

		Iterator<Fragment> postOrderIterator = this.tree.postOrderIterator();
		ArrayList<Fragment> toDelete = new ArrayList<Fragment>();

		while (postOrderIterator.hasNext()) {
			Fragment frag = postOrderIterator.next();

			if (outdeg[frag.getVertexId()] == 0
					&& this.tree.getLoss(frag.getParent(), frag).getWeight() < 0) {

				outdeg[frag.getParent().getVertexId()] = outdeg[frag
						.getParent().getVertexId()] - 1;

				toDelete.add(frag);
			}
		}

		for (Fragment fragment : toDelete) {
			this.treeVertexUsed[fragment.getVertexId()] = false;
			this.tree.deleteVertex(fragment);
		}
	}

	/**
	 * Sums up the weights for each subtree and deletes the subtree, if
	 * the sum is negative.
	 */
	protected void rds() {
		// initializes a new array for the scores
		double [] subtreeScore = new double[tree.getFragments().size()];

		int[] outdeg = new int[this.tree.numberOfVertices()];
		for (Fragment frag : this.tree.getFragments()) {
			outdeg[frag.getVertexId()] = frag.getOutDegree();
		}

		Iterator<Fragment> postOrderIterator = this.tree.postOrderIterator();
		HashSet<Fragment> toDelete = new HashSet<Fragment>();

		while (postOrderIterator.hasNext()) {
			Fragment currentFragment = postOrderIterator.next();

			if (outdeg[currentFragment.getVertexId()] == 0) {
				subtreeScore[currentFragment.getVertexId()] = 0;
			} else {
				int currFragId = currentFragment.getVertexId();

				for (Loss outLoss : currentFragment.getOutgoingEdges()) {
					int childId = outLoss.getTarget().getVertexId();
					subtreeScore[currFragId] = subtreeScore[currFragId]
							+ outLoss.getWeight()
							+ subtreeScore[childId];
				}
				if (subtreeScore[currFragId] < 0) {

					Iterator<Fragment> subtreeIterator = this.tree
							.postOrderIterator(currentFragment);
					while (subtreeIterator.hasNext()) {
						Fragment subtreeFrag = subtreeIterator.next();

						if (subtreeFrag.equals(currentFragment)) {
							outdeg[subtreeFrag.getVertexId()] = 0;
							subtreeScore[subtreeFrag.getVertexId()] = 0;
							break;
						}
						toDelete.add(subtreeFrag);
						outdeg[subtreeFrag.getParent().getVertexId()] = outdeg[subtreeFrag
								.getParent().getVertexId()] - 1;
						subtreeScore[subtreeFrag.getVertexId()] = 0;
					}
				}
			}
		}

		for (Fragment fragment : toDelete) {
			this.treeVertexUsed[fragment.getVertexId()] = false;
			this.tree.deleteVertex(fragment);
		}
	}

}
