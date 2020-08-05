
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
