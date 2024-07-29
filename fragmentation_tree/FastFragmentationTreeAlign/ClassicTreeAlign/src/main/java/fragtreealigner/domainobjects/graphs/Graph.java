
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

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class Graph<NodeType extends Node<NodeType, EdgeType>, EdgeType extends Edge<EdgeType, NodeType>> implements Serializable {
	protected Session session;
	protected String id;
	protected boolean isDirected;
	protected String type;		// TODO zu ersetzen durch property-Objekt oder aehnliches
	protected List<NodeType> nodes;
	protected List<EdgeType> edges;
	protected HashMap<String, NodeType> nodeHash;
	protected double maxEdgeScore;

	public enum RearrangementType { REVERSE, RANDOM }
	
	public Graph(Session session) {
		this.isDirected = true;
		this.nodes    = new LinkedList<NodeType>();
		this.edges    = new LinkedList<EdgeType>();
		this.nodeHash = new HashMap<String, NodeType>();
		this.session  = session;
	}
	
	public Graph(Session session, String id) {
		this(session);
		this.id = id;
	}

	public Session getSession() {
		return session;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}

	public void setDirected(boolean isDirected) {
		this.isDirected = isDirected;
	}
	
	public boolean isDirected() {
		return isDirected;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public List<NodeType> getNodes() {
		return nodes;
	}

	public List<EdgeType> getEdges() {
		return edges;
	}

	public NodeType getNodeByName(String name) {
		return nodeHash.get(name);
	}

	public void setMaxEdgeScore(double maxEdgeScore) {
		this.maxEdgeScore = maxEdgeScore;		
	}
	
	public double getMaxEdgeScore() {
		return maxEdgeScore;
	}

/* 
 * end of getter and setter methods
*/	
	
	public NodeType addNode(String label) {
		System.err.println("Method >addNode(String label)< is not implemented for graph objects.");
		NodeType node = null;
//		this.addNode(label, node);
		return node;
	}

	protected void addNode(NodeType node, String label) {
		node.setId(nodes.size());
		this.nodes.add(node);
		this.nodeHash.put(label, node);	
	}
	
	protected void connect(EdgeType edge) {
		edge.setId(edges.size());
		edge.getFromNode().addOutEdge(edge);
		edge.getToNode().addInEdge(edge);
		this.edges.add(edge);
	}
	
	public EdgeType connect(NodeType parent, NodeType child) {
		System.err.println("Method >connect(NodeType parent, NodeType child)< is not implemented for graph objects.");
//		TODO Check whether edge already exists
//		TODO Check whether edgeToNode has already parent
		EdgeType edge = null;
		this.connect(edge);
		return edge;
	}

	public EdgeType connect(NodeType parent, NodeType child, String label) {
		EdgeType edge = this.connect(parent, child);
		edge.setLabel(label);
		return edge;
	}

	public void disconnect(EdgeType edge) {
		edge.getFromNode().removeOutEdge(edge);
		edge.getToNode().removeInEdge(edge);
		edge.setFromNode(null);
		edge.setToNode(null);
	}
	
	public void reconnect(EdgeType edge, NodeType parent, NodeType child) {
		disconnect(edge);
		edge.setFromNode(parent);
		edge.setToNode(child);
		parent.addOutEdge(edge);
		child.addInEdge(edge);
	}
	
	public int size() {
		return this.nodes.size();
	}
	
	public void clear() {
		nodes.clear();
		edges.clear();
		nodeHash.clear();		
	}

	public Graph<NodeType, EdgeType> rearrangeNodes(RearrangementType rearrangementType, int randSeed) {
		return rearrangeNodes(null, rearrangementType, randSeed);
	}
	
	@SuppressWarnings("unchecked")
	public Graph<NodeType, EdgeType> rearrangeNodes(NodeType excludedNode, RearrangementType rearrangementType, int randSeed) {
//		RearrangementType rearrangementType = session.getParameters().rearrangementType;
		Graph<NodeType, EdgeType> graph = (Graph<NodeType, EdgeType>) this.clone();
		List<NodeType> newNodes = graph.getNodes();
		List<NodeType> oriNodes = (List<NodeType>)((LinkedList<NodeType>)this.getNodes()).clone();
		if (excludedNode != null) oriNodes.remove(excludedNode.getId());
		
		Random randGen = new Random(nodeHash.keySet().hashCode() + randSeed);
		int index;//, i = 0;
		for (NodeType node : newNodes) {
			if ((excludedNode != null) && (node.getId() == excludedNode.getId())) {
//				i = 1;
				continue;
			}
			switch (rearrangementType) {
			case RANDOM:
				index = randGen.nextInt(oriNodes.size());
				break;
			
			case REVERSE:
				index = oriNodes.size() - 1;
				break;
				
			default:
				System.err.println("Unknown rearrangement type.");
				return graph;
			}
			node.setContent(oriNodes.get(index));
			oriNodes.remove(index);
		}
		return graph;
	}

	@SuppressWarnings("unchecked")
	public Graph<NodeType, EdgeType> rearrangeEdges(RearrangementType rearrangementType) {
//		RearrangementType rearrangementType = session.getParameters().rearrangementType;
		Graph<NodeType, EdgeType> graph = (Graph<NodeType, EdgeType>) this.clone();
		List<EdgeType> newEdges = graph.getEdges();
		List<EdgeType> oriEdges = (List<EdgeType>)((LinkedList<EdgeType>)this.getEdges()).clone();
		
		Random randGen = new Random(nodeHash.keySet().hashCode());
		int index;
		for (EdgeType edge : newEdges) {
			switch (rearrangementType) {
			case RANDOM:
				index = randGen.nextInt(oriEdges.size());
				break;
			
			case REVERSE:
				index = oriEdges.size() - 1;
				break;
				
			default:
				System.err.println("Unknown rearrangement type.");
				return graph;
			}
			edge.setContent(oriEdges.get(index));
			oriEdges.remove(index);
		}
		return graph;
	}

	public NodeType parseNode(String label, String parameters) {
		return addNode(label);
	}

	public EdgeType parseEdge(String edgeFrom, String edgeTo, String parameters) {
		NodeType parent = getNodeByName(edgeFrom);
		NodeType child = getNodeByName(edgeTo);
		String[] param = parameters.split(" ");
		if (!parameters.equalsIgnoreCase("")) return connect(parent, child, param[0]);
		else return connect(parent, child);
	}

	//TODO convert readFromList() to static method
	public void readFromList(BufferedReader reader) throws IOException {
		Pattern pNode = Pattern.compile( "^([0-9a-zA-Z_-]*):? ?(.*)" ); 
		Pattern pEdge = Pattern.compile( "^([0-9a-zA-Z_-]*) -> ([0-9A-Z_-]*):? ?(.*)" );
		Matcher mNode, mEdge;
		boolean nodeMatchFound, edgeMatchFound;
		String line, nodeLabel, edgeFrom, edgeTo, parameters;
	
		while (reader.ready()) {
			line = reader.readLine();
			mNode = pNode.matcher( line );
			mEdge = pEdge.matcher( line );
			nodeMatchFound = mNode.find();
			edgeMatchFound = mEdge.find();
			if (edgeMatchFound) {
				edgeFrom = mEdge.group(1);
				edgeTo = mEdge.group(2);
				parameters = mEdge.group(3);
				parseEdge(edgeFrom, edgeTo, parameters);
			} else if (nodeMatchFound) {
				nodeLabel = mNode.group(1);
				parameters = mNode.group(2);
				parseNode(nodeLabel, parameters);
			}
		}
	}

	public void writeToDot(BufferedWriter writer) throws IOException {
		ListIterator<NodeType> nodeIt;
		ListIterator<EdgeType> edgeIt;
		NodeType node;
		EdgeType edge;
		String label;
		int i = 0;
		writer.write(isDirected() ? "digraph G {\n" : "graph G {\n");
		for (nodeIt = nodes.listIterator(); nodeIt.hasNext();) {
			node = nodeIt.next();
			writer.write("\t" + node.getLabel() + " [label=\"" + node + "\"" + node.dotParams() + "];\n");
			i++;
		}
	//	writer.write("\t" + getRoot().getLabel() + " [label=\"" + getRoot() + "\", style=dashed];\n");

		i = 0;
		for (edgeIt = edges.listIterator(); edgeIt.hasNext();) {
			edge = edgeIt.next();
			label = " [label=\"" + ((edge.getLabel() == null) ? "" : edge.getLabel()) + "\"" + edge.dotParams() + "]";
//			label = (edge.getLabel() == null) ? "" : " [label=\"" + edge.getLabel() + "\"]";
			writer.write("\t" + edge.getFromNode().getLabel() + (isDirected() ? "->" : " -- ") + edge.getToNode().getLabel() + label + ";\n");
			i++;
		}
		writer.write("}");
		writer.close();
	}

//    public void writeToDot(BufferedWriter writer) throws IOException {
//		ListIterator<NodeType> nodeIt;
//		ListIterator<EdgeType> edgeIt;
//		NodeType node;
//		EdgeType edge;
//		String label;
//		int i = 0;
//		writer.write(isDirected() ? "digraph G {\n" : "graph G {\n");
//		for (nodeIt = nodes.listIterator(); nodeIt.hasNext();) {
//			node = nodeIt.next();
//			writer.write("\t" + node.getLabel() + " [label=\"" + node + "\"" + node.dotParams() + "];\n");
//			i++;
//		}
//	//	writer.write("\t" + getRoot().getLabel() + " [label=\"" + getRoot() + "\", style=dashed];\n");
//
//		i = 0;
//		for (edgeIt = edges.listIterator(); edgeIt.hasNext();) {
//			edge = edgeIt.next();
//			label = " [label=\"" + ((edge.getLabel() == null) ? "" : edge.getLabel()) + "\"" + edge.dotParams() + "]";
////			label = (edge.getLabel() == null) ? "" : " [label=\"" + edge.getLabel() + "\"]";
//			writer.write("\t" + edge.getFromNode().getLabel() + (isDirected() ? "->" : " -- ") + edge.getToNode().getLabel() + label + ";\n");
//			i++;
//		}
//		writer.write("}");
//		writer.close();
//	}
	
	public void visualize() {
		visualize("dot");
	}
	
	public void visualize(String algo) {
		try {
			this.writeToDot(new BufferedWriter(new FileWriter("output/graph.dot")));
			Runtime.getRuntime().exec(algo + " output/graph.dot -Tgif -ooutput/graph.gif");
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Runtime.getRuntime().exec("eog output/graph.gif");
		} catch (IOException e) {
			System.err.println("During the visualization of the alignment the following IOError occured:\n" + e);
		}	
	}

	public Graph<NodeType, EdgeType> clone() {
		Graph<NodeType, EdgeType> clonedGraph = new Graph<NodeType, EdgeType>(session);
		buildUpClonedGraph(clonedGraph);
		return clonedGraph;		
	}
	
	@SuppressWarnings("unchecked")
	protected void buildUpClonedGraph(Graph<NodeType, EdgeType> clonedGraph) {
		clonedGraph.setDirected(isDirected);
		if (id != null) clonedGraph.setId(new String(id));
		if (type != null) clonedGraph.setType(new String(type));

		NodeType clonedNode = null;
		for (NodeType node : nodes) {
			clonedNode = (NodeType) node.clone();
			clonedNode.detach();
			clonedGraph.addNode((NodeType)clonedNode, new String (clonedNode.getLabel()));
		}
		
		EdgeType clonedEdge;
		for (EdgeType edge : edges) {
			clonedEdge = (EdgeType)edge.clone();
			clonedEdge.setFromNode(clonedGraph.getNodeByName(edge.getFromNode().getLabel()));
			clonedEdge.setToNode(clonedGraph.getNodeByName(edge.getToNode().getLabel()));
			clonedGraph.connect(clonedEdge);
		}
	}
}
