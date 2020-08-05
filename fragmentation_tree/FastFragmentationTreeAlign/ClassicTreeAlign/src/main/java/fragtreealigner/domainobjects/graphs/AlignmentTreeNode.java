
package fragtreealigner.domainobjects.graphs;

import fragtreealigner.domainobjects.chem.components.Compound;
import fragtreealigner.domainobjects.chem.components.NeutralLoss;

@SuppressWarnings("serial")
public class AlignmentTreeNode extends TreeNode<AlignmentTreeNode, AlignmentTreeEdge> {
	private NeutralLoss neutralLoss;
	private Compound compound;
	private double weight;
	
	public AlignmentTreeNode() {
		super();
	}
	
	public AlignmentTreeNode(String label) {
		super(label);
	}
	
	public AlignmentTreeNode(String label, NeutralLoss neutralLoss) {
		this(label);
		this.setNeutralLoss(neutralLoss);
	}

	public AlignmentTreeNode(String label, NeutralLoss neutralLoss, double weight) {
		this(label, neutralLoss);
		this.setWeight(weight);
	}

	@Override
	public void setContent(AlignmentTreeNode node) {
		super.setContent(node);
		this.neutralLoss = node.getNeutralLoss();
		this.weight = node.getWeight();
	}

	public void setNeutralLoss(NeutralLoss neutralLoss) {
		this.neutralLoss = neutralLoss;
	}

	public NeutralLoss getNeutralLoss() {
		return neutralLoss;
	}

	public void setCompound(Compound compound) {
		this.compound = compound;
	}

	public Compound getCompound() {
		return compound;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		if (neutralLoss == null || compound == null) return getLabel();
		else return (getLabel() + "["+ compound.getMolecularFormula()+ " " + neutralLoss.getMolecularFormula() + "]");
	}
	
	@Override
	public AlignmentTreeNode clone() {
		AlignmentTreeNode clonedAligTreeNode = new AlignmentTreeNode(new String(label));
		if (neutralLoss != null) clonedAligTreeNode.setNeutralLoss(neutralLoss.clone());
		return clonedAligTreeNode;
	}
}
