package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.ms.utils.PropertySet;




public interface Spectrum<T extends Peak> extends Iterable<T>, Cloneable, PropertySet {

	public double getMzAt(int index);
	
	public double getIntensityAt(int index);
	
	public T getPeakAt(int index);
	
	public int size();
	
}
