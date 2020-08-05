
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
