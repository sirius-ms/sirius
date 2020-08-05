
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
