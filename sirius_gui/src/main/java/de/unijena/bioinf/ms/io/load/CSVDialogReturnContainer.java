package de.unijena.bioinf.ms.io.load;

public class CSVDialogReturnContainer {

	private double minEnergy, maxEnergy;
	private int massIndex, intIndex, msLevel;
	
	public CSVDialogReturnContainer() {
		minEnergy = -1;
		maxEnergy = -1;
		massIndex = -1;
		intIndex =-1;
		msLevel = -1;
	}
	
	public double getMinEnergy() {
		return minEnergy;
	}
	public void setMinEnergy(double minEnergy) {
		this.minEnergy = minEnergy;
	}
	public double getMaxEnergy() {
		return maxEnergy;
	}
	public void setMaxEnergy(double maxEnergy) {
		this.maxEnergy = maxEnergy;
	}
	public int getMassIndex() {
		return massIndex;
	}
	public void setMassIndex(int massIndex) {
		this.massIndex = massIndex;
	}
	public int getIntIndex() {
		return intIndex;
	}
	public void setIntIndex(int intIndex) {
		this.intIndex = intIndex;
	}
	public int getMsLevel() {
		return msLevel;
	}
	public void setMsLevel(int msLevel) {
		this.msLevel = msLevel;
	}
	
	

}
