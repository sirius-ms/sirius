package de.unijena.bioinf.myxo.structure;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.List;

public interface CompactExperiment {
	
	CompactSpectrum getMS1Spectrum();
	
	List<CompactSpectrum> getMS2Spectra();
	
	@SuppressWarnings("unused")
    double getFocusedMass();
	
	@SuppressWarnings("unused")
    String getCompoundName();
	
	@SuppressWarnings("unused")
    double getRetentionTime();
	
	@SuppressWarnings("unused")
    MolecularFormula getMolecularFormula();
	
	@SuppressWarnings("unused")
    Deviation getDeviation();
	
	@SuppressWarnings("unused")
    void setFocusedMass(double mass);
	
	@SuppressWarnings("unused")
    void setIonization(String ion);
	
	@SuppressWarnings("unused")
    String getIonization();

}
