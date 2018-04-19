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

import java.io.Serializable;

@SuppressWarnings("serial")
public class Edge<EdgeType extends Edge<EdgeType, NodeType>, NodeType extends Node<NodeType, ?>> implements Serializable {
	protected int id;
	protected String label;
	protected NodeType fromNode;
	protected NodeType toNode;

	public Edge() {}
	
	public Edge(NodeType fromNode, NodeType toNode) {
		this.fromNode = fromNode;
		this.toNode = toNode;
	}

	public Edge(NodeType fromNode, NodeType toNode, String label) {
		this.label = label;
		this.fromNode = fromNode;
		this.toNode = toNode;
	}
	
	public void setContent(EdgeType edge) {
		this.id = edge.getId();
		this.label = edge.getLabel();
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setFromNode(NodeType fromNode) {
		this.fromNode = fromNode;
	}

	public NodeType getFromNode() {
		return fromNode;
	}
	
	public void setToNode(NodeType toNode) {
		this.toNode = toNode;
	}

	public NodeType getToNode() {
		return toNode;
	}
	
	public String dotParams() {
		return "";
	}
	
	@Override
	public Edge<EdgeType, NodeType> clone() {
		Edge<EdgeType, NodeType> clonedEdge = new Edge<EdgeType, NodeType>(fromNode, toNode, new String(label));
		return clonedEdge;
	}
}
