package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public abstract class BasicSpectrum<P extends Peak> extends AbstractSpectrum<P> {
	
	protected final double[] masses;
	protected final double[] intensities;
	
	public BasicSpectrum(double[] masses, double[] intensities) {
		if (masses.length != intensities.length)
			throw new IllegalArgumentException("size of masses and intensities differ");
		this.masses = masses.clone();
		this.intensities = intensities.clone();
	}
	
	public <T extends Peak, S extends Spectrum<T>> BasicSpectrum(S s) {
		this(Spectrums.copyMasses(s), Spectrums.copyIntensities(s));
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone(); // possible because immutable
	}

	@Override
	public int size() {
		return masses.length;
	}

	@Override
	public <T> T getProperty(String name) {
		return null;
	}

	@Override
	public <T> T getProperty(String name, T defaultValue) {
		return defaultValue;
	}

}
