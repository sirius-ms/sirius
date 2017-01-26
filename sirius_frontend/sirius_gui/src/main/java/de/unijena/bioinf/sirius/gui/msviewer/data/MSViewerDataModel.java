package de.unijena.bioinf.sirius.gui.msviewer.data;


public interface MSViewerDataModel {
	
	public int getSize();
	
	public double getMass(int index);
	
	public double getRelativeIntensity(int index);
	
	public double getSignalNoise(int index);
	
	public double getAbsoluteIntensity(int index);
	
	public String getMolecularFormula(int index);
	
	public PeakInformation getInformations(int index);
	
	public boolean isMarked(int index);
	
	public boolean isImportantPeak(int index);
	
	public boolean isUnimportantPeak(int index);
	
//	public boolean isNoise(int index);
	
	public boolean isPlusZeroPeak(int index);
	
	public boolean isIsotope(int index);
	
	public int[] getIsotopePeaks(int index);
	
	public String getLabel();
	
	public int getIndexWithMass(double mass);
	
	public int findIndexOfPeak(double mass, double tolerance);

}
