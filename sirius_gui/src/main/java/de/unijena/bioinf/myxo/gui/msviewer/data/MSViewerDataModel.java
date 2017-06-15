package de.unijena.bioinf.myxo.gui.msviewer.data;


public interface MSViewerDataModel {
	
	@SuppressWarnings("unused")
    int getSize();
	
	double getMass(int index);
	
	double getRelativeIntensity(int index);
	
	@SuppressWarnings("unused")
    double getSignalNoise(int index);
	
	double getAbsoluteIntensity(int index);
	
	String getMolecularFormula(int index);
	
	@SuppressWarnings("unused")
    PeakInformation getInformations(int index);
	
	boolean isMarked(int index);
	
	boolean isImportantPeak(int index);
	
	boolean isUnimportantPeak(int index);
	
//	public boolean isNoise(int index);
	
	boolean isPlusZeroPeak(int index);
	
	boolean isIsotope(int index);
	
	@SuppressWarnings("unused")
    int[] getIsotopePeaks(int index);
	
	String getLabel();
	
	@SuppressWarnings("unused")
    int getIndexWithMass(double mass);
	
	@SuppressWarnings("unused")
    int findIndexOfPeak(double mass, double tolerance);

}
