package de.unijena.bioinf.ChemistryBase.ms.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public abstract class AbstractSpectrum<T extends Peak> implements Spectrum<T> {
	
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			
			private int index;
			
			@Override
			public boolean hasNext() {
				return index < size();
			}

			@Override
			public T next() {
				if (index < size())
					return getPeakAt(index++);
				else 
					throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	public double getMzAt(int index) {
		return getPeakAt(index).getMass();
	}
	
	public double getIntensityAt(int index) {
		return getPeakAt(index).getIntensity();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj.getClass().equals(getClass()))) return false;
		AbstractSpectrum<?> spectrum = (AbstractSpectrum<?>) obj;
		if (spectrum.size() != size()) return false;
		for (int i = 0; i < size(); i++) {
			if (!spectrum.getPeakAt(i).equals(getPeakAt(i)))
				return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		for (int i = 0; i < size(); i++) {
			hash = hash ^ getPeakAt(i).hashCode();
		}
		return hash;
	}
	
	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder(size()*12);
		final Iterator<T> iter = iterator();
		buffer.append("{").append(iter.next());
		while (iter.hasNext()) {
			buffer.append(", ");
			buffer.append(iter.next());
		}
		buffer.append("}");
		return buffer.toString();
	}
	
}
