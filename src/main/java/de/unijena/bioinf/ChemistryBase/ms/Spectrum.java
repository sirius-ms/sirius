package de.unijena.bioinf.ChemistryBase.ms;





public interface Spectrum<T extends Peak> extends Iterable<T>, Cloneable {

	public double getMzAt(int index);
	
	public double getIntensityAt(int index);
	
	public T getPeakAt(int index);
	
	public int size();
	
}
