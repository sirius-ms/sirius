package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExperimentContainer {

	private List<CompactSpectrum> ms1Spectra, ms2Spectra;

	private Ionization ionization;
	private double selectedFocusedMass;
	private double dataFocusedMass;
	private String name,guiName;
	private int suffix;
	private volatile ComputingStatus computeState;
	private String errorMessage;
	
	private volatile List<SiriusResultElement> results;
	private volatile List<IdentificationResult> originalResults;
	private volatile SiriusResultElement bestHit;


	public ExperimentContainer() {
		ms1Spectra = new ArrayList<CompactSpectrum>();
		ms2Spectra = new ArrayList<CompactSpectrum>();
		ionization = Ionization.UnknownPlus;
		selectedFocusedMass = -1;
		dataFocusedMass = -1;
		name = "";
		guiName ="";
		suffix = 1;
		results = Collections.emptyList();
		originalResults = Collections.emptyList();
		this.computeState = ComputingStatus.UNCOMPUTED;
	}

	public SiriusResultElement getBestHit() {
		return bestHit;
	}

	public void setBestHit(SiriusResultElement bestHit) {
		this.bestHit = bestHit;
	}

	public String getName() {
		return name;
	}
	
	public String getGUIName(){
		return guiName;
	}

	public void setName(String name) {
		this.name = name;
		this.guiName = this.suffix>=2 ? this.name + " ("+suffix+")" : this.name;
	}
	
	public void setSuffix(int value){
		this.suffix = value;
		this.guiName = this.suffix>=2 ? this.name + " ("+suffix+")" : this.name;
	}
	
	public int getSuffix(){
		return this.suffix;
	}

	public List<CompactSpectrum> getMs1Spectra() {
//		System.out.println("getMS1Spectra");
		return ms1Spectra;
	}

	public void setMs1Spectra(List<CompactSpectrum> ms1Spectra) {
//		System.out.println("setMS1Spectra");
		if(ms1Spectra==null) return;
		this.ms1Spectra = ms1Spectra;
	}

	public List<CompactSpectrum> getMs2Spectra() {
//		System.out.println("getMS2Spectra");
		return ms2Spectra;
	}

	public void setMs2Spectra(List<CompactSpectrum> ms2Spectra) {
//		System.out.println("setMS2Spectra");
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
	
	public List<SiriusResultElement> getResults(){
		return this.results;
	}

	public List<IdentificationResult> getRawResults() {
		return originalResults;
	}

	public void setRawResults(List<IdentificationResult> results) {
		this.originalResults = results;
		this.results = SiriusResultElementConverter.convertResults(originalResults);
		if (this.computeState==ComputingStatus.COMPUTING)
			this.computeState = results.size()==0 ? ComputingStatus.FAILED : ComputingStatus.COMPUTED;
	}

	public void setRawResults(List<IdentificationResult> results, List<SiriusResultElement> myxoresults) {
		this.originalResults = results;
		this.results = myxoresults;
        if (this.computeState==ComputingStatus.COMPUTING)
			this.computeState = results.size()==0 ? ComputingStatus.FAILED : ComputingStatus.COMPUTED;
	}


	public double getFocusedMass() {
		if (selectedFocusedMass > 0) return selectedFocusedMass;
		else return dataFocusedMass;
	}

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isComputed() {
		return computeState==ComputingStatus.COMPUTED;
	}

	public boolean isComputing() {
		return computeState == ComputingStatus.COMPUTING;
	}

	public boolean isUncomputed() {
		return computeState == ComputingStatus.UNCOMPUTED;
	}

	public ComputingStatus getComputeState() {
		return computeState;
	}

	public void setComputeState(ComputingStatus st) {
		this.computeState = st;
	}

	public boolean isFailed() {
		return this.computeState==ComputingStatus.FAILED;
	}

    public boolean isQueued() {
        return computeState == ComputingStatus.QUEUED;
    }
}
