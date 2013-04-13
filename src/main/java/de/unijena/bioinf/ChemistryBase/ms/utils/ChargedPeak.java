package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

public class ChargedPeak extends Peak{
	
	private final Ionization ionization;
	
	public ChargedPeak(double mz, double intensity, int charge) {
		this(mz, intensity, new Charge(charge));
	}
	
	public ChargedPeak(double mz, double intensity, Ionization ion) {
		super(mz, intensity);
		this.ionization = ion;
	}
	
	public int getCharge() {
		return ionization.getCharge();
	}
	
	public Ionization getIonization() {
		return ionization;
	}
	
	public double getNeutralMass() {
		return ionization.subtractFromMass(mass);
	}
	
	public double getMassToChargeRatio() {
		return mass;
	}
}
