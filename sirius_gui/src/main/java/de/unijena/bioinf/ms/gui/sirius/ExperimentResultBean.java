package de.unijena.bioinf.ms.gui.sirius;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.projectspace.ExperimentDirectory;
import de.unijena.bioinf.babelms.projectspace.GuiProjectSpace;
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.AbstractEDTBean;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.compute.jjobs.SiriusIdentificationGuiJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IdentificationResults;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is the wrapper for the ExperimentsResult class to interact with the gui
 * elements. It uses a special property change support that executes the events
 * in the EDT. So you can change all fields from any thread, the gui will still
 * be updated in the EDT. Some operations may NOT be Thread save, so you may have
 * to care about Synchronization.
 */
public class ExperimentResultBean extends AbstractEDTBean implements PropertyChangeListener, DataAnnotation {
    //the ms experiment we use for computationz
    private final ExperimentResult experimentResult;

    // Here are fields to view the Identifications results
    private volatile ResultsListView results = new ResultsListView(null);


    //todo best hit property change is needed.
    // e.g. if the scoring changes from sirius to zodiac
    //todo make compute state nice

    public ExperimentResultBean(MutableMs2Experiment source) {
        this(source, new ArrayList<>());
    }

    public ExperimentResultBean(MutableMs2Experiment source, Iterable<IdentificationResult> results) {
        this(new ExperimentResult(source, results));
    }

    public ExperimentResultBean(ExperimentResult expResult) {
        this.experimentResult = expResult;
        configureListeners();
        if (experimentResult.hasAnnotation(IdentificationResults.class)) {
            setRawResults(experimentResult.getResults());
            setSiriusComputeState(ComputingStatus.COMPUTED);
        } else {
            setSiriusComputeState(ComputingStatus.UNCOMPUTED);
        }
    }

    public IdentificationResultBean getBestHit() {
        if (experimentResult.hasResults())
            return experimentResult.getResults().getBest().getAnnotation(IdentificationResultBean.class);
        return null;
    }

    public String getName() {
        return getMs2Experiment().getName();
    }

    public String getGUIName() {
        return getName() + " (" + getProjectSpaceID().getIndex() + ")";
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
        if (results == null)
            return Collections.emptyList();
        return results;
    }

    public double getIonMass() {
        return getMs2Experiment().getIonMass();
    }

    public ExperimentDirectory getProjectSpaceID() {
        return getExperimentResult().getAnnotation(ExperimentDirectory.class);
    }




    public ComputingStatus getSiriusComputeState() {
        return experimentResult.getAnnotation(ComputingStatus.class);
    }

    public boolean isComputed() {
        return getSiriusComputeState() == ComputingStatus.COMPUTED;
    }

    public boolean isComputing() {
        return getSiriusComputeState() == ComputingStatus.COMPUTING;
    }

    public boolean isUncomputed() {
        return getSiriusComputeState() == ComputingStatus.UNCOMPUTED;
    }

    public boolean isFailed() {
        return getSiriusComputeState() == ComputingStatus.FAILED;
    }

    public boolean isQueued() {
        return getSiriusComputeState() == ComputingStatus.QUEUED;
    }


    public void setName(String name) {
        final String old = getMs2Experiment().getName();
        getMs2Experiment().setName(name);
        GuiProjectSpace.PS.changeName(this, old);
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


    private void configureListeners() {
        experimentResult.addAnnotationChangeListener(evt -> {
            if (evt.getPropertyName().equals(DataAnnotation.getIdentifier(IdentificationResults.class)))
                setRawResults((IdentificationResults) evt.getNewValue());

            firePropertyChange(evt);
        });
    }

    private class ResultsListView extends AbstractList<IdentificationResultBean> {
        private IdentificationResults rawResults;
        private ComputingStatus compStatus;

        private ResultsListView() {
            this(null);
        }

        private ResultsListView(IdentificationResults rawResults) {
            setRawResults(rawResults);
        }

        @Override
        public IdentificationResultBean get(int index) {
            if (rawResults == null) throw new ArrayIndexOutOfBoundsException(index);
            return unWrap(rawResults.get(index));
        }

        @Override
        public int size() {
            if (rawResults == null) return 0;
            return rawResults.size();
        }

        public IdentificationResultBean getBest() {
            if (rawResults == null) return null;
            return unWrap(rawResults.getBest());
        }

        private IdentificationResultBean unWrap(final @NotNull IdentificationResult ir) {
            return ir.getAnnotationOrThrow(IdentificationResultBean.class);
        }

        private void setRawResults(IdentificationResults rawResults) {
            if (this.rawResults != null)
                this.rawResults.forEach(it -> it.setAnnotation(IdentificationResultBean.class,null));

            this.rawResults = rawResults;

            if (this.rawResults != null)
                this.rawResults.forEach(IdentificationResultBean::new);
        }

        public synchronized void setSiriusComputeState(ComputingStatus st) {
            getExperimentResult().setAnnotation(ComputingStatus.class, st);
        }
    }
}
