package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.sirius.projectspace.Index;
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.compute.jjobs.SiriusIdentificationGuiJob;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * This is the wrapper for the Ms2Experiment.class to interact with the gui
 * elements. It uses a special property change support that executes the events
 * in the EDT. So you can change all fields from any thread, the gui will still
 * be updated in the EDT. Some operations may NOT be Thread save, so you may have
 * to care about Synchronization.
 */
public class ExperimentContainer extends AbstractEDTBean implements PropertyChangeListener {
    //the ms experiment we use for computation
    private final MutableMs2Experiment experiment;

    //Here are fields to view the SiriusResultElement
    private volatile List<SiriusResultElement> results;
    private volatile SiriusResultElement bestHit;
    private volatile int bestHitIndex = 0;

    private volatile ComputingStatus siriusComputeState = ComputingStatus.UNCOMPUTED;


    //here are fields to view the ExperimentContainer
    private String guiName;
//    private int suffix;


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
        bestHit = null;
        results = Collections.emptyList();
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

    public int getIndex() {
        return experiment.getAnnotation(Index.class, Index.NO_INDEX).index;
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

    public Iterable<IdentificationResult> getRawResults() {
        return () -> new IdentificationResultIterator(getResults().iterator());
    }

    public double getIonMass() {
        return experiment.getIonMass();
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

    public void setResults(List<SiriusResultElement> myxoresults) {
        List<SiriusResultElement> old = this.results;
        this.results = myxoresults;
        firePropertyChange("results_upadated", old, this.results);
    }

    public void setName(String name) {
        experiment.setName(name);
        setGuiName(createGuiName());
    }

    public void setIndex(int value) {
        experiment.setAnnotation(Index.class, new Index(value));
        setGuiName(createGuiName());
    }

    private String createGuiName() {
        final int i = getIndex();
        return i >= 2 ? experiment.getName() + " (" + i + ")" : experiment.getName();
    }

    // with change event
    private void setGuiName(String guiName) {
        String old = this.guiName;
        this.guiName = guiName;
        firePropertyChange("guiName", old, this.guiName);
    }

    public void setBestHit(final SiriusResultElement bestHit) {
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
        PrecursorIonType old = experiment.getPrecursorIonType();
        experiment.setPrecursorIonType(ionization);
        firePropertyChange("ionization", old, experiment.getPrecursorIonType());
    }

    public void setIonMass(double ionMass) {
        double old = experiment.getIonMass();
        experiment.setIonMass(ionMass);
        firePropertyChange("ionMass", old, experiment.getIonMass());
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

    public URL getSource() {
        return experiment.getSource();
    }

    public MutableMs2Experiment getMs2Experiment() {
        return experiment;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt instanceof JobStateEvent) {
            JobStateEvent e = (JobStateEvent) evt;
            if (e.getSource() instanceof SiriusIdentificationGuiJob)
                setSiriusComputeState(Jobs.getComputingState(e.getNewValue()));
        }
    }
}
