
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

@SuppressWarnings("serial")
public class TreeNode<NodeType extends TreeNode<NodeType, EdgeType>, EdgeType extends TreeEdge<EdgeType, NodeType>> extends Node<NodeType, EdgeType>{
	protected int postOrderPos;

	public TreeNode() {
		super();
	}
	
	public TreeNode(String label) {
		super(label);
	}

	@Override
	public void setContent(NodeType node) {
		super.setContent(node);
		this.postOrderPos = node.getPostOrderPos();
	}
	
	public int getPostOrderPos() {
		return postOrderPos;
	}

	public void setPostOrderPos(int postOrderPos) {
		this.postOrderPos = postOrderPos;
	}

	public EdgeType getInEdge() {
		if (this.numParents() == 0) return null;
		else return this.getInEdges().get(0);
	}
	
	public NodeType getParent() {
		if (this.numParents() == 0) return null;
		else return this.getParents().get(0);
	}
	
	@Override
	public TreeNode<NodeType, EdgeType> clone() {
		TreeNode<NodeType, EdgeType> clonedTreeNode = new TreeNode<NodeType, EdgeType>(new String(label));
		clonedTreeNode.setPostOrderPos(postOrderPos);
		return clonedTreeNode;
	}

}
