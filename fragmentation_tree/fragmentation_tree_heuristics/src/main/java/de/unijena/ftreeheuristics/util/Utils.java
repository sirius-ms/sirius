package de.unijena.ftreeheuristics.util;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.Iterator;

public class Utils {

	public String lossToString(String name, Loss currentLoss) {
		return name + ": from: " + currentLoss.getSource().getVertexId() + " ["
				+ currentLoss.getSource().getColor() + ", "
				+ currentLoss.getSource().getFormula() + "] " + " to: "
				+ currentLoss.getTarget().getVertexId() + " ["
				+ currentLoss.getTarget().getColor() + ", "
				+ currentLoss.getTarget().getFormula() + "] " + " name: "
				+ currentLoss.toString() + " weight: "
				+ currentLoss.getWeight();
	}

	public String graphToString(FGraph graph) {
		StringBuilder sb = new StringBuilder(); // the fragment has a parent if
												// the fragment in the tree has
												// indegree greater than null

		sb.append("########################## new part ###################\n");
		sb.append("-----------------------Graph---------------------\n");
		sb.append("# losses: " + graph.numberOfEdges() + "\n");
		// sb.append("root: " + tree.getRoot().getVertexId() + " " +
		// tree.getRoot() + " color: " + tree.getRoot().getColor()+ "\n");
		//
		for (Loss l : graph.losses()) {
			sb.append("from: " + l.getSource().getVertexId() + " ["
					+ l.getSource().getColor() + ", "
					+ l.getSource().getFormula() + "] " + " to: "
					+ l.getTarget().getVertexId() + " ["
					+ l.getTarget().getColor() + ", "
					+ l.getTarget().getFormula() + "] " + " name: "
					+ l.toString() + " weigth: " + l.getWeight() + "\n");
		}
		// for (Fragment fragment : tree.getFragments()) {
		// sb.append(fragment + "\t\tid: " + fragment.getVertexId() +
		// "\tcolor: " + fragment.getColor());
		// for (Loss l : fragment.getIncomingEdges()) {
		// sb.append("\t" + l.getWeight());
		// }
		// sb.append("\n");
		// }
		sb.append(graph.getFragments() + "\n");
		sb.append("-----------------------graph END---------------------");
		return sb.toString();
	}

	public String treeToString(FTree t) {
		StringBuilder sb = new StringBuilder(); // the fragment has a parent if
												// the fragment in the tree has
												// indegree greater than null

		sb.append("########################## new part ###################\n");
		sb.append("-----------------------Tree---------------------\n");
		sb.append("# losses: " + t.numberOfEdges() + " vetr: "
				+ t.numberOfVertices() + "\n");
		// sb.append("root: " + tree.getRoot().getVertexId() + " " +
		// tree.getRoot() + " color: " + tree.getRoot().getColor()+ "\n");
		//
		for (Loss l : t.losses()) {
			sb.append("from: " + l.getSource().getVertexId() + " ["
					+ l.getSource().getColor() + ", "
					+ l.getSource().getFormula() + "] " + " to: "
					+ l.getTarget().getVertexId() + " ["
					+ l.getTarget().getColor() + ", "
					+ l.getTarget().getFormula() + "] " + " name: "
					+ l.toString() + " weigth: " + l.getWeight() + "\n");
		}
		// for (Fragment fragment : tree.getFragments()) {
		// sb.append(fragment + "\t\tid: " + fragment.getVertexId() +
		// "\tcolor: " + fragment.getColor());
		// for (Loss l : fragment.getIncomingEdges()) {
		// sb.append("\t" + l.getWeight());
		// }
		// sb.append("\n");
		// }
		sb.append(t.getFragments() + "\n");
		sb.append("-----------------------Tree END---------------------");
		return sb.toString();
	}
	
	protected void postOrderGraph(FGraph graph, String where) {
		StringBuilder sb = new StringBuilder();
		sb.append(where + "\n");
		Iterator<Fragment> iterBlaz = graph.postOrderIterator();
		while (iterBlaz.hasNext()) {
			Fragment frag = iterBlaz.next();
			sb.append(frag.getVertexId() + " / " + frag.getColor());
			for (Loss l : frag.getIncomingEdges()) {
				sb.append(" " + l.getWeight());
			}
			sb.append("\n");
		}
		sb.append(where + "\n");
		System.out.println(sb.toString());
	}

	protected void postOrderTree(FTree tree, String where) {
		StringBuilder sb = new StringBuilder();
		sb.append(where + "\n");
		Iterator<Fragment> iterBlaz = tree.postOrderIterator();
		while (iterBlaz.hasNext()) {
			Fragment frag = iterBlaz.next();
			sb.append(frag.getVertexId() + " / " + frag.getColor());
			for (Loss l : frag.getIncomingEdges()) {
				sb.append(" " + l.getWeight());
			}
			sb.append("\n");
		}
		sb.append(where + "\n");
		System.out.println(sb.toString());
	}

	protected void preOrderGraph(FGraph graph, String where) {
		StringBuilder sb = new StringBuilder();
		sb.append(where + "\n");
		Iterator<Fragment> iterBlaz = graph.preOrderIterator();
		while (iterBlaz.hasNext()) {
			Fragment frag = iterBlaz.next();
			sb.append(frag.getVertexId() + " / " + frag.getColor());
			for (Loss l : frag.getIncomingEdges()) {
				sb.append(" " + l.getWeight());
			}
			sb.append("\n");
		}
		sb.append(where + "\n");
		System.out.println(sb.toString());
	}
}
