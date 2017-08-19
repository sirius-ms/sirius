package de.unijena.bioinf.sirius.gui.msviewer.data;


public interface MSViewerDataModel {

	public double minMz();
	public double maxMz();
	
	int getSize();
	
	double getMass(int index);
	
	double getRelativeIntensity(int index);
	
	double getSignalNoise(int index);
	
	double getAbsoluteIntensity(int index);
	
	String getMolecularFormula(int index);
	
	PeakInformation getInformations(int index);
	
	boolean isMarked(int index);
	
	boolean isImportantPeak(int index);
	
	boolean isUnimportantPeak(int index);
	
//	public boolean isNoise(int index);
	
	boolean isPlusZeroPeak(int index);
	
	boolean isIsotope(int index);
	
	int[] getIsotopePeaks(int index);
	
	String getLabel();
	
	int getIndexWithMass(double mass);
	
	int findIndexOfPeak(double mass, double tolerance);

}
