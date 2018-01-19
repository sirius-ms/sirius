package de.unijena.bioinf.sirius.gui.compute.jjobs;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.jjobs.DependentJJob;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.jjobs.SwingJJobContainer;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.logging.TextAreaJJobContainer;

import java.util.List;

//todo woud be nicer to simply extend the sirius identification job, or simply wrap it, but its not static
public class SiriusIdentificationGuiJob extends DependentJJob<List<SiriusResultElement>> implements GuiObservableJJob<List<SiriusResultElement>> {

    final ExperimentContainer ec;
    final int numberOfCandidates;
    private final Sirius sirius;

    public SiriusIdentificationGuiJob(String profile, int numberOfCandidates, ExperimentContainer ec) {
        super(JobType.CPU);
        this.sirius = Jobs.getSiriusByProfile(profile);
        this.numberOfCandidates = numberOfCandidates;
        this.ec = ec;
        addPropertyChangeListener(JobStateEvent.JOB_STATE_EVENT, this.ec);
    }


    @Override
    protected List<SiriusResultElement> compute() throws Exception {
        final MutableMs2Experiment experiment = ec.getMs2Experiment();

        //todo find a nice way to combine progress between subjobs
        updateProgress(0, 110, 1);
        final Sirius.SiriusIdentificationJob identificationJob = sirius.makeIdentificationJob(experiment, numberOfCandidates);
        identificationJob.addPropertyChangeListener(JobProgressEvent.JOB_PROGRESS_EVENT, evt -> {
            JobProgressEvent e = (JobProgressEvent) evt;
            updateProgress(e.getMinValue(), e.getMaxValue() + 5, e.getNewValue());
        });

        updateProgress(0, 110, 4);
        final List<IdentificationResult> results = identificationJob.call();

        checkForInterruption();
        ec.setRawResults(results);
        updateProgress(0, 110, 110);
        return ec.getResults();
    }

    @Override
    public void cleanup() {
        removePropertyChangeListener(this.ec);
    }

    @Override
    public SwingJJobContainer<List<SiriusResultElement>> asSwingJob() {
        return new TextAreaJJobContainer<>(this, ec.getGUIName(), "Molecular Formula Identification");
    }
}
