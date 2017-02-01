package de.unijena.bioinf.sirius.gui.structure;

import ca.odell.glazedlists.EventList;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jdesktop.beans.AbstractBean;

import javax.swing.event.SwingPropertyChangeSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExperimentContainer extends AbstractBean {

    private List<CompactSpectrum> ms1Spectra, ms2Spectra;

    //private Ionization ionization;
    private PrecursorIonType ionization;
    private double selectedFocusedMass;
    private double dataFocusedMass;
    private String name, guiName;
    private URL source;
    private int suffix;
    private volatile ComputingStatus computeState;
    private String errorMessage;

    private volatile List<SiriusResultElement> results;
    private volatile List<IdentificationResult> originalResults;
    private volatile SiriusResultElement bestHit;
    private volatile int bestHitIndex =0;


    public ExperimentContainer() {
        ms1Spectra = new ArrayList<CompactSpectrum>();
        ms2Spectra = new ArrayList<CompactSpectrum>();
        ionization = PrecursorIonType.unknown(1);
        selectedFocusedMass = -1;
        dataFocusedMass = -1;
        name = "";
        guiName = "";
        suffix = 1;
        results = Collections.emptyList();
        originalResults = Collections.emptyList();
        this.computeState = ComputingStatus.UNCOMPUTED;
    }

    public SiriusResultElement getBestHit() {
        return bestHit;
    }

    public int getBestHitIndex() {
        return bestHitIndex;
    }

    public String getName() {
        return name;
    }

    public String getGUIName() {
        return guiName;
    }

    public int getSuffix() {
        return this.suffix;
    }

    public List<CompactSpectrum> getMs1Spectra() {
//		System.out.println("getMS1Spectra");
        return ms1Spectra;
    }

    public void setMs1Spectra(List<CompactSpectrum> ms1Spectra) {
//		System.out.println("setMS1Spectra");
        if (ms1Spectra == null) return;
        this.ms1Spectra = ms1Spectra;
    }

    public List<CompactSpectrum> getMs2Spectra() {
//		System.out.println("getMS2Spectra");
        return ms2Spectra;
    }

    public void setMs2Spectra(List<CompactSpectrum> ms2Spectra) {
//		System.out.println("setMS2Spectra");
        if (ms2Spectra == null) return;
        this.ms2Spectra = ms2Spectra;
    }

    public PrecursorIonType getIonization() {
        return ionization;
    }

    public double getSelectedFocusedMass() {
        return selectedFocusedMass;
    }

    public double getDataFocusedMass() {
        return dataFocusedMass;
    }

    public List<SiriusResultElement> getResults() {
        return this.results;
    }

    public List<IdentificationResult> getRawResults() {
        return originalResults;
    }

    public double getFocusedMass() {
        if (selectedFocusedMass > 0) return selectedFocusedMass;
        else return dataFocusedMass;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isComputed() {
        return computeState == ComputingStatus.COMPUTED;
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

    public boolean isFailed() {
        return this.computeState == ComputingStatus.FAILED;
    }

    public boolean isQueued() {
        return computeState == ComputingStatus.QUEUED;
    }


    public void setRawResults(List<IdentificationResult> results) {
        setRawResults(results,SiriusResultElementConverter.convertResults(results));
        if (this.computeState == ComputingStatus.COMPUTING)
            setComputeState(results.size() == 0 ? ComputingStatus.FAILED : ComputingStatus.COMPUTED);
    }

    public void setRawResults(List<IdentificationResult> results, List<SiriusResultElement> myxoresults) {
        List<SiriusResultElement> old = this.results;
        this.results = myxoresults;
        this.originalResults = results;
        firePropertyChange("results_upadated",old,this.results);
        if (this.computeState == ComputingStatus.COMPUTING)
            setComputeState((results == null || results.size() == 0) ? ComputingStatus.FAILED : ComputingStatus.COMPUTED);
    }

    public void setName(String name) {
        this.name = name;
        setGuiName(this.suffix >= 2 ? this.name + " (" + suffix + ")" : this.name);
    }

    public void setSuffix(int value) {
        this.suffix = value;
        setGuiName(this.suffix >= 2 ? this.name + " (" + suffix + ")" : this.name);
    }

    // with change event
    protected void setGuiName(String guiName) {
        String old = this.guiName;
        this.guiName = guiName;
        firePropertyChange("guiName", old, this.guiName);
    }

    public void setBestHit(final SiriusResultElement bestHit) {
        if (this.bestHit != bestHit) {
            if (this.bestHit != null)
                this.bestHit.setBestHit(false);
            this.bestHit = bestHit;
            this.bestHit.setBestHit(true);
            bestHitIndex = getResults().indexOf(bestHit);
        }
        //todo do we need change event her. I think not becaus the element fires one
    }

    public void setIonization(PrecursorIonType ionization) {
        PrecursorIonType old = this.ionization;
        this.ionization = ionization;
        firePropertyChange("ionization", old, this.ionization);
    }

    public void setSelectedFocusedMass(double focusedMass) {
        double old = selectedFocusedMass;
        selectedFocusedMass = focusedMass;
        firePropertyChange("selectedFocusedMass", old, selectedFocusedMass);
    }

    public void setDataFocusedMass(double focusedMass) {
        double old = dataFocusedMass;
        dataFocusedMass = focusedMass;
        firePropertyChange("dataFocusedMass", old, dataFocusedMass);
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setComputeState(ComputingStatus st) {
        ComputingStatus oldST = this.computeState;
        this.computeState = st;
        firePropertyChange("computeState", oldST, this.computeState);
    }

    public void fireUpdateEvent(){
        firePropertyChange("updated", false, true);
    }

    public URL getSource() {
        return source;
    }

    public void setSource(URL source) {
        this.source = source;
    }
}
