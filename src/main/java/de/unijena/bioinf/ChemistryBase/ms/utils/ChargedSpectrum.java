package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
@Deprecated
public class ChargedSpectrum extends BasicSpectrum<ChargedPeak>{

	private final Ionization ionization;
	
	public ChargedSpectrum(ChargedSpectrum p) {
		super(p);
		this.ionization = p.ionization;
	}
	
	public ChargedSpectrum(Spectrum<?> s, Ionization ionization) {
		super(s);
		this.ionization = ionization;
	}
	
	public ChargedSpectrum(Spectrum<?> s, int charge) {
		this(s, new Charge(charge));
	}
	
	public ChargedSpectrum(double[] masses, double[] intensities, int charge) {
		this(masses, intensities, new Charge(charge));
	}
	
	public ChargedSpectrum(double[] masses, double[] intensities, Ionization ionization) {
		super(masses, intensities);
		this.ionization = ionization;
	}

    public Ionization getIonization() {
        return ionization;
    }
	
	public SimpleSpectrum getNeutralMassSpectrum() {
		return Spectrums.neutralMassSpectrum(this, ionization);
	}

	@Override
	public ChargedPeak getPeakAt(int index) {
		return new ChargedPeak(masses[index], intensities[index], ionization);
	}
	
	
	
	
}
