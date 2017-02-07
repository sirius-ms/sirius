package de.unijena.bioinf.myxo.gui.msviewer.data;


public interface MSViewerDataModel {
	
	@SuppressWarnings("unused")
	public int getSize();
	
	public double getMass(int index);
	
	public double getRelativeIntensity(int index);
	
	@SuppressWarnings("unused")
	public double getSignalNoise(int index);
	
	public double getAbsoluteIntensity(int index);
	
	public String getMolecularFormula(int index);
	
	@SuppressWarnings("unused")
	public PeakInformation getInformations(int index);
	
	public boolean isMarked(int index);
	
	public boolean isImportantPeak(int index);
	
	public boolean isUnimportantPeak(int index);
	
//	public boolean isNoise(int index);
	
	public boolean isPlusZeroPeak(int index);
	
	public boolean isIsotope(int index);
	
	@SuppressWarnings("unused")
	public int[] getIsotopePeaks(int index);
	
	public String getLabel();
	
	@SuppressWarnings("unused")
	public int getIndexWithMass(double mass);
	
	@SuppressWarnings("unused")
	public int findIndexOfPeak(double mass, double tolerance);

}
