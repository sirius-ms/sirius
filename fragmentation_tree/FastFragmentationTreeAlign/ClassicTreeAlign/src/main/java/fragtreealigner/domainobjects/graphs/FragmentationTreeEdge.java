
package fragtreealigner.domainobjects.graphs;

import fragtreealigner.domainobjects.chem.components.NeutralLoss;

@SuppressWarnings("serial")
public class FragmentationTreeEdge extends TreeEdge<FragmentationTreeEdge, FragmentationTreeNode> {
	private NeutralLoss neutralLoss;
	private float doubleBondEquivalent;
	private double score;
	
	public FragmentationTreeEdge(FragmentationTreeNode fromNode, FragmentationTreeNode toNode) {
		super(fromNode, toNode);
	}

	public FragmentationTreeEdge(FragmentationTreeNode fromNode, FragmentationTreeNode toNode, String label, NeutralLoss neutralLoss, float doubleBondEquivalent, double score) {
		super(fromNode, toNode, label);
		this.neutralLoss = neutralLoss;
		this.doubleBondEquivalent = doubleBondEquivalent;
		this.score = score;
	}
	
	public void setContent(FragmentationTreeEdge edge) {
		super.setContent(edge);
		this.neutralLoss = edge.getNeutralLoss();
		this.doubleBondEquivalent = edge.getDoubleBondEquivalent();
		this.score = edge.getScore();
	}
	
	public void setNeutralLoss(NeutralLoss neutralLoss) {
		this.neutralLoss = neutralLoss;
	}

	public NeutralLoss getNeutralLoss() {
		return neutralLoss;
	}

	public void setDoubleBondEquivalent(float doubleBondEquivalent) {
		this.doubleBondEquivalent = doubleBondEquivalent;
	}
	
	public float getDoubleBondEquivalent() {
		return doubleBondEquivalent;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getScore() {
		return score;
	}

	public String getCmlId() {
		return "nl" + getId() + "_" + getNeutralLoss().getMolecularFormula();
	}
	
	@Override
	public FragmentationTreeEdge clone() {
		FragmentationTreeEdge clonedEdge = new FragmentationTreeEdge(fromNode, toNode);
		if (label != null) clonedEdge.setLabel(new String(label));
		if (neutralLoss != null) clonedEdge.setNeutralLoss(neutralLoss.clone());
		clonedEdge.setDoubleBondEquivalent(doubleBondEquivalent);
		
		return clonedEdge;
	}
	
	@Override
	public String toString() {
		StringBuffer out = new StringBuffer();
		out.append(fromNode.getLabel());
		out.append("->");
		out.append(toNode.getLabel());
		return super.toString();
	}
}
