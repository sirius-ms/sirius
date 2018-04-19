/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package fragtreealigner.domainobjects.graphs;

import fragtreealigner.util.Session;

@SuppressWarnings("serial")
public class AlignmentResTree extends Tree<AlignmentResTreeNode, AlignmentResTreeEdge> {
	public AlignmentResTree(Session session) {
		super(session);
	}
	
	@Override
	public AlignmentResTreeNode addNode(String label) {
		AlignmentResTreeNode node = new AlignmentResTreeNode(label);
		super.addNode(node, label);
		return node;
	}
	
	public AlignmentResTreeNode addNode(String label, AlignmentTreeNode node1, AlignmentTreeNode node2, float score) {
		AlignmentResTreeNode node = new AlignmentResTreeNode(label, node1, node2, score);
		super.addNode(node, label);
		return node;	
	}

	public AlignmentResTreeNode addNode(String label, AlignmentTreeNode node1, AlignmentTreeNode node2, float score, char flag) {
		AlignmentResTreeNode node = new AlignmentResTreeNode(label, node1, node2, score, flag);
		super.addNode(node, label);
		return node;	
	}
	
	@Override
	public AlignmentResTreeEdge connect(AlignmentResTreeNode parent, AlignmentResTreeNode child) {
		AlignmentResTreeEdge edge = new AlignmentResTreeEdge(parent, child);
		super.connect(edge);
		return edge;
	}

	public AlignmentResTreeEdge connect(AlignmentResTreeNode parent, AlignmentResTreeNode child, String label) {
		AlignmentResTreeEdge edge = new AlignmentResTreeEdge(parent, child, label);
		this.connect(edge);
		return edge;
	}
	
	@Override
	public AlignmentResTree clone() {
		AlignmentResTree clonedAligResTree = new AlignmentResTree(session);
		buildUpClonedGraph(clonedAligResTree);
		return clonedAligResTree;
	}

	protected void buildUpClonedGraph(AlignmentResTree clonedGraph) {
		super.buildUpClonedGraph(clonedGraph);
	}
}
