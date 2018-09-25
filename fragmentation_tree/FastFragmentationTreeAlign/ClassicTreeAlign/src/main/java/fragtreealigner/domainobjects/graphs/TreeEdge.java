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

@SuppressWarnings("serial")
public class TreeEdge<EdgeType extends TreeEdge<EdgeType, NodeType>, NodeType extends TreeNode<NodeType, EdgeType>>  extends Edge<EdgeType, NodeType>{
	public TreeEdge() {}
	
	public TreeEdge(NodeType fromNode, NodeType toNode) {
		super(fromNode, toNode);
	}

	public TreeEdge(NodeType fromNode, NodeType toNode, String label) {
		super(fromNode, toNode, label);
	}
	
	@Override
	public void setContent(EdgeType edge) {
		super.setContent(edge);
	}
	
	@Override
	public TreeEdge<EdgeType, NodeType> clone() {
		TreeEdge<EdgeType, NodeType> clonedEdge = new TreeEdge<EdgeType, NodeType>(fromNode, toNode, new String(label));
		return clonedEdge;
	}
}
