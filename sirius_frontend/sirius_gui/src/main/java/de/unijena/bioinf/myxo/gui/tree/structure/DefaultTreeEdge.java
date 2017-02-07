package de.unijena.bioinf.myxo.gui.tree.structure;

public class DefaultTreeEdge implements TreeEdge {
	
	private TreeNode source, target;
	private String mf;
	private double score, mfMass;
	
	public DefaultTreeEdge(){
		source = null;
		target = null;
		mf = "";
		score = 0;
		mfMass = 0;
	}

	@Override
	public TreeNode getSource() {
		return this.source;
	}

	@Override
	public TreeNode getTarget() {
		return this.target;
	}

	@Override
	public String getLossFormula() {
		return this.mf;
	}

	@Override
	public double getLossMass() {
		return this.mfMass;
	}

	@Override
	public double getScore() {
		return this.score;
	}

	@Override
	public void setSource(TreeNode source) {
		this.source = source;
	}

	@Override
	public void setTarget(TreeNode target) {
		this.target = target;
	}

	@Override
	public void setLossFormula(String formula) {
		this.mf = formula;
	}

	@Override
	public void setLossMass(double mass) {
		this.mfMass = mass;
	}

	@Override
	public void setScore(double score) {
		this.score = score;
	}

}
