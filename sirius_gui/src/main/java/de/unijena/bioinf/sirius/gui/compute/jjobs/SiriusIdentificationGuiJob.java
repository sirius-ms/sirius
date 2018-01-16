package de.unijena.bioinf.sirius.gui.compute.jjobs;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.compute.FormulaWhiteListJob;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.logging.TextAreaJJobContainer;

import java.util.List;

public class SiriusIdentificationGuiJob extends MasterJJob<List<IdentificationResult>> implements GuiObservableJJob<List<IdentificationResult>> {

    private final Sirius sirius;
    final ExperimentContainer ec;
    final String profile;
    final double ppm;
    final FormulaConstraints constraints;
    final SearchableDatabase searchableDatabase;
    final boolean onlyOrganic;
    final int numberOfCandidates;


    public SiriusIdentificationGuiJob(String profile, double ppm, int numberOfCandidates, FormulaConstraints constraints, boolean onlyOrganic, SearchableDatabase db, ExperimentContainer ec) {
        super(JobType.CPU);
        sirius = Jobs.getSiriusByProfile(profile);
        this.profile = profile;
        this.ppm = ppm;
        this.constraints = constraints;
        this.searchableDatabase = db;
        this.onlyOrganic = onlyOrganic;
        this.numberOfCandidates = numberOfCandidates;
        this.ec = ec;
        addPropertyChangeListener(JobStateEvent.JOB_STATE_EVENT, this.ec);

    }

    @Override
    protected JobManager jobManager() {
        return SiriusJobs.getGlobalJobManager();
    }

    @Override
    protected List<IdentificationResult> compute() throws Exception {
        MutableMs2Experiment experiment = new MutableMs2Experiment(ec.getMs2Experiment()); //todo we want to copy?
        //todo finde a nice way to combine progress between subjobs

        updateProgress(0, 1, 110);
        final Sirius.SiriusIdentificationJob identificationJob = sirius.makeIdentificationJob(experiment, numberOfCandidates);
        identificationJob.addPropertyChangeListener(JobProgressEvent.JOB_PROGRESS_EVENT, evt -> {
            JobProgressEvent e = (JobProgressEvent) evt;
            updateProgress(e.getMinValue(), e.getNewValue(), e.getMaxValue());
        });

        updateProgress(0, 3, 110);

        //todo progress stuf
        //todo find good solution for subjobs and depedencies
        //  sirius.getMs2Analyzer().setIsotopeHandling(container.enableIsotopesInMs2 ? FragmentationPatternAnalysis.IsotopeInMs2Handling.ALWAYS : FragmentationPatternAnalysis.IsotopeInMs2Handling.IGNORE);
        sirius.setTimeout(experiment, -1, Integer.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.treebuilder.timeout", "0")));
        sirius.setFormulaConstraints(experiment, constraints);
        sirius.setAllowedMassDeviation(experiment, new Deviation(ppm));
//        sirius.enableRecalibration(experiment, true);
//        sirius.setIsotopeMode(experiment, IsotopePatternHandling.both);

        updateProgress(0, 4, 110);
        checkForInterruption();
        if (searchableDatabase != null) {
            FormulaWhiteListJob wlj = new FormulaWhiteListJob(ppm, onlyOrganic, searchableDatabase, experiment);
            wlj.call();
        }

        updateProgress(0, 8, 110);
        checkForInterruption();

        final List<IdentificationResult> results = identificationJob.call();

        checkForInterruption();
        ec.setRawResults(results);
        return results;
    }

    @Override
    public void cleanup() {
        removePropertyChangeListener(this.ec);
    }

    @Override
    public SwingJJobContainer<List<IdentificationResult>> asSwingJob() {
        return new TextAreaJJobContainer<>(this, ec.getGUIName(), "Molecular Formula Identification");
    }
}
