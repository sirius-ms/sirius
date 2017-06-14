package de.unijena.bioinf.myxo.gui.tree.structure;

public interface TreeEdge {
	
	@SuppressWarnings("unused")
	public TreeNode getSource();
	
	public TreeNode getTarget();
	
	public String getLossFormula();
	
	@SuppressWarnings("unused")
	public double getLossMass();
	
	@SuppressWarnings("unused")
	public double getScore();
	
	@SuppressWarnings("unused")
	public void setSource(TreeNode source);
	
	@SuppressWarnings("unused")
	public void setTarget(TreeNode target);
	
	@SuppressWarnings("unused")
	public void setLossFormula(String formula);
	
	@SuppressWarnings("unused")
	public void setLossMass(double mass);
	
	@SuppressWarnings("unused")
	public void setScore(double score);

}
