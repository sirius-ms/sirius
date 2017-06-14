package de.unijena.bioinf.myxo.structure;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultCompactExperiment implements CompactExperiment {
	
	public List<CompactSpectrum> ms2;
	
	public CompactSpectrum ms1;
	
	public double fM;
	
	private String name;
	
	private double rt;
	
	private MolecularFormula mf;
	
	private Deviation dev;
	
	private String ionization;
	
	public DefaultCompactExperiment(){
		ms2 = new ArrayList<>();
		fM = -1;
		name = "";
		rt = 0;
		ionization = "";
	}
	
	public void setMS1Spectrum(CompactSpectrum ms1){
		this.ms1 = ms1;
	}
	
	public void addMS2Spectrum(CompactSpectrum sp){
		if(sp!=null && !ms2.contains(sp)) ms2.add(sp);
	}

	@Override
	public CompactSpectrum getMS1Spectrum() {
		return this.ms1;
	}

	@Override
	public List<CompactSpectrum> getMS2Spectra() {
		return Collections.unmodifiableList(ms2);
	}
	
	public void setFocusedMass(double fM){
		this.fM = fM;
	}

	@Override
	public double getFocusedMass() {
		return this.fM;
	}
	
	public void setCompoundName(String name){
		this.name = name;
	}

	@Override
	public String getCompoundName() {
		return this.name;
	}
	
	@SuppressWarnings("unused")
	public void setRetentionTime(double rt){
		if(rt>0)this.rt = rt;
	}

	@Override
	public double getRetentionTime() {
		return this.rt;
	}

	public void setMolecularFormula(MolecularFormula mf){
		this.mf = mf;
	}
	
	@Override
	public MolecularFormula getMolecularFormula() {
		return this.mf;
	}
	
	@SuppressWarnings("unused")
	public void setDeviation(Deviation dev){
		this.dev = dev;
	}

	@Override
	public Deviation getDeviation() {
		return this.dev;
	}

	@Override
	public void setIonization(String ion) {
		this.ionization = ion;
	}

	@Override
	public String getIonization() {
		return this.ionization;
	}

}
