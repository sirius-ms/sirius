package de.unijena.bioinf.myxo.structure;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.List;

public interface CompactExperiment {
	
	public CompactSpectrum getMS1Spectrum();
	
	public List<CompactSpectrum> getMS2Spectra();
	
	@SuppressWarnings("unused")
	public double getFocusedMass();
	
	@SuppressWarnings("unused")
	public String getCompoundName();
	
	@SuppressWarnings("unused")
	public double getRetentionTime();
	
	@SuppressWarnings("unused")
	public MolecularFormula getMolecularFormula();
	
	@SuppressWarnings("unused")
	public Deviation getDeviation();
	
	@SuppressWarnings("unused")
	public void setFocusedMass(double mass);
	
	@SuppressWarnings("unused")
	public void setIonization(String ion);
	
	@SuppressWarnings("unused")
	public String getIonization();

}
