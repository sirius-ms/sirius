package de.unijena.bioinf.ChemistryBase.ms;

public interface MutableSpectrum<T extends Peak> extends Spectrum<T> {
	
	public void addPeak(T peak);
	
	public void setPeakAt(int index, T peak);
	
	public void setMzAt(int index, double mass);
	
	public void setIntensityAt(int index, double intensity);
	
	public Peak removePeakAt(int index);
	
	public void swap(int index1, int index2);	

}
