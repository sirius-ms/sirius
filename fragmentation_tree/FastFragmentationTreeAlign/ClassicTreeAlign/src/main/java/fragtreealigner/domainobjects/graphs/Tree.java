
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

package fragtreealigner.domainobjects.graphs;

import fragtreealigner.util.Session;

import java.io.BufferedReader;
import java.io.IOException;

@SuppressWarnings("serial")
public class Tree<NodeType extends TreeNode<NodeType, EdgeType>, EdgeType extends TreeEdge<EdgeType, NodeType>> extends Graph<NodeType, EdgeType> {
	NodeType root;

	public Tree(Session session) {
		super(session);
	}
	
	public Tree(Session session, String id) {
		super(session, id);
	}
	
	public NodeType getRoot() {
		return root;
	}

	public NodeType determineRoot() {
		root = null;
		for ( NodeType node : nodes ) {
			if ( node.numParents() == 0 ) {
				if ( root == null ) root = node;
				else System.err.println("Error: Tree is a forest!");
			}
		}
		return root;
	}
	
	@Override
	public String toString() 
	{
		return getClass().getName() + "@" + Integer.toHexString(hashCode()) + "\n" + this.toNewickNotation(); 
	}

	public String toNewickNotation() {
		return toNewickNotation(root);
	}
	
	public String toNewickNotation(NodeType node) {
		if ( node.numChildren() == 0 ) return node.toString();
		else {
			String out = node.toString() + "( ";
			for ( NodeType child : node.getChildren() ) {
				out += toNewickNotation(child);
				out += " ";
			}
			out += ")";
			return out;
		}
	}

	@Override
	public void clear() {
		root = null;
		super.clear();
	}
	
	public void postOrderNodeList(NodeType[] nodeList) {
		if (nodeList.length != this.size()) System.err.println("Node list has wrong length!");
//		NodeType[] nodeList = new NodeType[size()];
		postOrder(nodeList, root, 0);
	}
	
	private int postOrder(NodeType[] nodeList, NodeType node, int pos) {
		for (NodeType child: node.getChildren()) {
			pos = postOrder(nodeList, child, pos);
		}
		nodeList[pos] = node;
		node.setPostOrderPos(pos + 1);
		return pos + 1;
	}
	
	@Override
	public void readFromList(BufferedReader reader) throws IOException {
		super.readFromList(reader);
		this.determineRoot();
	}
	
	@Override
	public Tree<NodeType, EdgeType> clone() {
		Tree<NodeType, EdgeType> clonedTree = new Tree<NodeType, EdgeType>(session);
		buildUpClonedGraph(clonedTree);
		return clonedTree;
	}
	
	protected void buildUpClonedGraph(Tree<NodeType, EdgeType> clonedGraph) {
		super.buildUpClonedGraph(clonedGraph);
		clonedGraph.determineRoot(); // TODO this could result in a different cloned graph
	}
}
