
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

import java.io.Serializable;
import java.util.Vector;

@SuppressWarnings("serial")
public class Node<NodeType extends Node<NodeType, EdgeType>, EdgeType extends Edge<EdgeType, NodeType>> implements Serializable {
	protected int id;
	protected String label;
	protected Vector<EdgeType> inEdges;
	protected Vector<EdgeType> outEdges;
	protected Vector<NodeType> parents;
	protected Vector<NodeType> children;
	
	public Node() {
		inEdges = new Vector<EdgeType>();
		outEdges = new Vector<EdgeType>();
		parents = new Vector<NodeType>();
		children = new Vector<NodeType>();
	}

	public Node(String label) {
		this();
		this.label = label;
	}
	
	public void setContent(NodeType node) {
		this.id = node.getId();
		this.label = node.getLabel();
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
	
	public void setInEdges(Vector<EdgeType> inEdges) {
		this.inEdges = inEdges;
	}
	
	public Vector<EdgeType> getInEdges() {
		return inEdges;
	}

	public void setOutEdges(Vector<EdgeType> outEdges) {
		this.outEdges = outEdges;
	}
	
	public Vector<EdgeType> getOutEdges() {
		return outEdges;
	}

	public void setParents(Vector<NodeType> parents) {
		this.parents = parents;
	}
	
	public Vector<NodeType> getParents() {
		return parents;
	}

	public void setChildren(Vector<NodeType> children) {
		this.children = children;
	}
	
	public Vector<NodeType> getChildren() {
		return children;
	}
	
/* 
 * end of getter and setter methods
*/			
	
	public void addInEdge(EdgeType inEdge) {
		inEdges.add(inEdge);
		parents.add(inEdge.getFromNode());
	}

	public void addOutEdge(EdgeType outEdge) {
		outEdges.add(outEdge);
		children.add(outEdge.getToNode());
	}

	public void removeInEdge(EdgeType inEdge) {
		inEdges.remove(inEdge);	
		parents.remove(inEdge.getFromNode());
	}

	public void removeOutEdge(EdgeType outEdge) {
		outEdges.remove(outEdge);		
		children.remove(outEdge.getToNode());
	}

	public int numParents() {
		return inEdges.size();
	}

	public int numChildren() {
		return outEdges.size();
	}

	public String dotParams() {
		return "";
	}
	
	public void detach() {
		inEdges.clear();
		outEdges.clear();
		parents.clear();
		children.clear();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Node<NodeType, EdgeType> clone() {
		Node<NodeType, EdgeType> clonedNode = new Node<NodeType, EdgeType>(new String(label));
		clonedNode.setInEdges((Vector<EdgeType>) inEdges.clone());
		clonedNode.setOutEdges((Vector<EdgeType>) outEdges.clone());
		clonedNode.setChildren((Vector<NodeType>) children.clone());
		clonedNode.setParents((Vector<NodeType>) parents.clone());
		return clonedNode;
	}
}
