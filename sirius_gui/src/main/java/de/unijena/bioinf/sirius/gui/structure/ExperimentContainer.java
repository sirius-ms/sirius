package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jdesktop.beans.AbstractBean;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExperimentContainer extends AbstractBean {
    //the ms experiment we use for computation
    private final MutableMs2Experiment experiment;
    //results and gui wrapper for the experiment
    private volatile List<SiriusResultElement> results;
    private volatile List<IdentificationResult> originalResults;
    private volatile SiriusResultElement bestHit;
    private volatile int bestHitIndex = 0;

    //todo map this drectly to ms2exp
    private double selectedFocusedMass;
    private double dataFocusedMass;

    private String guiName;
    //    private URL source;
    private int suffix;
    private volatile ComputingStatus computeState;
    private String errorMessage;


    public ExperimentContainer(Ms2Experiment source) {
        this(new MutableMs2Experiment(source));
    }

    public ExperimentContainer(MutableMs2Experiment source) {
        this.experiment = source;
        selectedFocusedMass = -1;
        dataFocusedMass = -1;
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
        return experiment.getName();
    }

    public String getGUIName() {
        return guiName;
    }

    public int getSuffix() {
        return this.suffix;
    }

    public List<SimpleSpectrum> getMs1Spectra() {
        return experiment.getMs1Spectra();
    }

    public List<MutableMs2Spectrum> getMs2Spectra() {
        return experiment.getMs2Spectra();
    }

    public SimpleSpectrum getCorrelatedSpectrum() {
        return experiment.getMergedMs1Spectrum();
    }

    public PrecursorIonType getIonization() {
        return experiment.getPrecursorIonType();
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
        setRawResults(results, SiriusResultElementConverter.convertResults(results));
    }

    public void setRawResults(List<IdentificationResult> results, List<SiriusResultElement> myxoresults) {
        List<SiriusResultElement> old = this.results;
        this.results = myxoresults;
        this.originalResults = results == null ? Collections.<IdentificationResult>emptyList() : results;
        firePropertyChange("results_upadated", old, this.results);
        if (this.computeState == ComputingStatus.COMPUTING)
            setComputeState((results == null || results.size() == 0) ? ComputingStatus.FAILED : ComputingStatus.COMPUTED);
    }

    public void setName(String name) {
        experiment.setName(name);
        setGuiName(createGuiName());
    }

    public void setSuffix(int value) {
        this.suffix = value;
        setGuiName(createGuiName());
    }

    private String createGuiName() {
        return this.suffix >= 2 ? experiment.getName() + " (" + suffix + ")" : experiment.getName();
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
    }

    public void setIonization(PrecursorIonType ionization) {
        PrecursorIonType old = experiment.getPrecursorIonType();
        experiment.setPrecursorIonType(ionization);
        firePropertyChange("ionization", old, experiment.getPrecursorIonType());
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

    public void fireUpdateEvent() {
        firePropertyChange("updated", false, true);
    }

    public URL getSource() {
        return experiment.getSource();
    }

    public MutableMs2Experiment getMs2Experiment() {
        return experiment;
    }

    public ExperimentContainer copy() {
        ExperimentContainer newExp = new ExperimentContainer(new MutableMs2Experiment(experiment));
        newExp.setDataFocusedMass(getDataFocusedMass());
        newExp.setSelectedFocusedMass(getSelectedFocusedMass());
        newExp.setSuffix(getSuffix());
        List<SiriusResultElement> sre = new ArrayList<>(getResults());
        newExp.setRawResults(getRawResults(), sre);
        return newExp;
    }

}
