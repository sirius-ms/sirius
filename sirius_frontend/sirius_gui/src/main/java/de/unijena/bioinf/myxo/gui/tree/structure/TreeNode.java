package de.unijena.bioinf.myxo.gui.tree.structure;

import java.util.List;

public interface TreeNode {
	
	@SuppressWarnings("unused")
	public TreeEdge getInEdge();
	
	public List<TreeEdge> getOutEdges();
	
	public int getOutEdgeNumber();
	
	public boolean hasParent();
	
	public TreeEdge getOutEdge(int index);
	
	public String getMolecularFormula();
	
	@SuppressWarnings("unused")
	public double getMolecularFormulaMass();
	
	public double getPeakMass();
	
	public double getPeakAbsoluteIntensity();
	
	public double getPeakRelativeIntensity();
	
	@SuppressWarnings("unused")
	public double getPeakSignalToNoise();
	
	public double getScore();
	
	@SuppressWarnings("unused")
	public String getCollisionEnergy();
	
	public void setInEdge(TreeEdge edge);
	
	public void addOutEdge(TreeEdge edge);
	
	public void setOutEdges(List<TreeEdge> outEdges);
	
	@SuppressWarnings("unused")
	public void setMolecularFormula(String mf);
	
	@SuppressWarnings("unused")
	public void setMolecularFormulaMass(double mass);
	
	@SuppressWarnings("unused")
	public void setPeakMass(double mass);
	
	@SuppressWarnings("unused")
	public void setPeakAbsoluteIntenstiy(double absInt);
	
	@SuppressWarnings("unused")
	public void setPeakRelativeIntensity(double relInt);
	
	@SuppressWarnings("unused")
	public void setPeakSignalToNoise(double sn);
	
	@SuppressWarnings("unused")
	public void setScore(double score);
	
	@SuppressWarnings("unused")
	public void setCollisionEnergy(String colEnergy);
	
	public void setHorizontalRequirement(int val);
	
	public int getHorizontalRequirement();
	
	public void setVerticalRequirement(int val);
	
	public int getVerticalRequirement();
	
	public void setHorizontalPosition(int val);
	
	public void setVerticalPosition(int val);
	
	public int getHorizontalPosition();
	
	public int getVerticalPosition();
	
	public void setNodeDepth(int depth);
	
	@SuppressWarnings("unused")
	public int getNodeDepth();
	
	@SuppressWarnings("unused")
	public void setID(int id);
	
	@SuppressWarnings("unused")
	public int getID();
	
	public int getNodeNumber();
	
	public void setNodeNumber(int number);

}
