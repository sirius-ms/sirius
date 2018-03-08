package de.unijena.bioinf.myxo.gui.tree.structure;

import java.util.List;

public interface TreeNode {
	
	@SuppressWarnings("unused")
    TreeEdge getInEdge();
	
	List<TreeEdge> getOutEdges();
	
	int getOutEdgeNumber();
	
	boolean hasParent();
	
	TreeEdge getOutEdge(int index);
	
	String getMolecularFormula();
	
	@SuppressWarnings("unused")
    double getMolecularFormulaMass();
	
	double getPeakMass();
	
	double getPeakAbsoluteIntensity();
	
	double getPeakRelativeIntensity();
	
	@SuppressWarnings("unused")
    double getPeakSignalToNoise();

	double getScore();

	double getDeviationMass();

	void setDeviatonMass(double deviationMass);
	
	@SuppressWarnings("unused")
    String getCollisionEnergy();
	
	void setInEdge(TreeEdge edge);
	
	void addOutEdge(TreeEdge edge);
	
	void setOutEdges(List<TreeEdge> outEdges);
	
	@SuppressWarnings("unused")
    void setMolecularFormula(String mf);
	
	@SuppressWarnings("unused")
    void setMolecularFormulaMass(double mass);
	
	@SuppressWarnings("unused")
    void setPeakMass(double mass);
	
	@SuppressWarnings("unused")
    void setPeakAbsoluteIntenstiy(double absInt);
	
	@SuppressWarnings("unused")
    void setPeakRelativeIntensity(double relInt);
	
	@SuppressWarnings("unused")
    void setPeakSignalToNoise(double sn);
	
	@SuppressWarnings("unused")
    void setScore(double score);
	
	@SuppressWarnings("unused")
    void setCollisionEnergy(String colEnergy);
	
	void setHorizontalRequirement(int val);
	
	int getHorizontalRequirement();
	
	void setVerticalRequirement(int val);
	
	int getVerticalRequirement();
	
	void setHorizontalPosition(int val);
	
	void setVerticalPosition(int val);
	
	int getHorizontalPosition();
	
	int getVerticalPosition();
	
	void setNodeDepth(int depth);
	
	@SuppressWarnings("unused")
    int getNodeDepth();
	
	@SuppressWarnings("unused")
    void setID(int id);
	
	@SuppressWarnings("unused")
    int getID();
	
	int getNodeNumber();
	
	void setNodeNumber(int number);

}
