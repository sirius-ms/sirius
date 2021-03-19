
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package fragtreealigner.domainobjects;

import fragtreealigner.domainobjects.graphs.*;
import fragtreealigner.util.Session;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class Alignment implements Serializable {
	private AlignmentTree tree1;
	private AlignmentTree tree2;
	private AlignmentResTree alignmentResult;
	private float score;
	private List<Float> scoreList;
	private float pLikeValue;
	private int numPullUps;
	private AlignmentTreeNode pullUpNode;
	private AlignmentTreeNode pullUpParent;
	private AlignmentTree pullUpTree;
	private Map<String, List<String>> nodeRelationTree1Tree2;
	private Map<String, List<String>> nodeRelationTree2Tree1;
	private Session session;
	
	private static String[] colorList = { "#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", 
		"#e31a1c", "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99", "#b15928" };
	private final static int NUM_COLORS = colorList.length; 
			 	
	public Alignment(Session session) {
		this.session = session;
	}
	
	public Alignment(AlignmentTree tree1, AlignmentTree tree2, AlignmentResTree alignmentResult, float score, List<Float> scoreList, float pLikeValue, int numPullUps, Session session) {
		super();
		this.tree1 = tree1;
		this.tree2 = tree2;
		this.alignmentResult = alignmentResult;
		this.score = score;
		this.scoreList = scoreList;
		this.pLikeValue = pLikeValue;
		this.numPullUps = numPullUps;
		this.session = session;
		nodeRelationTree1Tree2 = new HashMap<String, List<String>>();
		nodeRelationTree2Tree1 = new HashMap<String, List<String>>();
		if (alignmentResult != null) {
			computeNodeRelations();
		}
	}

	public AlignmentTree getTree1() {
		return tree1;
	}

	public AlignmentTree getTree2() {
		return tree2;
	}

	public AlignmentResTree getAlignmentResult() {
		return alignmentResult;
	}

	public float getScore() {
		return score;
	}
	
	public List<Float> getScoreList() {
		return scoreList;
	}
	
	public float getPlikeValue() {
		return pLikeValue;
	}
	
	public float getNumPullUps() {
		return numPullUps;
	}
	
	public void setPullUpNode(AlignmentTreeNode pullUpNode) {
		this.pullUpNode = pullUpNode;
	}

	public AlignmentTreeNode getPullUpNode() {
		return pullUpNode;
	}

	public void setPullUpParent(AlignmentTreeNode pullUpParent) {
		this.pullUpParent = pullUpParent;
	}

	public AlignmentTreeNode getPullUpParent() {
		return pullUpParent;
	}

	public void setPullUpTree(AlignmentTree pullUpTree) {
		this.pullUpTree = pullUpTree;
	}

	public AlignmentTree getPullUpTree() {
		return pullUpTree;
	}

	public Session getSession() {
		return session;
	}
	
	public List<String> getRelatedNodesInTree2(String nodeInGraph1) {
		return nodeRelationTree1Tree2.get(nodeInGraph1);
	}

	public List<String> getRelatedNodesInTree1(String nodeInGraph2) {
		return nodeRelationTree2Tree1.get(nodeInGraph2);
	}

	private void computeNodeRelations() {
		String label1, label2;
		for (AlignmentResTreeNode node : this.alignmentResult.getNodes()) {
			if (node.getCorrespondingNode1() != null && node.getCorrespondingNode2() != null) {
				label1 = node.getCorrespondingNode1().getLabel();
				label2 = node.getCorrespondingNode2().getLabel();
				if (nodeRelationTree1Tree2.get(label1) == null) nodeRelationTree1Tree2.put(label1, new LinkedList<String>());
				nodeRelationTree1Tree2.get(label1).add(label2);
				if (nodeRelationTree2Tree1.get(label2) == null) nodeRelationTree2Tree1.put(label2, new LinkedList<String>());
				nodeRelationTree2Tree1.get(label2).add(label1);
				if (node.getParent() != null && node.getParent().getFlag() == 'c') {
					if (node.getParent().getCorrespondingNode1() != null) {
						label1 = node.getParent().getCorrespondingNode1().getLabel();
						if (nodeRelationTree1Tree2.get(label1) == null) nodeRelationTree1Tree2.put(label1, new LinkedList<String>());
						nodeRelationTree1Tree2.get(label1).add(label2);						
						nodeRelationTree2Tree1.get(label2).add(label1);
					} else {
						label2 = node.getParent().getCorrespondingNode2().getLabel();
						nodeRelationTree1Tree2.get(label1).add(label2);						
						if (nodeRelationTree2Tree1.get(label2) == null) nodeRelationTree2Tree1.put(label2, new LinkedList<String>());
						nodeRelationTree2Tree1.get(label2).add(label1);						
					}
				}
			}
		}
	}
	
	public void writeToDot(BufferedWriter writer) throws IOException {
		writeToDot(writer, true, true);
	}
		
	public void writeToDot(BufferedWriter writer, boolean includeGraph1, boolean includeGraph2) throws IOException {
		ListIterator<AlignmentTreeEdge> edgeIt;
		ListIterator<AlignmentTreeNode> nodeIt;
		AlignmentTree tree;
		AlignmentTreeEdge edge;
		AlignmentTreeNode node;
		String label, prefix;

		List<AlignmentTreeEdge> edges;
		List<AlignmentTreeNode> nodes;
		StringBuffer dotNodeList1 = new StringBuffer("");
		StringBuffer dotNodeList2 = new StringBuffer("");
		String dotNodeList = "";
		buildDotNodeLists(alignmentResult.getRoot(), dotNodeList1, dotNodeList2, 0, 0);

		writer.write("digraph G {\n");
		for (int j = 0; j < 2; j++) {
			if (j == 0 && !includeGraph1) continue;
			if (j == 1 && !includeGraph2) continue;
			tree = (j == 0) ? tree1 : tree2;
			nodes = (j == 0) ? tree1.getNodes() : tree2.getNodes();
			edges = (j == 0) ? tree1.getEdges() : tree2.getEdges();
			dotNodeList = (j == 0) ? dotNodeList1.toString() : dotNodeList2.toString();
			prefix = (j == 0) ? "a" : "b";
			if (includeGraph1 && includeGraph2) writer.write("\tsubgraph sg" + j + " {\n");
			writer.write("\tnode [style=filled, colorscheme=paired12];\n");
			for (edgeIt = edges.listIterator(); edgeIt.hasNext();) {
				edge = edgeIt.next();
                if (dotNodeList.contains(edge.getFromNode().toString()) && dotNodeList.contains(edge.getToNode().toString())) {
                    label = (edge.getLabel() == null) ? "" : " [label=\"" + edge.getLabel() + "\"]";
                    writer.write("\t" + prefix + edge.getFromNode().getLabel() + " -> " + prefix + edge.getToNode().getLabel() + label + ";\n");

                }

			}
			if (pullUpTree == tree) {
				writer.write("\t" + prefix + pullUpParent.getLabel() + " -> " + prefix + pullUpNode.getLabel() + "[style=solid,color=\"gray\"]" + ";\n");
			}
            //write nodes not contained in alignment
			for (nodeIt = nodes.listIterator(); nodeIt.hasNext();) {
				node = nodeIt.next();
				writer.write("\t" + prefix + node.getLabel() + " [label=\"" + node + "\", fillcolor=\"#ffffff\", color=\"#999999\", fontcolor=\"#999999\"];\n");
			}
            //write nodes contained in alignment
			writer.write(dotNodeList);
			if (includeGraph1 && includeGraph2) writer.write("\t}\n\n");
		}
		writer.write("}");
		writer.close();
	}

//    public void writeToDot(BufferedWriter writer, boolean includeGraph1, boolean includeGraph2) throws IOException {
//		ListIterator<AlignmentTreeEdge> edgeIt;
//		ListIterator<AlignmentTreeNode> nodeIt;
//		AlignmentTree tree;
//		AlignmentTreeEdge edge;
//		AlignmentTreeNode node;
//		String label, prefix;
//
//		List<AlignmentTreeEdge> edges;
//		List<AlignmentTreeNode> nodes;
//		StringBuffer dotNodeList1 = new StringBuffer("");
//		StringBuffer dotNodeList2 = new StringBuffer("");
//		String dotNodeList = "";
//		buildDotNodeLists(alignmentResult.getRoot(), dotNodeList1, dotNodeList2, 0, 0);
//
//		writer.write("digraph G {\n");
//		for (int j = 0; j < 2; j++) {
//			if (j == 0 && !includeGraph1) continue;
//			if (j == 1 && !includeGraph2) continue;
//			tree = (j == 0) ? tree1 : tree2;
//			nodes = (j == 0) ? tree1.getNodes() : tree2.getNodes();
//			edges = (j == 0) ? tree1.getEdges() : tree2.getEdges();
//			dotNodeList = (j == 0) ? dotNodeList1.toString() : dotNodeList2.toString();
//			prefix = (j == 0) ? "a" : "b";
//			if (includeGraph1 && includeGraph2) writer.write("\tsubgraph sg" + j + " {\n");
//			//writer.write("\tnode [style=filled, colorscheme=paired12];\n");
//			for (edgeIt = edges.listIterator(); edgeIt.hasNext();) {
//				edge = edgeIt.next();
//                if (dotNodeList.contains(edge.getFromNode().toString()) && dotNodeList.contains(edge.getToNode().toString())) {
//                    label = (edge.getLabel() == null) ? "" : " [label=\"" + edge.getLabel() + "\"]";
//                    writer.write("\t" + prefix + edge.getFromNode().getLabel() + " -> " + prefix + edge.getToNode().getLabel() + label + ";\n");
//
//                }
//
//			}
//			if (pullUpTree == tree) {
//				writer.write("\t" + prefix + pullUpParent.getLabel() + " -> " + prefix + pullUpNode.getLabel() + "[style=solid,color=\"gray\"]" + ";\n");
//			}
//            //write nodes not contained in alignment
////			for (nodeIt = nodes.listIterator(); nodeIt.hasNext();) {
////				node = nodeIt.next();
////				writer.write("\t" + prefix + node.getLabel() + " [label=\"" + node + "\"];\n");
////                        //, fillcolor=\"#ffffff\", color=\"#999999\", fontcolor=\"#999999\"];\n");
////			}
//            //write nodes contained in alignment
//			writer.write(dotNodeList);
//			if (includeGraph1 && includeGraph2) writer.write("\t}\n\n");
//		}
//		writer.write("}");
//		writer.close();
//	}
	
	private int buildDotNodeLists(AlignmentResTreeNode node, StringBuffer dotNodeList1, StringBuffer dotNodeList2, int color, int maxColor) {
		String dotNodeParams = ", color=\"#000000\", fontcolor=\"#000000\"";
		String dotColor;
		if ((node.getCorrespondingNode1() != null) && (node.getCorrespondingNode2() != null) && (node != alignmentResult.getRoot())) {
//			dotColor = Integer.toString(color);
			dotColor = "\"" + colorList[(color-1)%NUM_COLORS] + "\"";
			if (color > maxColor) maxColor = color;
		} else if (node.getFlag() == 'c') {
			color = maxColor + 1;
//			dotColor = Integer.toString(color);
			dotColor = "\"" + colorList[(color-1)%NUM_COLORS] + "\"";
			if (color > maxColor) maxColor = color;	
			dotNodeParams += ", style=\"filled,dashed\"";
		} else {
			dotColor = "\"#ffffff\"";
			color = maxColor + 1;
		}
		if (node.getCorrespondingNode1() != null) {
			dotNodeList1.append("\t" + "a" + node.getCorrespondingNode1().getLabel() + " [label=\"" + node.getCorrespondingNode1() + "\", fillcolor=" + dotColor + dotNodeParams + "];\n");
		}
		if (node.getCorrespondingNode2() != null) {
			dotNodeList2.append("\t" + "b" + node.getCorrespondingNode2().getLabel() + " [label=\"" + node.getCorrespondingNode2() + "\", fillcolor=" + dotColor + dotNodeParams + "];\n");
		}
		for (AlignmentResTreeNode child: node.getChildren()) {
			maxColor = buildDotNodeLists(child, dotNodeList1, dotNodeList2, color, maxColor);
			if ((node.getCorrespondingNode1() == null) || (node.getCorrespondingNode2() == null) || (node == alignmentResult.getRoot())) {
				if (node.getFlag() == 'c') continue;
				color = maxColor + 1;
			}
		}
		return maxColor;
	}

	public void visualize() {
		try {
			this.getAlignmentResult().writeToDot(new BufferedWriter(new FileWriter("output/scores.dot")));
			this.writeToDot(new BufferedWriter(new FileWriter("output/alignment.dot")));
			//Runtime.getRuntime().exec("dot output/scores.dot -Tgif -ooutput/scores.gif");
			//Runtime.getRuntime().exec("dot output/alignment.dot -Tgif -ooutput/alignment.gif");
			Runtime.getRuntime().exec("eog output/scores.gif");
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Runtime.getRuntime().exec("eog output/alignment.gif");
		} catch (IOException e) {
			System.err.println("During the visualization of the alignment the following IOError occured:\n" + e);
		}	
	}

	public void createGraphics(String name1, String name2) {
		String filePrefix = "output/"+name1+"_"+name2;
		System.out.println("cg called "+filePrefix);
		try {
			this.getAlignmentResult().writeToDot(new BufferedWriter(new FileWriter(filePrefix+"Scores.dot")));
			this.writeToDot(new BufferedWriter(new FileWriter(filePrefix+".dot")));
			Runtime.getRuntime().exec("dot "+filePrefix+"Scores.dot -Tgif -o"+filePrefix+"Scores.gif");
			Runtime.getRuntime().exec("dot "+filePrefix+".dot -Tgif -o"+filePrefix+".gif");
		} catch (IOException e) {
			System.err.println("During the visualization of the alignment the following IOError occured:\n" + e);
		}	
		
	}

	public Alignment reverse() {
		Alignment result = new Alignment(tree2, tree1, null, score, scoreList, pLikeValue, numPullUps, session);
		result.nodeRelationTree1Tree2 = nodeRelationTree2Tree1;
		result.nodeRelationTree2Tree1 = nodeRelationTree1Tree2;
		return result;
	}

}
