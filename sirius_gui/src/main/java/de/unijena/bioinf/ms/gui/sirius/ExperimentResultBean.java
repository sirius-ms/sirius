package de.unijena.bioinf.ms.gui.sirius;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.babelms.projectspace.ExperimentDirectory;
import de.unijena.bioinf.babelms.projectspace.GuiProjectSpace;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.ms.frontend.core.AbstractEDTBean;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.compute.jjobs.SiriusIdentificationGuiJob;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the wrapper for the ExperimentsResult class to interact with the gui
 * elements. It uses a special property change support that executes the events
 * in the EDT. So you can change all fields from any thread, the gui will still
 * be updated in the EDT. Some operations may NOT be Thread save, so you may have
 * to care about Synchronization.
 */
public class ExperimentResultBean extends AbstractEDTBean implements PropertyChangeListener {
    private static final String GUI_NAME_PROPERTY = "guiName";

    //the ms experiment we use for computation
    private ExperimentResult experimentResult;

    //Here are fields to view the SiriusResultElement
    private volatile List<IdentificationResultBean> results;

    private volatile IdentificationResultBean bestHit;
    private volatile int bestHitIndex = 0;

    private volatile ComputingStatus siriusComputeState = ComputingStatus.UNCOMPUTED;

    private volatile int nameIndex = 0;


    public ExperimentResultBean(MutableMs2Experiment source) {
        this(source, new ArrayList<>());
    }

    public ExperimentResultBean(MutableMs2Experiment source, List<IdentificationResult> results) {
        this(new ExperimentResult(source, results));
    }

    public ExperimentResultBean(ExperimentResult expResult) {
        this.experimentResult = expResult;
        bestHit = null;
        results = SiriusResultElementConverter.convertResults(experimentResult.getResults());
        if (getResults().size() > 0) siriusComputeState = ComputingStatus.COMPUTED;
    }

    public IdentificationResultBean getBestHit() {
        return bestHit;
    }

    public int getBestHitIndex() {
        return bestHitIndex;
    }

    public String getName() {
        return getMs2Experiment().getName();
    }

    public String getGUIName() {
        return makeGUIName(getName(), getNameIndex());
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public List<SimpleSpectrum> getMs1Spectra() {
        return getMs2Experiment().getMs1Spectra();
    }

    public List<MutableMs2Spectrum> getMs2Spectra() {
        return getMs2Experiment().getMs2Spectra();
    }

    public SimpleSpectrum getMergedMs1Spectrum() {
        return getMs2Experiment().getMergedMs1Spectrum();
    }

    public PrecursorIonType getIonization() {
        return getMs2Experiment().getPrecursorIonType();
    }

    public List<IdentificationResultBean> getResults() {
        return results;
    }

    public double getIonMass() {
        return getMs2Experiment().getIonMass();
    }

    public ExperimentDirectory getIdentifier(){
        return getExperimentResult().getAnnotation(ExperimentDirectory.class);
    }

    public boolean isComputed() {
        return siriusComputeState == ComputingStatus.COMPUTED;
    }

    public boolean isComputing() {
        return siriusComputeState == ComputingStatus.COMPUTING;
    }

    public boolean isUncomputed() {
        return siriusComputeState == ComputingStatus.UNCOMPUTED;
    }

    public ComputingStatus getSiriusComputeState() {
        return siriusComputeState;
    }

    public boolean isFailed() {
        return this.siriusComputeState == ComputingStatus.FAILED;
    }

    public boolean isQueued() {
        return siriusComputeState == ComputingStatus.QUEUED;
    }


    public void setRawResults(Iterable<IdentificationResult> results) {
        setResults(SiriusResultElementConverter.convertResults(results));
    }

    public void setResults(List<IdentificationResultBean> myxoresults) {
        List<IdentificationResultBean> old = this.results;
        this.results = myxoresults;
        firePropertyChange("results_updated", old, this.results);
    }

    public void setName(String name) {
        final String old = getMs2Experiment().getName();
        getMs2Experiment().setName(name);
        GuiProjectSpace.PS.changeName(this, old);
    }

    public void setNameIndex(int nameIndex) {
        this.nameIndex = nameIndex;
        firePropertyChange(GUI_NAME_PROPERTY, null, getGUIName());
    }

    public void setBestHit(final IdentificationResultBean bestHit) {
        if (bestHit == null) {
            if (this.bestHit != null)
                this.bestHit.setBestHit(false);
            this.bestHit = bestHit;
            bestHitIndex = 0;
            return;
        }

        if (this.bestHit != bestHit) {
            if (this.bestHit != null)
                this.bestHit.setBestHit(false);
            this.bestHit = bestHit;
            this.bestHit.setBestHit(true);
            bestHitIndex = getResults().indexOf(bestHit);
        }
    }

    public void setIonization(PrecursorIonType ionization) {
        PrecursorIonType old = getMs2Experiment().getPrecursorIonType();
        getMs2Experiment().setPrecursorIonType(ionization);
        firePropertyChange("ionization", old, getMs2Experiment().getPrecursorIonType());
    }

    public void setIonMass(double ionMass) {
        double old = getMs2Experiment().getIonMass();
        getMs2Experiment().setIonMass(ionMass);
        firePropertyChange("ionMass", old, getMs2Experiment().getIonMass());
    }


    public synchronized void setSiriusComputeState(ComputingStatus st) {
        ComputingStatus oldST = this.siriusComputeState;
        this.siriusComputeState = st;
        firePropertyChange("siriusComputeState", oldST, this.siriusComputeState);
    }

    //this can be use to initiate an arbitrary update event, e.g. to initialize a view
    public void fireUpdateEvent() {
        firePropertyChange("updated", false, true);
    }

    public MutableMs2Experiment getMs2Experiment() {
        return (MutableMs2Experiment) experimentResult.getExperiment();
    }

    public ExperimentResult getExperimentResult() {
        return experimentResult;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt instanceof JobStateEvent) {
            JobStateEvent e = (JobStateEvent) evt;
            if (e.getSource() instanceof SiriusIdentificationGuiJob)
                setSiriusComputeState(Jobs.getComputingState(e.getNewValue()));
        }
    }

    private static String makeGUIName(String name, int nameIndex) {
        if (nameIndex <= 1)
            return name;
        return name + " (" + nameIndex + ")";
    }
}
