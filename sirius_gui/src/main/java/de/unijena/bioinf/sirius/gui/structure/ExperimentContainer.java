package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is the wrapper for the Ms2Experiment.class to interact with the gui
 * elements. It uses a special property change support that executes the events
 * in the EDT. So you can change all fields from any thread, the gui will still
 * be updated in the EDT. Some operations may NOT be Thread save, so you may have
 * to care about Synchronization.
 */
public class ExperimentContainer extends AbstractEDTBean {
    //the ms experiment we use for computation
    private final MutableMs2Experiment experiment;
    //results and ut gui wrapper bean
    private volatile List<SiriusResultElement> results;
    private volatile List<IdentificationResult> originalResults;
    private volatile SiriusResultElement bestHit;
    private volatile int bestHitIndex = 0;


    private String guiName;
    private int suffix;
    private volatile ComputingStatus computeState;
    private String errorMessage;


    public ExperimentContainer(Ms2Experiment source) {
        this(new MutableMs2Experiment(source));
    }

    public ExperimentContainer(Ms2Experiment source, List<IdentificationResult> results) {
        this(source);
        setRawResults(results);
    }

    public ExperimentContainer(MutableMs2Experiment source) {
        this.experiment = source;
        guiName = null;
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
        if (guiName == null)
            guiName = createGuiName();
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

    public SimpleSpectrum getMergedMs1Spectrum() {
        return experiment.getMergedMs1Spectrum();
    }

    public PrecursorIonType getIonization() {
        return experiment.getPrecursorIonType();
    }

    public List<SiriusResultElement> getResults() {
        return this.results;
    }

    public List<IdentificationResult> getRawResults() {
        return originalResults;
    }

    public double getIonMass() {
        return experiment.getIonMass();
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


    public  void setRawResults(List<IdentificationResult> results) {
        setRawResults(results, SiriusResultElementConverter.convertResults(results));
    }

    public  void setRawResults(List<IdentificationResult> results, List<SiriusResultElement> myxoresults) {
        List<SiriusResultElement> old = this.results;
        this.results = myxoresults;
        this.originalResults = results == null ? Collections.<IdentificationResult>emptyList() : results;
        firePropertyChange("results_upadated", old, this.results);
        /*if (this.computeState == ComputingStatus.COMPUTING)
            setComputeState((results == null || results.size() == 0) ? ComputingStatus.FAILED : ComputingStatus.COMPUTED);*/
    }

    public  void setName(String name) {
        experiment.setName(name);
        setGuiName(createGuiName());
    }

    public  void setSuffix(int value) {
        this.suffix = value;
        setGuiName(createGuiName());
    }

    private String createGuiName() {
        return this.suffix >= 2 ? experiment.getName() + " (" + suffix + ")" : experiment.getName();
    }

    // with change event
    private void setGuiName(String guiName) {
        String old = this.guiName;
        this.guiName = guiName;
        firePropertyChange("guiName", old, this.guiName);
    }

    public  void setBestHit(final SiriusResultElement bestHit) {
        if (this.bestHit != bestHit) {
            if (this.bestHit != null)
                this.bestHit.setBestHit(false);
            this.bestHit = bestHit;
            this.bestHit.setBestHit(true);
            bestHitIndex = getResults().indexOf(bestHit);
        }
    }

    public  void setIonization(PrecursorIonType ionization) {
        PrecursorIonType old = experiment.getPrecursorIonType();
        experiment.setPrecursorIonType(ionization);
        firePropertyChange("ionization", old, experiment.getPrecursorIonType());
    }

    public  void setIonMass(double ionMass) {
        double old = experiment.getIonMass();
        experiment.setIonMass(ionMass);
        firePropertyChange("ionMass", old, experiment.getIonMass());
    }


    public  void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public  void setComputeState(ComputingStatus st) {
        ComputingStatus oldST = this.computeState;
        this.computeState = st;
        firePropertyChange("computeState", oldST, this.computeState);
    }

    //this can be use to initiate an arbitrary update event, e.g. to initialize a view
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
        newExp.setSuffix(getSuffix());
        List<SiriusResultElement> sre = new ArrayList<>(getResults());
        newExp.setRawResults(getRawResults(), sre);
        return newExp;
    }


}
