package de.unijena.bioinf.myxo.gui.tree.structure;

public interface TreeEdge {
	
	@SuppressWarnings("unused")
    TreeNode getSource();
	
	TreeNode getTarget();
	
	String getLossFormula();
	
	@SuppressWarnings("unused")
    double getLossMass();
	
	@SuppressWarnings("unused")
    double getScore();
	
	@SuppressWarnings("unused")
    void setSource(TreeNode source);
	
	@SuppressWarnings("unused")
    void setTarget(TreeNode target);
	
	@SuppressWarnings("unused")
    void setLossFormula(String formula);
	
	@SuppressWarnings("unused")
    void setLossMass(double mass);
	
	@SuppressWarnings("unused")
    void setScore(double score);

}
