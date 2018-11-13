package de.unijena.bioinf.myxo.gui.tree.structure;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultTreeNode implements TreeNode {
	
	protected List<TreeEdge> outEdges;
	protected TreeEdge inEdge;
	
	private double peakMass, peakAbsInt, peakRelInt, peakSN;
	private String molecularFormula;
	private double formulaMass;
	private String colEnergy;
	private double score;
	private double deviationMass;
	private Double medianMassDeviation;

	private String ionization;

	private int horSize, vertSize;
	private int horPos, vertPos;
	private int depth;
	private int id;
	private int nodeNumber;

	public DefaultTreeNode(){
		peakMass = 0;
		peakAbsInt = 0;
		peakRelInt = 0;
		peakSN = 0;
		molecularFormula = "";
		formulaMass = 0;
		colEnergy = "";
		
		outEdges = new ArrayList<TreeEdge>(5);
		inEdge = null;
		score = 0;
		
		horSize = 0;
		vertSize = 0;
		horPos = 0;
		vertPos = 0;
		depth = 0;
		id=-1;
		nodeNumber = 0;
	}

	@Override
	public TreeEdge getInEdge() {
		return this.inEdge;
	}

	@Override
	public List<TreeEdge> getOutEdges() {
		return Collections.unmodifiableList(this.outEdges);
	}

	@Override
	public int getOutEdgeNumber() {
		return this.outEdges.size();
	}

	@Override
	public boolean hasParent() {
		return this.inEdge != null;
	}

	@Override
	public TreeEdge getOutEdge(int index) {
		return outEdges.get(index);
	}

	@Override
	public String getMolecularFormula() {
		return this.molecularFormula;
	}

	@Override
	public double getMolecularFormulaMass() {
		return this.formulaMass;
	}

	@Override
	public double getPeakMass() {
		return this.peakMass;
	}

	@Override
	public double getPeakAbsoluteIntensity() {
		return this.peakAbsInt;
	}

	@Override
	public double getPeakRelativeIntensity() {
		return this.peakRelInt;
	}

	@Override
	public double getPeakSignalToNoise() {
		return this.peakSN;
	}

	@Override
	public double getScore() {
		return this.score;
	}

	@Override
	public double getDeviationMass() {
		return deviationMass;
	}

	@Override
	public void setDeviatonMass(double deviationMass) {
		this.deviationMass = deviationMass;
	}

	@Override
	public String getCollisionEnergy() {
		return this.colEnergy;
	}
	
	@Override
	public void setInEdge(TreeEdge inEdge) {
		this.inEdge = inEdge;
	}

	@Override
	public void setScore(double score){
		this.score = score;
	}

	@Override
	public void setPeakMass(double peakMass) {
		this.peakMass = peakMass;
	}

	@Override
	public void setPeakAbsoluteIntenstiy(double peakAbsInt) {
		this.peakAbsInt = peakAbsInt;
	}

	@Override
	public void setPeakRelativeIntensity(double peakRelInt) {
		this.peakRelInt = peakRelInt;
	}

	@Override
	public void setPeakSignalToNoise(double peakSN) {
		this.peakSN = peakSN;
	}

	@Override
	public void setMolecularFormula(String molecularFormula) {
		this.molecularFormula = molecularFormula;
	}

	@Override
	public void setMolecularFormulaMass(double formulaMass) {
		this.formulaMass = formulaMass;
	}

	@Override
	public void setCollisionEnergy(String colEnergy) {
		this.colEnergy = colEnergy;
	}
	
	@Override
	public void addOutEdge(TreeEdge outEdge){
		this.outEdges.add(outEdge);
	}

	@Override
	public void setHorizontalRequirement(int val) {
		this.horSize = val;
	}

	@Override
	public int getHorizontalRequirement() {
		return this.horSize;
	}

	@Override
	public void setVerticalRequirement(int val) {
		this.vertSize = val;
	}

	@Override
	public int getVerticalRequirement() {
		return this.vertSize;
	}

	@Override
	public void setHorizontalPosition(int val) {
		this.horPos = val;
	}

	@Override
	public void setVerticalPosition(int val) {
		this.vertPos = val;
	}

	@Override
	public int getHorizontalPosition() {
		return this.horPos;
	}

	@Override
	public int getVerticalPosition() {
		return this.vertPos;
	}

	@Override
	public void setNodeDepth(int depth) {
		this.depth = depth;
	}

	@Override
	public int getNodeDepth() {
		return this.depth;
	}

	@Override
	public void setID(int id) {
		this.id = id;
	}

	@Override
	public int getID() {
		return this.id;
	}

	@Override
	public void setOutEdges(List<TreeEdge> outEdges) {
		this.outEdges = outEdges;
	}

	@Override
	public int getNodeNumber() {
		return this.nodeNumber;
	}

	@Override
	public void setNodeNumber(int number) {
		this.nodeNumber = number;
	}

	@Override
	public void setMedianMassDeviation(Double medianMassDeviation) {
		this.medianMassDeviation = medianMassDeviation;
	}

	@Override
	public Double getMedianMassDeviation() {
		return medianMassDeviation;
	}


	@Override
	public void setIonization(String ionization) {
		this.ionization = ionization;
	}

	@Override
	public String getIonization() {
		return ionization;
	}
}
