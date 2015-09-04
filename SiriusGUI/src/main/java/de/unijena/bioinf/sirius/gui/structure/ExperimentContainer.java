package de.unijena.bioinf.sirius.gui.structure;

import java.util.*;

import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;

public class ExperimentContainer {
	
	private List<CompactSpectrum> ms1Spectra, ms2Spectra;
	
	private Ionization ionization;
	private double selectedFocusedMass;
	private double dataFocusedMass;
	private String name;
	
	private List<SiriusResultElement> results;

	public ExperimentContainer() {
		ms1Spectra = new ArrayList<CompactSpectrum>();
		ms2Spectra = new ArrayList<CompactSpectrum>();
		ionization = Ionization.Unknown;
		selectedFocusedMass = -1;
		dataFocusedMass = -1;
		name = "";
		results = Collections.emptyList();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<CompactSpectrum> getMs1Spectra() {
		return ms1Spectra;
	}

	public void setMs1Spectra(List<CompactSpectrum> ms1Spectra) {
		if(ms1Spectra==null) return;
		this.ms1Spectra = ms1Spectra;
	}

	public List<CompactSpectrum> getMs2Spectra() {
		return ms2Spectra;
	}

	public void setMs2Spectra(List<CompactSpectrum> ms2Spectra) {
		if(ms2Spectra==null) return;
		this.ms2Spectra = ms2Spectra;
	}

	public Ionization getIonization() {
		return ionization;
	}

	public void setIonization(Ionization ionization) {
		this.ionization = ionization;
	}

	public double getSelectedFocusedMass() {
		return selectedFocusedMass;
	}
	
	public double getDataFocusedMass() {
		return dataFocusedMass;
	}

	public void setSelectedFocusedMass(double focusedMass) {
		this.selectedFocusedMass = focusedMass;
	}
	
	public void setDataFocusedMass(double focusedMass) {
		this.dataFocusedMass = focusedMass;
	}
	
	public void setResults(List<SiriusResultElement> results){
		this.results = results;
	}
	
	public List<SiriusResultElement> getResults(){
		return this.results;
	}
	

}
