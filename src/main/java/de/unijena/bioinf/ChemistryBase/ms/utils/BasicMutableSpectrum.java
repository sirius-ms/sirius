package de.unijena.bioinf.ChemistryBase.ms.utils;

import org.apache.commons.collections.primitives.ArrayDoubleList;
import org.apache.commons.collections.primitives.DoubleList;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public abstract class BasicMutableSpectrum<P extends Peak> extends AbstractSpectrum<P> implements MutableSpectrum<P> {

	protected DoubleList masses;
	protected DoubleList intensities;
	
	public <S extends Spectrum<? extends P>> BasicMutableSpectrum(S immutable) {
		this.masses = new ArrayDoubleList(Spectrums.copyMasses(immutable));
		this.intensities = new ArrayDoubleList(Spectrums.copyIntensities(immutable));
	}
	
	public BasicMutableSpectrum() {
		this.masses = new ArrayDoubleList();
		this.intensities = new ArrayDoubleList();
	}

	@Override
	public int size() {
		return masses.size();
	}

	@Override
	public <T> T getProperty(String name) {
		return null;
	}

	@Override
	public <T> T getProperty(String name, T defaultValue) {
		return defaultValue;
	}

	@Override
	public void addPeak(P peak) {
		masses.add(peak.getMass());
		intensities.add(peak.getIntensity());
	}

	@Override
	public void setPeakAt(int index, P peak) {
		masses.set(index, peak.getMass());
		intensities.set(index, peak.getIntensity());
	}

	@Override
	public Peak removePeakAt(int index) {
		final Peak p = getPeakAt(index);
		masses.removeElementAt(index);
		intensities.removeElementAt(index);
		return p;
	}

	@Override
	public void setMzAt(int index, double mz) {
		masses.set(index, mz);
	}

	@Override
	public void setIntensityAt(int index, double intensity) {
		intensities.set(index, intensity);
	}
	

}
