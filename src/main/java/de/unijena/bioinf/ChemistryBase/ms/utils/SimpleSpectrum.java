package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.util.Arrays;

public class SimpleSpectrum extends BasicSpectrum<Peak> implements OrderedSpectrum{

	protected final int hash;
	
	public SimpleSpectrum(double[] masses, double[] intensities) {
		this(new ArrayWrapperSpectrum(masses, intensities));
	}
	
	public <T extends Peak, S extends Spectrum<T>> SimpleSpectrum(S s) {
		super(orderedSpectrum(s));
		this.hash = Arrays.hashCode(this.masses) ^ Arrays.hashCode(this.intensities);
	}

	@Override
	public double getMzAt(int index) {
		return masses[index];
	}
	
	@Override
	public double getIntensityAt(int index) {
		return intensities[index];
	}
	
	@Override
	public Peak getPeakAt(int index) {
		return new Peak(masses[index], intensities[index]);
	}
	
	
	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public SimpleSpectrum clone() {
		return new SimpleSpectrum(masses, intensities);
	}
		
	private static Spectrum<? extends Peak> orderedSpectrum(Spectrum<? extends Peak> s) {
        if (s instanceof OrderedSpectrum) return s;
        final MutableSpectrum<? extends Peak> t = (s instanceof MutableSpectrum) ? (MutableSpectrum<? extends Peak>)s :
															new ArrayWrapperSpectrum(s);
		Spectrums.sortSpectrumByMass(t);
		return t;
	}
	
	
}
