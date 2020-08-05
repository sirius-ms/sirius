
package fragtreealigner.domainobjects.graphs;

@SuppressWarnings("serial")
public class AlignmentResTreeEdge extends TreeEdge<AlignmentResTreeEdge, AlignmentResTreeNode> {
	public AlignmentResTreeEdge(AlignmentResTreeNode fromNode, AlignmentResTreeNode toNode) {
		super(fromNode, toNode);
	}

	public AlignmentResTreeEdge(AlignmentResTreeNode fromNode, AlignmentResTreeNode toNode, String label) {
		super(fromNode, toNode, label);
	}
	
	@Override
	public AlignmentResTreeEdge clone() {
		AlignmentResTreeEdge clonedAligResTreeEdge = new AlignmentResTreeEdge(fromNode, toNode, new String(label));
		return clonedAligResTreeEdge;
	}

	public void setContent(AlignmentResTreeEdge edge) {
		super.setContent(edge);
	}
}
