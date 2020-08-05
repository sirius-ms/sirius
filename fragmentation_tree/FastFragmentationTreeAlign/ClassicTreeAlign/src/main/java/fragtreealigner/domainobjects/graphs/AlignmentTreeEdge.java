
package fragtreealigner.domainobjects.graphs;

@SuppressWarnings("serial")
public class AlignmentTreeEdge extends TreeEdge<AlignmentTreeEdge, AlignmentTreeNode> {
	public AlignmentTreeEdge(AlignmentTreeNode fromNode, AlignmentTreeNode toNode) {
		super(fromNode, toNode);
	}

	public AlignmentTreeEdge(AlignmentTreeNode fromNode, AlignmentTreeNode toNode, String label) {
		super(fromNode, toNode, label);
	}
	
	@Override
	public AlignmentTreeEdge clone() {
		AlignmentTreeEdge clonedAligTreeEdge = new AlignmentTreeEdge(fromNode, toNode);
		if (label != null) clonedAligTreeEdge.setLabel(new String(label));
		return clonedAligTreeEdge;
	}
	
	public void setContent(AlignmentTreeEdge edge) {
		super.setContent(edge);
	}
}
